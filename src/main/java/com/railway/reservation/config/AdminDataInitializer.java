package com.railway.reservation.config;

import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminDataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail != null && !adminEmail.trim().isEmpty()
                && adminPassword != null && !adminPassword.trim().isEmpty()) {
            String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
            if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                User admin = new User(
                        "Administrator",
                        normalizedEmail,
                        "0000000000",
                        passwordEncoder.encode(adminPassword.trim()),
                        Role.ROLE_ADMIN
                );
                userRepository.save(admin);
            }
        }
    }
}
