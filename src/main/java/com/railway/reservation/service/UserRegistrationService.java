package com.railway.reservation.service;

import com.railway.reservation.dto.RegisterRequestDto;
import com.railway.reservation.dto.RegisterResponseDto;
import com.railway.reservation.dto.SignUpDtos.SignUpStarted;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.PendingRegistration;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.PendingRegistrationRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.mail.EmailNotSentException;
import com.railway.reservation.service.mail.EmailSender;
import com.railway.reservation.service.mail.VerificationEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Sign-up with email verification (RRS-F-001). The details are kept aside and a 6-digit code is
 * emailed; the account is created only when that code is entered. Codes expire after 10 minutes,
 * allow 5 wrong tries, can be re-sent once a minute and at most 5 times an hour.
 */
@Service
public class UserRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final int CODE_MINUTES = 10;
    public static final int MAX_ATTEMPTS = 5;
    public static final int RESEND_SECONDS = 60;
    public static final int MAX_CODES_PER_HOUR = 5;

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSender emailSender;
    private final Clock clock;

    public UserRegistrationService(UserRepository userRepository, PendingRegistrationRepository pendingRepository,
                                   PasswordEncoder passwordEncoder, JwtService jwtService, EmailSender emailSender,
                                   Clock clock) {
        this.userRepository = userRepository;
        this.pendingRepository = pendingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    /** Keeps the details aside and emails a code. Nothing is created in the users table yet. */
    @Transactional
    public SignUpStarted start(RegisterRequestDto request) {
        String email = normalize(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PendingRegistration pending = pendingRepository.findByEmailForUpdate(email).orElse(null);
        boolean fresh = pending == null;
        if (fresh) pending = new PendingRegistration(email, now);
        pending.setDetails(request.getName().trim(), request.getPhone().trim(), passwordEncoder.encode(request.getPassword()));

        if (!fresh && codeStillUsable(pending, now) && secondsUntilResend(pending, now) > 0) {
            // A code went out moments ago (a double click, or details corrected): keep it, do not send another
            pendingRepository.save(pending);
            return started(pending, now);
        }
        try {
            sendCode(pending, now);
            pendingRepository.saveAndFlush(pending);
        } catch (DataIntegrityViolationException e) {
            throw tooSoon(RESEND_SECONDS);
        }
        return started(pending, now);
    }

    @Transactional
    public SignUpStarted resend(String rawEmail) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now(clock);
        PendingRegistration pending = pendingRepository.findByEmailForUpdate(email).orElseThrow(UserRegistrationService::notFound);
        long wait = secondsUntilResend(pending, now);
        if (wait > 0) throw tooSoon(wait);
        sendCode(pending, now);
        return started(pending, now);
    }

    /** Checks the code and, when it matches, creates the account and signs the passenger in. */
    @Transactional(noRollbackFor = ApiException.class)
    public RegisterResponseDto verify(String rawEmail, String code) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now(clock);
        PendingRegistration pending = pendingRepository.findByEmailForUpdate(email).orElseThrow(UserRegistrationService::notFound);
        if (pending.getCodeHash() == null) {
            throw ApiException.badRequest("CODE_USED_UP", "That code was entered wrongly too many times. Send a new code.");
        }
        if (now.isAfter(pending.getCodeExpiresAt())) {
            throw ApiException.badRequest("CODE_EXPIRED", "That code has expired. Send a new code.");
        }
        if (!passwordEncoder.matches(code, pending.getCodeHash())) {
            int left = MAX_ATTEMPTS - pending.recordFailedAttempt(MAX_ATTEMPTS);
            if (left <= 0) {
                throw ApiException.badRequest("CODE_USED_UP", "That code was entered wrongly too many times. Send a new code.");
            }
            throw ApiException.badRequest("CODE_INVALID",
                    "That code is not right. " + left + (left == 1 ? " try" : " tries") + " left.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            pendingRepository.delete(pending);
            throw new DuplicateEmailException();
        }
        User user;
        try {
            user = userRepository.saveAndFlush(new User(pending.getName(), email, pending.getPhone(),
                    pending.getPasswordHash(), Role.ROLE_PASSENGER));
        } catch (DataIntegrityViolationException ex) {
            // The unique database constraint remains authoritative if two confirmations race
            throw new DuplicateEmailException();
        }
        pendingRepository.delete(pending);
        log.info("Account created after email verification userId={}", user.getId());
        return new RegisterResponseDto(user.getId(), jwtService.generateToken(user));
    }

    /** Sign-ups abandoned for a day are forgotten. */
    @Transactional
    public int purgeAbandoned() {
        return pendingRepository.deleteStale(LocalDateTime.now(clock).minusDays(1));
    }

    private void sendCode(PendingRegistration pending, LocalDateTime now) {
        boolean newWindow = pending.getCodesSent() == 0 || pending.getLastSentAt().isBefore(now.minusHours(1));
        if (!newWindow && pending.getCodesSent() >= MAX_CODES_PER_HOUR) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_CODES",
                    "Too many codes were sent to this address. Try again in an hour.");
        }
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        pending.newCode(passwordEncoder.encode(code), now, now.plusMinutes(CODE_MINUTES), newWindow);
        try {
            emailSender.send(pending.getEmail(), VerificationEmail.subject(code),
                    VerificationEmail.text(pending.getName(), code, CODE_MINUTES),
                    VerificationEmail.html(pending.getName(), code, CODE_MINUTES));
        } catch (EmailNotSentException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EMAIL_NOT_SENT",
                    "We could not send a code to this address. Check it and try again.");
        }
    }

    private static boolean codeStillUsable(PendingRegistration p, LocalDateTime now) {
        return p.getCodeHash() != null && now.isBefore(p.getCodeExpiresAt());
    }

    private static long secondsUntilResend(PendingRegistration p, LocalDateTime now) {
        return Math.max(0, RESEND_SECONDS - Duration.between(p.getLastSentAt(), now).toSeconds());
    }

    private static SignUpStarted started(PendingRegistration p, LocalDateTime now) {
        return new SignUpStarted(p.getEmail(), Math.max(0, Duration.between(now, p.getCodeExpiresAt()).toSeconds()),
                secondsUntilResend(p, now));
    }

    private static ApiException tooSoon(long seconds) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_TOO_SOON",
                "A code was sent moments ago. You can ask for another in " + seconds + " seconds.");
    }

    private static ApiException notFound() {
        return ApiException.badRequest("SIGNUP_NOT_FOUND", "There is no sign-up waiting for this email. Start again.");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
