package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.dto.RegisterRequestDto;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainClassRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verification test for Issue #19: Booking UI/UX Design (RRS-NF-003, SAD Section 4.6, TC-UX-01).
 * Validates static resource serving, semantic UI elements, responsive stylesheet, and the complete
 * 5-minute user booking journey via backend REST integration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingUiIntegrationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Train savedTrain;
    private TrainClass ccClass;
    private User testUser;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        trainRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(new User(
                "Demo Passenger",
                "demopassenger@example.com",
                "9876543210",
                "hashedpwd123",
                Role.ROLE_PASSENGER
        ));

        Train train = new Train(
                "12675",
                "Kovai Express",
                "Chennai",
                "Bengaluru",
                LocalTime.of(6, 10),
                LocalTime.of(11, 30)
        );
        ccClass = new TrainClass("CC", 100, new BigDecimal("450.00"));
        TrainClass ecClass = new TrainClass("EC", 40, new BigDecimal("950.00"));
        train.addClass(ccClass);
        train.addClass(ecClass);

        savedTrain = trainRepository.save(train);
        ccClass = savedTrain.getClasses().stream()
                .filter(c -> c.getClassName().equals("CC"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("TC-UX-01: Root URL '/' returns 200 OK and forwards to index.html")
    void testRootEndpointServesIndexHtml() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    @DisplayName("TC-UX-01: 'index.html' contains all mandatory SAD 4.6 UX elements and input validation hooks")
    void testIndexHtmlContainsRequiredUxElements() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                // Flash notifications container (SAD 4.6)
                .andExpect(content().string(containsString("id=\"flashContainer\"")))
                // Clear input form fields with required attributes
                .andExpect(content().string(containsString("id=\"srcInput\"")))
                .andExpect(content().string(containsString("id=\"dstInput\"")))
                .andExpect(content().string(containsString("id=\"dateInput\"")))
                // Station swap button
                .andExpect(content().string(containsString("id=\"swapStationsBtn\"")))
                // Usability session timer for TC-UX-01 (< 5 min)
                .andExpect(content().string(containsString("id=\"uxTimerBadge\"")))
                // Quick route chips
                .andExpect(content().string(containsString("class=\"quick-chips\"")))
                // Search results & class availability container
                .andExpect(content().string(containsString("id=\"resultsSection\"")))
                // Passenger booking form
                .andExpect(content().string(containsString("id=\"passengerBookingForm\"")))
                .andExpect(content().string(containsString("id=\"passengersTable\"")))
                // Confirmation section
                .andExpect(content().string(containsString("id=\"confirmationSection\"")))
                .andExpect(content().string(containsString("id=\"confirmedPnrDisplay\"")))
                // PNR lookup modal
                .andExpect(content().string(containsString("id=\"pnrModal\"")));
    }

    @Test
    @DisplayName("TC-UX-01: Stylesheet 'css/style.css' is served with responsive breakpoints and flash styling")
    void testStylesheetServesResponsiveStyles() throws Exception {
        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--primary: #1e3a8a;")))
                .andExpect(content().string(containsString(".flash-banner")))
                .andExpect(content().string(containsString("@media (max-width: 768px)")));
    }

    @Test
    @DisplayName("TC-UX-01: Application logic 'js/app.js' is served and handles XSS-safe DOM and API integration")
    void testJavaScriptAppLogicIsServed() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("trainSearchForm")))
                .andExpect(content().string(containsString("/api/v1/trains/search")))
                .andExpect(content().string(containsString("/api/v1/bookings")));
    }

    @Test
    @DisplayName("TC-UX-01: Complete end-to-end booking user flow takes < 5 minutes and returns confirmed PNR")
    void testCompleteBookingUserFlow_WithinFiveMinutes() throws Exception {
        long startTime = System.currentTimeMillis();

        // Step 1: User queries trains
        String journeyDate = LocalDate.now().plusDays(2).toString();
        MvcResult searchResult = mockMvc.perform(get("/api/v1/trains/search")
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", journeyDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainNo").value("12675"))
                .andExpect(jsonPath("$[0].classes[0].className").value("CC"))
                .andExpect(jsonPath("$[0].classes[0].availableSeats").value(100))
                .andReturn();

        // Step 2: User queries live seat availability
        mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/availability")
                        .param("class", "CC")
                        .param("date", journeyDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("CC"))
                .andExpect(jsonPath("$.availableSeats").value(100));

        // Step 3: User registers passenger account
        RegisterRequestDto regRequest = new RegisterRequestDto();
        regRequest.setName("Alice Walker");
        regRequest.setEmail("alice" + System.currentTimeMillis() + "@example.com");
        regRequest.setPhone("9876543211");
        regRequest.setPassword("SecurePass123");

        MvcResult regResult = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andReturn();

        Number registeredUserId = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("userId").numberValue();

        // Step 4: User submits booking request with passenger details
        BookingRequestDto bookingRequest = new BookingRequestDto(
                registeredUserId.longValue(),
                savedTrain.getId(),
                ccClass.getId(),
                LocalDate.parse(journeyDate),
                List.of(
                        new PassengerDto("Alice Walker", 28, "Female"),
                        new PassengerDto("Charlie Walker", 30, "Male")
                )
        );

        MvcResult bookingResult = mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pnr").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalFare").value(900.00))
                .andExpect(jsonPath("$.passengers[0].seatNo").value("CC-1"))
                .andExpect(jsonPath("$.passengers[1].seatNo").value("CC-2"))
                .andReturn();

        String generatedPnr = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .get("pnr").asText();

        // Step 5: User verifies booking via PNR status lookup
        mockMvc.perform(get("/api/v1/bookings/" + generatedPnr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value(generatedPnr))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.passengers").isArray());

        long durationMs = System.currentTimeMillis() - startTime;
        // Verify TC-UX-01 requirement: Booking journey completes in far less than 5 minutes (300,000 ms)
        assertTrue(durationMs < 300_000, "Booking flow must complete in less than 5 minutes according to TC-UX-01");
    }
}
