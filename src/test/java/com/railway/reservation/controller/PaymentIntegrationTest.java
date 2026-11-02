package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.dto.PaymentCallbackRequestDto;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.PaymentRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private Train testTrain;
    private TrainClass testClass;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User(
                "Shakthi Test", "shakthi.pay.test@example.com", "9876543210", "$2a$10$hashedPass", Role.ROLE_PASSENGER));

        testTrain = new Train("20607", "Vande Bharat Express", "Chennai", "Bengaluru",
                LocalTime.of(5, 45), LocalTime.of(10, 10));
        testClass = new TrainClass("CC", 100, new BigDecimal("920.00"));
        testTrain.addClass(testClass);
        testTrain = trainRepository.save(testTrain);
    }

    @Test
    @DisplayName("TC-BK-01: End-to-end booking -> payment callback SUCCESS -> booking CONFIRMED")
    void testEndToEnd_PaymentSuccess() throws Exception {
        LocalDate journeyDate = LocalDate.now().plusDays(5);

        String token = jwtService.generateToken(testUser);

        // Step 1: Create booking (Seats held, status PENDING, payment initiated)
        BookingRequestDto bookingRequest = new BookingRequestDto(
                testUser.getId(),
                testTrain.getId(),
                testClass.getId(),
                journeyDate,
                List.of(
                        new PassengerDto("Ravi Kumar", 29, "Male"),
                        new PassengerDto("Priya Kumar", 27, "Female")
                )
        );

        MvcResult bookingResult = mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pnr").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").isNotEmpty())
                .andExpect(jsonPath("$.gatewayRef").isNotEmpty())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(bookingResult.getResponse().getContentAsString());
        String pnr = jsonNode.get("pnr").asText();
        String gatewayRef = jsonNode.get("gatewayRef").asText();
        BigDecimal totalFare = new BigDecimal(jsonNode.get("totalFare").asText());

        // Step 2: Gateway sends SUCCESS callback
        PaymentCallbackRequestDto callbackDto = new PaymentCallbackRequestDto(
                gatewayRef, pnr, "SUCCESS", totalFare);

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        // Step 3: Verify booking is now CONFIRMED via PNR query
        mockMvc.perform(get("/api/v1/bookings/" + pnr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value(pnr))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("TC-PAY-02: End-to-end booking -> payment callback FAILED -> booking FAILED & held seats released")
    void testEndToEnd_PaymentFailed_ReleasesSeats() throws Exception {
        LocalDate journeyDate = LocalDate.now().plusDays(6);

        // Check seats available before booking
        MvcResult searchBefore = mockMvc.perform(get("/api/v1/trains/search")
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", journeyDate.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode trainsArray = objectMapper.readTree(searchBefore.getResponse().getContentAsString());
        int availSeatsInitial = -1;
        for (JsonNode t : trainsArray) {
            if ("20607".equals(t.get("trainNo").asText())) {
                for (JsonNode c : t.get("classes")) {
                    if ("CC".equals(c.get("className").asText())) {
                        availSeatsInitial = c.get("availableSeats").asInt();
                    }
                }
            }
        }

        String token = jwtService.generateToken(testUser);

        // Step 1: Book 2 seats -> status PENDING, seats locked
        BookingRequestDto bookingRequest = new BookingRequestDto(
                testUser.getId(),
                testTrain.getId(),
                testClass.getId(),
                journeyDate,
                List.of(
                        new PassengerDto("Anita Verma", 31, "Female"),
                        new PassengerDto("Suresh Verma", 35, "Male")
                )
        );

        MvcResult bookingResult = mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode bookingJson = objectMapper.readTree(bookingResult.getResponse().getContentAsString());
        String pnr = bookingJson.get("pnr").asText();
        String gatewayRef = bookingJson.get("gatewayRef").asText();
        BigDecimal totalFare = new BigDecimal(bookingJson.get("totalFare").asText());

        // Check seats available while PENDING: should be decreased by 2
        MvcResult searchWhilePending = mockMvc.perform(get("/api/v1/trains/search")
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", journeyDate.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode pendingArray = objectMapper.readTree(searchWhilePending.getResponse().getContentAsString());
        for (JsonNode t : pendingArray) {
            if ("20607".equals(t.get("trainNo").asText())) {
                for (JsonNode c : t.get("classes")) {
                    if ("CC".equals(c.get("className").asText())) {
                        assertEquals(availSeatsInitial - 2, c.get("availableSeats").asInt(),
                                "Held seats must decrease availability while PENDING");
                    }
                }
            }
        }

        // Step 2: Payment Gateway sends FAILED callback (user cancelled or payment declined)
        PaymentCallbackRequestDto callbackDto = new PaymentCallbackRequestDto(
                gatewayRef, pnr, "FAILED", totalFare);

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.bookingStatus").value("FAILED"));

        // Step 3: Verify booking is marked FAILED via PNR query
        mockMvc.perform(get("/api/v1/bookings/" + pnr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        // Step 4: Verify held seats are IMMEDIATELY RELEASED back to the availability pool
        MvcResult searchAfterFailure = mockMvc.perform(get("/api/v1/trains/search")
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", journeyDate.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode releasedArray = objectMapper.readTree(searchAfterFailure.getResponse().getContentAsString());
        for (JsonNode t : releasedArray) {
            if ("20607".equals(t.get("trainNo").asText())) {
                for (JsonNode c : t.get("classes")) {
                    if ("CC".equals(c.get("className").asText())) {
                        assertEquals(availSeatsInitial, c.get("availableSeats").asInt(),
                                "Seats must be released back to the original available count after payment failure (TC-PAY-02)");
                    }
                }
            }
        }
    }
}

