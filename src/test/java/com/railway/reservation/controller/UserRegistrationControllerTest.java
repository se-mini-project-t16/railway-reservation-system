package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.railway.reservation.support.MailCatcherConfig;
import com.railway.reservation.support.SignUp;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(MailCatcherConfig.class)
class UserRegistrationControllerTest {

    private static final String REGISTER_URL = "/api/v1/users/register";
    private static final String RAW_PASSWORD = "correct-horse-42";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailCatcherConfig.MailCatcher mail;

    @org.junit.jupiter.api.BeforeEach
    void clearMail() {
        mail.clear();
    }

    @Test
    void registersUserWithBcryptAndDoesNotExposePasswordData() throws Exception {
        String started = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("Kishan", "kishan@example.com")))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String response = SignUp.verify(mockMvc, "kishan@example.com", mail.codeFor("kishan@example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andReturn().getResponse().getContentAsString();

        assertThat(started).doesNotContain(RAW_PASSWORD).doesNotContain("passwordHash").doesNotContain("\"password\"");
        assertThat(response).doesNotContain(RAW_PASSWORD).doesNotContain("passwordHash").doesNotContain("\"password\"");
        User savedUser = userRepository.findByEmail("kishan@example.com").orElseThrow();
        assertThat(savedUser.getPasswordHash()).startsWith("$2");
        assertThat(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .matches(RAW_PASSWORD, savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsMissingAndInvalidFieldsWithBadRequest() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        Map<String, String> invalidRequest = Map.of(
                "name", "   ",
                "email", "not-an-email",
                "phone", "12",
                "password", "short"
        );
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void rejectsDuplicateEmailWithConflictIncludingCaseDifferences() throws Exception {
        SignUp.complete(mockMvc, mail, objectMapper.readValue(validRequest("Kishan", "kishan@example.com"), Map.class));

        String duplicateResponse = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("Other", "KISHAN@example.com")))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(duplicateResponse).doesNotContain(RAW_PASSWORD).doesNotContain("passwordHash");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsMalformedJsonWithBadRequest() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPasswordLongerThanBcryptUtf8Limit() throws Exception {
        String multibytePassword = "é".repeat(37);
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Kishan",
                "email", "kishan@example.com",
                "phone", "+919876543210",
                "password", multibytePassword
        ));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private String validRequest(String name, String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "email", email,
                "phone", "+919876543210",
                "password", RAW_PASSWORD
        ));
    }
}
