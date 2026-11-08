package com.railway.reservation.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.railway.reservation.support.MailCatcherConfig;
import com.railway.reservation.support.SignUp;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TC-SEC-01: passwords are BCrypt hashed, never returned in responses and never logged. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(MailCatcherConfig.class)
@DisplayName("TC-SEC-01 password protection [RRS-SR-001]")
class PasswordProtectionSecurityTest {

    private static final String REGISTER_URL = "/api/v1/users/register";
    private static final String LOGIN_URL = "/api/v1/users/login";
    private static final String EMAIL = "sec01-user@example.com";
    private static final String RAW_PASSWORD = "Sup3r-Secret#Pass-91";
    private static final String WRONG_PASSWORD = "Wr0ng-Secret#Pass-77";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailCatcherConfig.MailCatcher mail;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private Logger rootLogger;
    private Logger appLogger;
    private Level originalAppLevel;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogCapture() {
        rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appLogger = (Logger) LoggerFactory.getLogger("com.railway.reservation");
        originalAppLevel = appLogger.getLevel();
        appLogger.setLevel(Level.DEBUG);

        logAppender = new ListAppender<>();
        logAppender.start();
        rootLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogCapture() {
        rootLogger.detachAppender(logAppender);
        logAppender.stop();
        appLogger.setLevel(originalAppLevel);
    }

    @Test
    void passwordIsStoredAsBcryptHashWithSalt() throws Exception {
        register();

        User stored = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("$2");
        assertThat(stored.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, stored.getPasswordHash())).isTrue();
    }

    @Test
    void sameRawPasswordProducesDifferentHashesBecauseOfSalt() {
        assertThat(passwordEncoder.encode(RAW_PASSWORD)).isNotEqualTo(passwordEncoder.encode(RAW_PASSWORD));
    }

    @Test
    void passwordAndHashNeverAppearInRegisterOrLoginResponses() throws Exception {
        String registerResponse = register();
        String hash = userRepository.findByEmail(EMAIL).orElseThrow().getPasswordHash();

        String loginResponse = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(registerResponse).doesNotContain(RAW_PASSWORD).doesNotContain(hash);
        assertThat(loginResponse).doesNotContain(RAW_PASSWORD).doesNotContain(hash);
    }

    @Test
    void passwordsAreNeverWrittenToLogs() throws Exception {
        register();
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(RAW_PASSWORD)))
                .andExpect(status().isOk());
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(WRONG_PASSWORD)))
                .andExpect(status().isUnauthorized());

        for (ILoggingEvent event : logAppender.list) {
            String message = event.getFormattedMessage();
            assertThat(message).doesNotContain(RAW_PASSWORD).doesNotContain(WRONG_PASSWORD);
        }
    }

    /** Both sign-up steps; returns the two responses joined so neither can leak a password. */
    private String register() throws Exception {
        String started = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Security User",
                                "email", EMAIL,
                                "phone", "9876543210",
                                "password", RAW_PASSWORD))))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String created = SignUp.verify(mockMvc, EMAIL, mail.codeFor(EMAIL))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return started + created;
    }

    private String loginJson(String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", password));
    }
}
