package com.railway.reservation.service;

import com.railway.reservation.dto.LoginRequestDto;
import com.railway.reservation.dto.LoginResponseDto;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final int maxFailedAttempts;
    private final long lockDurationMinutes;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.auth.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${app.auth.lock-duration-minutes:15}") long lockDurationMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        if (maxFailedAttempts < 1 || lockDurationMinutes < 1) {
            throw new IllegalArgumentException("Login lockout settings must be positive.");
        }
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailForUpdate(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        LocalDateTime now = LocalDateTime.now();
        if (user.isAccountLocked()) {
            throw new InvalidCredentialsException();
        }

        // Expired locks start a fresh consecutive-failure window.
        if (user.getLockedUntil() != null) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= maxFailedAttempts) {
                user.setLockedUntil(now.plusMinutes(lockDurationMinutes));
            }
            userRepository.save(user);
            throw new InvalidCredentialsException();
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return new LoginResponseDto(jwtService.generateToken(user));
    }
}
