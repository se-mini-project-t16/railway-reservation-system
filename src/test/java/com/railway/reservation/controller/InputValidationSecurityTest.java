package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TC-SEC-03: server-side rejection of SQL injection and XSS payloads with 400 Bad Request. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("TC-SEC-03 input validation security [RRS-SR-003]")
class InputValidationSecurityTest {

    private static final String REGISTER_URL = "/api/v1/users/register";
    private static final String SEARCH_URL = "/api/v1/trains/search";
    private static final String ADMIN_TRAINS_URL = "/api/v1/admin/trains";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "admin' OR '1'='1",
            "x'; DROP TABLE users;--",
            "<img src=x onerror=alert(1)>"
    })
    void registrationRejectsMaliciousNameWith400(String maliciousName) throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(maliciousName, "attacker@example.com")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationStillAcceptsApostropheInName() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Conor O'Brien", "obrien-sec03@example.com")))
                .andExpect(status().isCreated());
    }

    @Test
    void registrationStillAcceptsHyphenatedName() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Anne-Marie", "annemarie-sec03@example.com")))
                .andExpect(status().isCreated());
    }

    @Test
    void searchRejectsSqlInjectionInSourceWith400() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "admin' OR '1'='1")
                        .param("dst", "Delhi")
                        .param("date", futureDate()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchRejectsXssInDestinationWith400() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Bengaluru")
                        .param("dst", "<script>alert(1)</script>")
                        .param("date", futureDate()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCreateTrainRejectsScriptTagInNameWith400() throws Exception {
        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(trainJson("99031", "<script>alert(1)</script>", "Bengaluru", "Delhi")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCreateTrainRejectsSqlInjectionInStationWith400() throws Exception {
        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(trainJson("99032", "Test Express", "Bengaluru' OR '1'='1", "Delhi")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCreateTrainStillAcceptsNormalNames() throws Exception {
        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(trainJson("99033", "Test Express", "Bengaluru", "Delhi")))
                .andExpect(status().isCreated());
    }

    private String adminToken() {
        User admin = userRepository.save(new User(
                "Security Admin", "sec03-admin@railway.test", "9876543210",
                passwordEncoder.encode("Admin#12345"), Role.ROLE_ADMIN));
        return jwtService.generateToken(admin);
    }

    private String registerJson(String name, String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "email", email,
                "phone", "9876543210",
                "password", "Passenger#123"));
    }

    private String trainJson(String trainNo, String name, String source, String destination) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "trainNo", trainNo,
                "name", name,
                "sourceStation", source,
                "destinationStation", destination,
                "departureTime", "19:20:00",
                "arrivalTime", "06:30:00",
                "classes", List.of(Map.of("className", "SL", "totalSeats", 72, "fare", 780.00))));
    }

    private String futureDate() {
        return LocalDate.now().plusDays(7).toString();
    }
}
