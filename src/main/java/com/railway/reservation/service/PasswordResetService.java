package com.railway.reservation.service;

import com.railway.reservation.dto.LoginResponseDto;
import com.railway.reservation.dto.SignUpDtos.SignUpStarted;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.PasswordResetCode;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.PasswordResetCodeRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.notify.AccountEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Forgotten passwords (RRS-F-002). A 6-digit code is emailed to the account's address and the new password
 * is accepted only with that code. The request answers identically whether or not the email has an account,
 * so the form cannot be used to find out who is registered. Codes follow the sign-up rules: 10 minutes,
 * 5 wrong tries, one new code a minute and at most 5 an hour.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String WRONG = "That code is not right, or it has expired. Check the newest email or send a new code.";

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PasswordResetService(UserRepository userRepository, PasswordResetCodeRepository codeRepository,
                                PasswordEncoder passwordEncoder, JwtService jwtService,
                                ApplicationEventPublisher events, Clock clock) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.events = events;
        this.clock = clock;
    }

    /** Emails a code when the address has an account. The answer is the same either way. */
    @Transactional
    public SignUpStarted request(String rawEmail) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now(clock);
        SignUpStarted answer = new SignUpStarted(email, UserRegistrationService.CODE_MINUTES * 60L, UserRegistrationService.RESEND_SECONDS);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return answer;

        PasswordResetCode reset = codeRepository.findByUserIdForUpdate(user.getId()).orElse(null);
        boolean fresh = reset == null;
        if (fresh) reset = new PasswordResetCode(user.getId(), now);
        if (!fresh && Duration.between(reset.getLastSentAt(), now).toSeconds() < UserRegistrationService.RESEND_SECONDS) {
            return answer;   // a code went out moments ago
        }
        boolean newWindow = reset.getCodesSent() == 0 || reset.getLastSentAt().isBefore(now.minusHours(1));
        if (!newWindow && reset.getCodesSent() >= UserRegistrationService.MAX_CODES_PER_HOUR) {
            log.warn("Password reset codes capped for userId={}", user.getId());
            return answer;
        }
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        reset.newCode(passwordEncoder.encode(code), now, now.plusMinutes(UserRegistrationService.CODE_MINUTES), newWindow);
        codeRepository.save(reset);
        events.publishEvent(new AccountEvents.PasswordResetRequested(user.getEmail(), user.getName(), code,
                UserRegistrationService.CODE_MINUTES));
        log.info("Password reset code issued userId={}", user.getId());
        return answer;
    }

    /** Sets the new password when the code matches, clears any lockout and signs the passenger in. */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponseDto reset(String rawEmail, String code, String newPassword) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByEmailForUpdate(email).orElseThrow(PasswordResetService::wrong);
        PasswordResetCode reset = codeRepository.findByUserIdForUpdate(user.getId()).orElseThrow(PasswordResetService::wrong);
        if (reset.getCodeHash() == null) throw wrong();
        if (!passwordEncoder.matches(code, reset.getCodeHash())) {
            reset.recordFailedAttempt(UserRegistrationService.MAX_ATTEMPTS);
            throw wrong();
        }
        if (now.isAfter(reset.getCodeExpiresAt())) {
            throw ApiException.badRequest("CODE_EXPIRED", "That code has expired. Send a new code.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        codeRepository.delete(reset);
        events.publishEvent(new AccountEvents.PasswordChanged(user.getEmail(), user.getName()));
        log.info("Password reset completed userId={}", user.getId());
        return new LoginResponseDto(jwtService.generateToken(user));
    }

    @Transactional
    public int purgeOld() {
        return codeRepository.deleteStale(LocalDateTime.now(clock).minusDays(1));
    }

    private static ApiException wrong() {
        return ApiException.badRequest("CODE_INVALID", WRONG);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
