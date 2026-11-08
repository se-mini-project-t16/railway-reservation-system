package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.railway.reservation.support.MailCatcherConfig;
import com.railway.reservation.support.SignUp;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(MailCatcherConfig.class)
class UserLoginControllerTest {

    private static final String LOGIN_URL = "/api/v1/users/login";
    private static final String RAW_PASSWORD = "correct-horse-42";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailCatcherConfig.MailCatcher mail;

    @Value("${app.jwt.secret}")
    private String configuredJwtSecret;

    @Test
    void logsInWithBcryptPasswordAndReturnsSignedJwtWithExpectedClaims() throws Exception {
        User user = createUser("kishan@example.com");

        String response = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("KISHAN@example.com", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();
        SecretKey testKey = Keys.hmacShaKeyFor(configuredJwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(testKey).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("kishan@example.com");
        assertThat(claims.get("userId", Number.class).longValue()).isEqualTo(user.getId());
        assertThat(claims.get("role", String.class)).isEqualTo(Role.ROLE_PASSENGER.name());
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(new Date());
        assertThat(claims.getExpiration()).isAfter(new Date());
        assertThat(response).doesNotContain(RAW_PASSWORD).doesNotContain(user.getPasswordHash());
    }

    @Test
    void rejectsIncorrectPasswordWithUnauthorizedResponse() throws Exception {
        createUser("kishan@example.com");

        String response = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("kishan@example.com", "incorrect-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(RAW_PASSWORD).doesNotContain("passwordHash");
    }

    @Test
    void locksAccountAfterFiveFailedAttemptsAndBlocksCorrectPassword() throws Exception {
        User user = createUser("kishan@example.com");

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequest("kishan@example.com", "incorrect-password")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid credentials."));
        }

        User lockedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isAfter(LocalDateTime.now());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("kishan@example.com", RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void persistsLockoutAcrossCommittedLoginRequests() throws Exception {
        String email = "lockout-" + UUID.randomUUID() + "@example.com";
        User user = createUser(email);

        try {
            for (int attempt = 1; attempt <= 5; attempt++) {
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest(email, "incorrect-password")))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.message").value("Invalid credentials."));

                User persistedUser = userRepository.findById(user.getId()).orElseThrow();
                assertThat(persistedUser.getFailedLoginAttempts()).isEqualTo(attempt);
                if (attempt < 5) {
                    assertThat(persistedUser.getLockedUntil()).isNull();
                } else {
                    assertThat(persistedUser.getLockedUntil()).isAfter(LocalDateTime.now());
                }
            }

            User lockedUser = userRepository.findById(user.getId()).orElseThrow();
            LocalDateTime persistedLockExpiry = lockedUser.getLockedUntil();

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequest(email, RAW_PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid credentials."));

            User afterCorrectPasswordAttempt = userRepository.findById(user.getId()).orElseThrow();
            assertThat(afterCorrectPasswordAttempt.getFailedLoginAttempts()).isEqualTo(5);
            assertThat(afterCorrectPasswordAttempt.getLockedUntil()).isEqualTo(persistedLockExpiry);
        } finally {
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentFailedLoginsAreCountedWithoutLostUpdates() throws Exception {
        String email = "lockout-race-" + UUID.randomUUID() + "@example.com";
        User user = createUser(email);
        int concurrentAttempts = 5;
        CountDownLatch ready = new CountDownLatch(concurrentAttempts);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentAttempts);
        List<Future<Integer>> responses = new ArrayList<>();

        try {
            for (int attempt = 0; attempt < concurrentAttempts; attempt++) {
                responses.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent login request.");
                    }
                    return mockMvc.perform(post(LOGIN_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginRequest(email, "incorrect-password")))
                            .andReturn().getResponse().getStatus();
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Integer> response : responses) {
                assertThat(response.get(15, TimeUnit.SECONDS)).isEqualTo(401);
            }

            User persistedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(persistedUser.getFailedLoginAttempts()).isEqualTo(concurrentAttempts);
            assertThat(persistedUser.getLockedUntil()).isAfter(LocalDateTime.now());
        } finally {
            start.countDown();
            executor.shutdownNow();
            boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
            try {
                userRepository.deleteById(user.getId());
            } finally {
                assertThat(terminated).as("concurrent login requests should finish before cleanup").isTrue();
            }
        }
    }

    @Test
    void successfulLoginResetsFailuresAndExpiredLock() throws Exception {
        User user = createUser("kishan@example.com");
        user.setFailedLoginAttempts(2);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        userRepository.save(user);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("kishan@example.com", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());

        User loggedInUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(loggedInUser.getFailedLoginAttempts()).isZero();
        assertThat(loggedInUser.getLockedUntil()).isNull();
    }

    @Test
    void rejectsUnknownEmailWithSameUnauthorizedResponse() throws Exception {
        String response = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("unknown@example.com", RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("unknown@example.com").doesNotContain("passwordHash");
    }

    @Test
    void rejectsSqlInjectionPayloadInLoginEmail() throws Exception {
        String injection = "admin' OR '1'='1";

        String response = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(injection, "anything")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(injection).doesNotContain("passwordHash");
    }

    @Test
    void registeredUserCanImmediatelyLogIn() throws Exception {
        Map<String, String> registration = Map.of(
                "name", "Kishan",
                "email", "kishan@example.com",
                "phone", "+919876543210",
                "password", RAW_PASSWORD
        );

        SignUp.start(mockMvc, registration).andExpect(status().isAccepted());

        // Until the emailed code is confirmed there is no account to sign in to
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("kishan@example.com", RAW_PASSWORD)))
                .andExpect(status().isUnauthorized());

        SignUp.verify(mockMvc, "kishan@example.com", mail.codeFor("kishan@example.com")).andExpect(status().isCreated());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("kishan@example.com", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    private User createUser(String email) {
        return userRepository.save(new User(
                "Kishan", email, "+919876543210", passwordEncoder.encode(RAW_PASSWORD), Role.ROLE_PASSENGER));
    }

    private String loginRequest(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }
}
