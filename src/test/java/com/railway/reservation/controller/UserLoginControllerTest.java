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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated());

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
