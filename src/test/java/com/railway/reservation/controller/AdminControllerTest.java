package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.TrainClassDto;
import com.railway.reservation.dto.UpdateTrainDto;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainClassRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    private static final String ADMIN_TRAINS_URL = "/api/v1/admin/trains";
    private static final String SEARCH_URL = "/api/v1/trains/search";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainClassRepository trainClassRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User adminUser;
    private User passengerUser;
    private String adminToken;
    private String passengerToken;
    private String expiredToken;
    private static final String TAMPERED_TOKEN = "eyJhbGciOiJIUzI1NiJ9.tampered.token";

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        trainRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(new User(
                "Admin User",
                "admin@railway.com",
                "9999999999",
                passwordEncoder.encode("AdminPass123!"),
                Role.ROLE_ADMIN
        ));

        passengerUser = userRepository.save(new User(
                "Passenger User",
                "passenger@railway.com",
                "8888888888",
                passwordEncoder.encode("Pass1234!"),
                Role.ROLE_PASSENGER
        ));

        adminToken = jwtService.generateToken(adminUser);
        passengerToken = jwtService.generateToken(passengerUser);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        expiredToken = Jwts.builder()
                .subject(adminUser.getEmail())
                .claim("userId", adminUser.getId())
                .claim("role", adminUser.getRole().name())
                .issuedAt(new Date(System.currentTimeMillis() - 200000))
                .expiration(new Date(System.currentTimeMillis() - 100000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("TC-ADM-01: Admin creates (201), updates (200), deletes (204); train appears in search after create and disappears after delete")
    void testAdminCreateUpdateDeleteTrainLifecycle() throws Exception {
        String futureDate = LocalDate.now().plusDays(2).toString();

        // 1. Admin creates train -> 201 Created
        CreateTrainDto createDto = new CreateTrainDto(
                "22691",
                "Rajdhani Express",
                "Bengaluru",
                "New Delhi",
                LocalTime.of(20, 0, 0),
                LocalTime.of(5, 30, 0),
                List.of(
                        new TrainClassDto("3A", 64, new BigDecimal("2150.00")),
                        new TrainClassDto("2A", 48, new BigDecimal("3100.00"))
                )
        );

        String createResponse = mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.trainNo").value("22691"))
                .andExpect(jsonPath("$.name").value("Rajdhani Express"))
                .andExpect(jsonPath("$.classes", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        Long createdTrainId = objectMapper.readTree(createResponse).get("id").asLong();

        // 2. Search for the train -> Train appears in search
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Bengaluru")
                        .param("dst", "New Delhi")
                        .param("date", futureDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.trainNo == '22691')].name").value(hasItem("Rajdhani Express")));

        // 3. Admin updates train -> 200 OK
        UpdateTrainDto updateDto = new UpdateTrainDto(
                "22691",
                "Bengaluru Rajdhani Express",
                "Bengaluru City",
                "New Delhi",
                LocalTime.of(20, 10, 0),
                LocalTime.of(5, 45, 0),
                List.of(
                        new TrainClassDto("3A", 72, new BigDecimal("2200.00")),
                        new TrainClassDto("1A", 18, new BigDecimal("4500.00"))
                )
        );

        mockMvc.perform(put(ADMIN_TRAINS_URL + "/" + createdTrainId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTrainId))
                .andExpect(jsonPath("$.name").value("Bengaluru Rajdhani Express"))
                .andExpect(jsonPath("$.sourceStation").value("Bengaluru City"))
                .andExpect(jsonPath("$.classes", hasSize(2)));

        // 4. Admin deletes train -> 204 No Content
        mockMvc.perform(delete(ADMIN_TRAINS_URL + "/" + createdTrainId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 5. Search for the train -> Train does NOT appear in search
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Bengaluru City")
                        .param("dst", "New Delhi")
                        .param("date", futureDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.trainNo == '22691')]").doesNotExist());
    }

    @Test
    @DisplayName("TC-SEC-04: Enforce RBAC on POST /api/v1/admin/trains (no token -> 401, passenger -> 403, tampered -> 401, expired -> 401)")
    void testPostEndpointRbac() throws Exception {
        CreateTrainDto dto = sampleCreateDto();
        String json = objectMapper.writeValueAsString(dto);

        // No token -> 401
        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Passenger token -> 403
        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // Tampered token -> 401
        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + TAMPERED_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Expired token -> 401
        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("TC-SEC-04: Enforce RBAC on PUT /api/v1/admin/trains/{id} (no token -> 401, passenger -> 403, tampered -> 401, expired -> 401)")
    void testPutEndpointRbac() throws Exception {
        UpdateTrainDto dto = sampleUpdateDto();
        String json = objectMapper.writeValueAsString(dto);

        // No token -> 401
        mockMvc.perform(put(ADMIN_TRAINS_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Passenger token -> 403
        mockMvc.perform(put(ADMIN_TRAINS_URL + "/1")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // Tampered token -> 401
        mockMvc.perform(put(ADMIN_TRAINS_URL + "/1")
                        .header("Authorization", "Bearer " + TAMPERED_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Expired token -> 401
        mockMvc.perform(put(ADMIN_TRAINS_URL + "/1")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("TC-SEC-04: Enforce RBAC on DELETE /api/v1/admin/trains/{id} (no token -> 401, passenger -> 403, tampered -> 401, expired -> 401)")
    void testDeleteEndpointRbac() throws Exception {
        // No token -> 401
        mockMvc.perform(delete(ADMIN_TRAINS_URL + "/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Passenger token -> 403
        mockMvc.perform(delete(ADMIN_TRAINS_URL + "/1")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // Tampered token -> 401
        mockMvc.perform(delete(ADMIN_TRAINS_URL + "/1")
                        .header("Authorization", "Bearer " + TAMPERED_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Expired token -> 401
        mockMvc.perform(delete(ADMIN_TRAINS_URL + "/1")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Rejects invalid request body with 400 Bad Request")
    void testInvalidBody_Returns400() throws Exception {
        // Blank train number
        CreateTrainDto blankTrainNo = new CreateTrainDto(
                "", "Express", "Src", "Dst",
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                List.of(new TrainClassDto("CC", 50, new BigDecimal("100.00")))
        );

        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankTrainNo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("trainNo")));

        // Empty classes list
        CreateTrainDto emptyClasses = new CreateTrainDto(
                "12345", "Express", "Src", "Dst",
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                Collections.emptyList()
        );

        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyClasses)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("classes")));

        // Invalid nested class: totalSeats < 1 and fare < 0.01
        CreateTrainDto invalidClass = new CreateTrainDto(
                "12345", "Express", "Src", "Dst",
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                List.of(new TrainClassDto("CC", 0, new BigDecimal("0.00")))
        );

        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidClass)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Rejects duplicate trainNo with 409 Conflict")
    void testDuplicateTrainNo_Returns409() throws Exception {
        Train train = new Train("12675", "Existing Train", "Chennai", "Bengaluru",
                LocalTime.of(6, 0), LocalTime.of(11, 0));
        train.addClass(new TrainClass("CC", 100, new BigDecimal("450.00")));
        trainRepository.save(train);

        CreateTrainDto duplicateDto = new CreateTrainDto(
                "12675",
                "Another Train With Same No",
                "Mumbai",
                "Pune",
                LocalTime.of(7, 0),
                LocalTime.of(10, 0),
                List.of(new TrainClassDto("2S", 50, new BigDecimal("120.00")))
        );

        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(containsString("12675")));
    }

    @Test
    @DisplayName("Returns 404 Not Found when updating or deleting unknown train ID")
    void testUnknownId_Returns404() throws Exception {
        UpdateTrainDto updateDto = sampleUpdateDto();

        mockMvc.perform(put(ADMIN_TRAINS_URL + "/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));

        mockMvc.perform(delete(ADMIN_TRAINS_URL + "/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Returns 409 Conflict when deleting a train that has existing bookings")
    void testDeleteTrainWithExistingBookings_Returns409() throws Exception {
        Train train = new Train("12951", "Mumbai Rajdhani", "Mumbai", "Delhi",
                LocalTime.of(17, 0), LocalTime.of(8, 30));
        TrainClass tc = new TrainClass("3A", 100, new BigDecimal("1800.00"));
        train.addClass(tc);
        Train savedTrain = trainRepository.save(train);
        TrainClass savedClass = savedTrain.getClasses().get(0);

        Booking booking = new Booking(
                "PNR12345678",
                passengerUser,
                savedTrain,
                savedClass,
                LocalDate.now().plusDays(5),
                new BigDecimal("1800.00")
        );
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        mockMvc.perform(delete(ADMIN_TRAINS_URL + "/" + savedTrain.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(containsString("bookings exist")));
    }

    private CreateTrainDto sampleCreateDto() {
        return new CreateTrainDto(
                "54321",
                "Sample Express",
                "Station A",
                "Station B",
                LocalTime.of(10, 0, 0),
                LocalTime.of(14, 0, 0),
                List.of(new TrainClassDto("SL", 100, new BigDecimal("250.00")))
        );
    }

    private UpdateTrainDto sampleUpdateDto() {
        return new UpdateTrainDto(
                "54321",
                "Updated Express",
                "Station A",
                "Station B",
                LocalTime.of(10, 30, 0),
                LocalTime.of(14, 30, 0),
                List.of(new TrainClassDto("SL", 120, new BigDecimal("270.00")))
        );
    }
}
