package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.FareQuoteRequestDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.dto.RegisterRequestDto;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.Payment;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.PaymentRepository;
import com.railway.reservation.repository.TrainClassRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verification tests for Issue #19 (Booking UI/UX, RRS-NF-003) and the Issue #10
 * ticket-access integration paths (public PNR redaction, verified-payment PDF,
 * and pending-payment rejection).
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

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtService jwtService;

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
                .andExpect(content().string(containsString("id=\"confPaymentStatus\"")))
                .andExpect(content().string(containsString("id=\"downloadPdfTicketBtn\"")))
                // Existing-user login UI for full-ticket/PDF access
                .andExpect(content().string(containsString("id=\"openLoginModalBtn\"")))
                .andExpect(content().string(containsString("id=\"loginModal\"")))
                .andExpect(content().string(containsString("id=\"loginForm\"")))
                .andExpect(content().string(containsString("id=\"loginEmail\"")))
                .andExpect(content().string(containsString("id=\"loginPassword\"")))
                .andExpect(content().string(containsString("id=\"loginSubmitBtn\"")))
                // PNR lookup modal
                .andExpect(content().string(containsString("id=\"pnrModal\"")))
                // RRS-F-004-UI: Station autocomplete combobox dropdowns & Route stops modal
                .andExpect(content().string(containsString("id=\"srcSuggestions\"")))
                .andExpect(content().string(containsString("id=\"dstSuggestions\"")))
                .andExpect(content().string(containsString("id=\"routeModal\"")))
                .andExpect(content().string(containsString("id=\"chipDateToday\"")))
                .andExpect(content().string(containsString("id=\"chipDateTomorrow\"")))
                .andExpect(content().string(containsString("id=\"chipDateWeekend\"")))
                // RRS-F-007-EXT: Sticky commercial fare summary card & breakdown elements
                .andExpect(content().string(containsString("id=\"fareBreakdownCard\"")))
                .andExpect(content().string(containsString("id=\"breakdownBaseFare\"")))
                .andExpect(content().string(containsString("id=\"breakdownSuperfastCharge\"")))
                .andExpect(content().string(containsString("id=\"breakdownReservationFee\"")))
                .andExpect(content().string(containsString("id=\"breakdownGst\"")))
                .andExpect(content().string(containsString("id=\"breakdownSeniorRow\"")))
                .andExpect(content().string(containsString("id=\"breakdownSeniorDiscount\"")));
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
                .andExpect(content().string(containsString("/api/v1/bookings")))
                // RRS-F-010: authenticated full-ticket/PDF access and token handling
                .andExpect(content().string(containsString("/api/v1/users/login")))
                .andExpect(content().string(containsString("sessionStorage.setItem('jwtToken'")))
                .andExpect(content().string(containsString("encodeURIComponent(pnr)}/ticket")))
                .andExpect(content().string(containsString("/ticket.pdf")))
                .andExpect(content().string(containsString("isTokenExpired")))
                .andExpect(content().string(containsString("clearStoredToken")))
                .andExpect(content().string(containsString("seatNumbers")))
                .andExpect(content().string(containsString("booking.paymentStatus")))
                // RRS-F-004-UI: Station autocomplete, intermediate stops modal, and skeleton loading
                .andExpect(content().string(containsString("/api/v1/trains/stations")))
                .andExpect(content().string(containsString("setupStationAutocomplete")))
                .andExpect(content().string(containsString("openRouteModal")))
                .andExpect(content().string(containsString("renderSkeletonLoading")));
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
        User registeredUser = userRepository.findById(registeredUserId.longValue()).orElseThrow();
        String authToken = jwtService.generateToken(registeredUser);

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
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pnr").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").isNotEmpty())
                .andExpect(jsonPath("$.totalFare").value(900.00))
                .andExpect(jsonPath("$.passengers[0].seatNo").value("CC-1"))
                .andExpect(jsonPath("$.passengers[1].seatNo").value("CC-2"))
                .andReturn();

        String generatedPnr = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .get("pnr").asText();

        // Step 5: User verifies booking via public PNR status lookup
        mockMvc.perform(get("/api/v1/bookings/" + generatedPnr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value(generatedPnr))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalFare").value(900.00))
                .andExpect(jsonPath("$.passengers.length()").value(2))
                .andExpect(jsonPath("$.passengers[0].seatNo").value("CC-1"))
                .andExpect(jsonPath("$.passengers[1].seatNo").value("CC-2"));

        long durationMs = System.currentTimeMillis() - startTime;
        // Verify TC-UX-01 requirement: Booking journey completes in far less than 5 minutes (300,000 ms)
        assertTrue(durationMs < 300_000, "Booking flow must complete in less than 5 minutes according to TC-UX-01");
    }

    @Test
    @DisplayName("TC-BK-03: Verified payment allows owner ticket JSON and a valid PDF")
    void testVerifiedPaymentAllowsOwnerTicketAndPdf() throws Exception {
        createBooking("VERIFIED01", "SUCCESS", BookingStatus.CONFIRMED);
        String token = jwtService.generateToken(testUser);

        mockMvc.perform(get("/api/v1/bookings/VERIFIED01/ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.pnr").value("VERIFIED01"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        MvcResult pdfResult = mockMvc.perform(get("/api/v1/bookings/VERIFIED01/ticket.pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ticket-VERIFIED01.pdf\""))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 4, "Generated PDF should contain bytes");
        assertEquals("%PDF", new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("TC-BK-03: Pending payment blocks owner ticket JSON and PDF")
    void testPendingPaymentBlocksOwnerTicketAndPdf() throws Exception {
        createBooking("PENDING001", "PENDING", BookingStatus.PENDING);
        String token = jwtService.generateToken(testUser);

        mockMvc.perform(get("/api/v1/bookings/PENDING001/ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Payment has not been verified for this booking."));

        mockMvc.perform(get("/api/v1/bookings/PENDING001/ticket.pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-HIS-01: End-to-end booking history retrieves user's past and upcoming bookings sorted chronologically [RRS-F-014]")
    void testBookingHistory_EndToEnd_ChronologicalAndUserIsolated() throws Exception {
        // 1. Create upcoming booking for testUser
        createBookingWithDate("UPCOMING01", "SUCCESS", BookingStatus.CONFIRMED, LocalDate.now().plusDays(5));

        // 2. Create past booking for testUser
        createBookingWithDate("PAST000001", "SUCCESS", BookingStatus.CONFIRMED, LocalDate.now().minusDays(2));

        // 3. Create booking for a DIFFERENT user
        User otherUser = userRepository.save(new User(
                "Other Person", "otherperson@example.com", "9111111111", "hash", Role.ROLE_PASSENGER));
        Booking otherBooking = new Booking("OTHER00001", otherUser, savedTrain, ccClass,
                LocalDate.now().plusDays(3), new BigDecimal("450.00"));
        otherBooking.setStatus(BookingStatus.CONFIRMED);
        otherBooking.addPassenger(new Passenger("Other Pax", 30, "Male", "CC-2"));
        bookingRepository.save(otherBooking);

        // 4. Authenticate as testUser
        String token = jwtService.generateToken(testUser);

        // 5. Query booking history
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pnr").value("UPCOMING01"))
                .andExpect(jsonPath("$[0].upcoming").value(true))
                .andExpect(jsonPath("$[0].cancellable").value(true))
                .andExpect(jsonPath("$[0].ticketUrl").value("/ticket.html?pnr=UPCOMING01"))
                .andExpect(jsonPath("$[0].passengers[0].name").value("Alice Walker"))
                .andExpect(jsonPath("$[1].pnr").value("PAST000001"))
                .andExpect(jsonPath("$[1].upcoming").value(false))
                .andExpect(jsonPath("$[1].cancellable").value(false));

        // 6. Verify index.html contains UI elements for booking history
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"openHistoryModalBtn\"")))
                .andExpect(content().string(containsString("id=\"historyModal\"")))
                .andExpect(content().string(containsString("id=\"historyListContainer\"")));
    }

    private void createBookingWithDate(String pnr, String paymentStatus, BookingStatus bookingStatus, LocalDate journeyDate) {
        Booking booking = new Booking(pnr, testUser, savedTrain, ccClass,
                journeyDate, new BigDecimal("450.00"));
        booking.setStatus(bookingStatus);
        booking.addPassenger(new Passenger("Alice Walker", 28, "Female", "CC-1"));
        booking = bookingRepository.save(booking);

        Payment payment = new Payment(booking, booking.getTotalFare(), "TXN-" + pnr, paymentStatus);
        paymentRepository.save(payment);
        booking.setPayment(payment);
    }

    private void createBooking(String pnr, String paymentStatus, BookingStatus bookingStatus) {
        Booking booking = new Booking(pnr, testUser, savedTrain, ccClass,
                LocalDate.now().plusDays(2), new BigDecimal("450.00"));
        booking.setStatus(bookingStatus);
        booking.addPassenger(new Passenger("Alice Walker", 28, "Female", "CC-1"));
        booking = bookingRepository.save(booking);

        Payment payment = new Payment(booking, booking.getTotalFare(), "TXN-" + pnr, paymentStatus);
        paymentRepository.save(payment);
        booking.setPayment(payment);
    }

    @Test
    @DisplayName("TC-BKG-01: End-to-end fare quote preview returns itemized breakdown [RRS-F-007-EXT]")
    void testFareQuoteEndpoint_EndToEndIntegration() throws Exception {
        FareQuoteRequestDto quoteRequest = new FareQuoteRequestDto(
                ccClass.getId(),
                List.of(
                        new PassengerDto("Ramesh Kumar", 64, "Male", null, "LOWER", "VEG", true),
                        new PassengerDto("Priya Kumar", 30, "Female", null, "UPPER", "NON_VEG", false)
                )
        );

        mockMvc.perform(post("/api/v1/bookings/fare-quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quoteRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.passengerCount").value(2))
                .andExpect(jsonPath("$.farePerSeat").value(450.0))
                .andExpect(jsonPath("$.eligibleSeniorCount").value(1))
                .andExpect(jsonPath("$.baseFare").exists())
                .andExpect(jsonPath("$.superfastCharge").exists())
                .andExpect(jsonPath("$.reservationFee").exists())
                .andExpect(jsonPath("$.gst").exists())
                .andExpect(jsonPath("$.seniorDiscount").exists())
                .andExpect(jsonPath("$.totalFare").exists());
    }

    @Test
    @DisplayName("TC-BKG-02: End-to-end booking stores berth preference, meal choice, and senior concession [RRS-F-007-EXT]")
    void testCreateBooking_EndToEnd_WithPreferencesAndConcession() throws Exception {
        String token = jwtService.generateToken(testUser);

        BookingRequestDto bookingRequest = new BookingRequestDto(
                testUser.getId(),
                savedTrain.getId(),
                ccClass.getId(),
                LocalDate.now().plusDays(4),
                List.of(
                        new PassengerDto("Venkatesh Rao", 67, "Male", null, "SIDE_LOWER", "VEG", true)
                )
        );

        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pnr").exists())
                .andExpect(jsonPath("$.passengers[0].berthPreference").value("SIDE_LOWER"))
                .andExpect(jsonPath("$.passengers[0].mealPreference").value("VEG"))
                .andExpect(jsonPath("$.passengers[0].seniorConcession").value(true))
                .andExpect(jsonPath("$.seniorDiscount").exists())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String createdPnr = objectMapper.readTree(responseJson).get("pnr").asText();

        Booking persisted = bookingRepository.findByPnr(createdPnr).orElseThrow();
        assertEquals(1, persisted.getPassengers().size());
        Passenger passenger = persisted.getPassengers().get(0);
        assertEquals("SIDE_LOWER", passenger.getBerthPreference());
        assertEquals("VEG", passenger.getMealPreference());
        assertTrue(passenger.getSeniorConcession());
        assertNotNull(persisted.getBaseFare());
        assertNotNull(persisted.getSuperfastCharge());
        assertNotNull(persisted.getReservationFee());
        assertNotNull(persisted.getGst());
        assertNotNull(persisted.getSeniorDiscount());
    }
}
