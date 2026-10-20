package com.railway.reservation.service;

import com.railway.reservation.dto.RegisterRequestDto;
import com.railway.reservation.dto.RegisterResponseDto;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        User user = new User(
                request.getName().trim(),
                normalizedEmail,
                request.getPhone().trim(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_PASSENGER
        );

        try {
            User savedUser = userRepository.save(user);
            return new RegisterResponseDto(savedUser.getId());
        } catch (DataIntegrityViolationException ex) {
            // The unique database constraint remains authoritative if concurrent requests race.
            throw new DuplicateEmailException();
        }
    }
}
