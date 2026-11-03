package com.railway.reservation.controller;

import com.railway.reservation.model.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchControllerTest {

    private static final String SEARCH_URL = "/api/v1/trains/search";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainClassRepository trainClassRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private Train savedTrain;
    private TrainClass ccClass;
    private TrainClass ecClass;
    private User testUser;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        trainRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(new User("Passenger One", "passenger1@example.com", "9876543210", "hashedpw", Role.ROLE_PASSENGER));

        Train train = new Train("12675", "Kovai Express", "Chennai", "Bengaluru",
                LocalTime.of(6, 10), LocalTime.of(11, 30));
        ccClass = new TrainClass("CC", 100, new BigDecimal("450.00"));
        ecClass = new TrainClass("EC", 40, new BigDecimal("950.00"));
        train.addClass(ccClass);
        train.addClass(ecClass);

        savedTrain = trainRepository.save(train);
        // refresh references
        ccClass = savedTrain.getClasses().stream().filter(c -> c.getClassName().equals("CC")).findFirst().orElseThrow();
        ecClass = savedTrain.getClasses().stream().filter(c -> c.getClassName().equals("EC")).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("TC-SRC-01: Guest/user searches trains by route and date, receives 200 OK with timings and classes")
    void searchTrains_successful() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", tomorrow)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trainNo").value("12675"))
                .andExpect(jsonPath("$[0].name").value("Kovai Express"))
                .andExpect(jsonPath("$[0].sourceStation").value("Chennai"))
                .andExpect(jsonPath("$[0].destinationStation").value("Bengaluru"))
                .andExpect(jsonPath("$[0].departureTime").value("06:10:00"))
                .andExpect(jsonPath("$[0].arrivalTime").value("11:30:00"))
                .andExpect(jsonPath("$[0].classes", hasSize(2)))
                .andExpect(jsonPath("$[0].classes[0].className").value("CC"))
                .andExpect(jsonPath("$[0].classes[0].availableSeats").value(100))
                .andExpect(jsonPath("$[0].classes[0].fare").value(450.00));
    }

    @Test
    @DisplayName("TC-SRC-02: Search for non-existent route returns 200 OK with empty list and notice header")
    void searchTrains_noTrainsFound() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Delhi")
                        .param("dst", "Goa")
                        .param("date", tomorrow)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Search-Message", "No trains found for selected route and date"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("TC-PERF-01: Train search response completes well within 3 seconds")
    void searchTrains_performanceCheck() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", tomorrow))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;
        org.assertj.core.api.Assertions.assertThat(duration).isLessThan(3000);
    }

    @Test
    @DisplayName("Rejects missing src parameter with 400 Bad Request")
    void searchTrains_missingSrc() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("dst", "Bengaluru")
                        .param("date", tomorrow))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source station")));
    }

    @Test
    @DisplayName("Rejects missing dst parameter with 400 Bad Request")
    void searchTrains_missingDst() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("date", tomorrow))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Destination station")));
    }

    @Test
    @DisplayName("Rejects missing date parameter with 400 Bad Request")
    void searchTrains_missingDate() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Journey date")));
    }

    @Test
    @DisplayName("Rejects invalid date format with 400 Bad Request")
    void searchTrains_invalidDateFormat() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", "21-10-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid journey date format")));
    }

    @Test
    @DisplayName("Rejects identical source and destination with 400 Bad Request")
    void searchTrains_identicalSourceAndDestination() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "chennai")
                        .param("date", tomorrow))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source and destination stations cannot be the same")));
    }

    @Test
    @DisplayName("Rejects past journey date with 400 Bad Request")
    void searchTrains_pastDate() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", yesterday))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Journey date cannot be in the past")));
    }

    @Test
    @DisplayName("Case-insensitive search returns matching trains")
    void searchTrains_caseInsensitive() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "chEnNaI")
                        .param("dst", "BeNgaLuRu")
                        .param("date", tomorrow))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trainNo").value("12675"));
    }

    // ====================================================
    // RRS-F-005 Seat Availability Endpoint & Integration Tests
    // ====================================================

    @Test
    @DisplayName("RRS-F-005: GET /api/v1/trains/{id}/availability returns available seats and fare")
    void getSeatAvailability_successful() throws Exception {
        String journeyDate = LocalDate.now().plusDays(3).toString();

        mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/availability")
                        .param("class", "CC")
                        .param("date", journeyDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("CC"))
                .andExpect(jsonPath("$.totalSeats").value(100))
                .andExpect(jsonPath("$.availableSeats").value(100))
                .andExpect(jsonPath("$.fare").value(450.00));
    }

    @Test
    @DisplayName("RRS-F-005: Dynamically computes available seats excluding CANCELLED and FAILED bookings")
    void getSeatAvailability_dynamicComputationWithBookings() throws Exception {
        LocalDate travelDate = LocalDate.now().plusDays(4);

        // 1. CONFIRMED booking with 3 passengers
        Booking confirmedBooking = new Booking("PNR0000001", testUser, savedTrain, ccClass, travelDate, new BigDecimal("1350.00"));
        confirmedBooking.setStatus(BookingStatus.CONFIRMED);
        confirmedBooking.addPassenger(new Passenger("P1", 25, "M", "CC-1"));
        confirmedBooking.addPassenger(new Passenger("P2", 26, "F", "CC-2"));
        confirmedBooking.addPassenger(new Passenger("P3", 27, "M", "CC-3"));
        bookingRepository.save(confirmedBooking);

        // 2. PENDING booking with 2 passengers (also holds seats)
        Booking pendingBooking = new Booking("PNR0000002", testUser, savedTrain, ccClass, travelDate, new BigDecimal("900.00"));
        pendingBooking.setStatus(BookingStatus.PENDING);
        pendingBooking.addPassenger(new Passenger("P4", 30, "M", "CC-4"));
        pendingBooking.addPassenger(new Passenger("P5", 31, "F", "CC-5"));
        bookingRepository.save(pendingBooking);

        // 3. CANCELLED booking with 4 passengers (MUST BE EXCLUDED)
        Booking cancelledBooking = new Booking("PNR0000003", testUser, savedTrain, ccClass, travelDate, new BigDecimal("1800.00"));
        cancelledBooking.setStatus(BookingStatus.CANCELLED);
        cancelledBooking.addPassenger(new Passenger("P6", 35, "M", "CC-6"));
        cancelledBooking.addPassenger(new Passenger("P7", 36, "F", "CC-7"));
        cancelledBooking.addPassenger(new Passenger("P8", 37, "M", "CC-8"));
        cancelledBooking.addPassenger(new Passenger("P9", 38, "F", "CC-9"));
        bookingRepository.save(cancelledBooking);

        // 4. FAILED booking with 2 passengers (MUST BE EXCLUDED)
        Booking failedBooking = new Booking("PNR0000004", testUser, savedTrain, ccClass, travelDate, new BigDecimal("900.00"));
        failedBooking.setStatus(BookingStatus.FAILED);
        failedBooking.addPassenger(new Passenger("P10", 40, "M", "CC-10"));
        failedBooking.addPassenger(new Passenger("P11", 41, "F", "CC-11"));
        bookingRepository.save(failedBooking);

        // 5. Booking on a DIFFERENT date (MUST NOT affect travelDate)
        Booking diffDateBooking = new Booking("PNR0000005", testUser, savedTrain, ccClass, travelDate.plusDays(2), new BigDecimal("450.00"));
        diffDateBooking.setStatus(BookingStatus.CONFIRMED);
        diffDateBooking.addPassenger(new Passenger("P12", 45, "M", "CC-1"));
        bookingRepository.save(diffDateBooking);

        // 6. Booking on a DIFFERENT class (EC instead of CC) (MUST NOT affect CC)
        Booking diffClassBooking = new Booking("PNR0000006", testUser, savedTrain, ecClass, travelDate, new BigDecimal("950.00"));
        diffClassBooking.setStatus(BookingStatus.CONFIRMED);
        diffClassBooking.addPassenger(new Passenger("P13", 50, "F", "EC-1"));
        bookingRepository.save(diffClassBooking);

        // Expected booked seats for CC on travelDate: 3 (CONFIRMED) + 2 (PENDING) = 5
        // Expected available seats for CC: 100 - 5 = 95
        mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/availability")
                        .param("class", "CC")
                        .param("date", travelDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("CC"))
                .andExpect(jsonPath("$.totalSeats").value(100))
                .andExpect(jsonPath("$.availableSeats").value(95));

        // Also verify the main search endpoint dynamically reflects 95 available seats
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", travelDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].classes[?(@.className == 'CC')].availableSeats").value(contains(95)))
                .andExpect(jsonPath("$[0].classes[?(@.className == 'EC')].availableSeats").value(contains(39)));
    }

    @Test
    @DisplayName("RRS-F-005: Availability endpoint returns 404 for non-existent train")
    void getSeatAvailability_trainNotFound() throws Exception {
        String journeyDate = LocalDate.now().plusDays(2).toString();

        mockMvc.perform(get("/api/v1/trains/99999/availability")
                        .param("class", "CC")
                        .param("date", journeyDate))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Train not found with ID: 99999")));
    }

    @Test
    @DisplayName("RRS-F-005: Availability endpoint returns 404 for non-existent class")
    void getSeatAvailability_classNotFound() throws Exception {
        String journeyDate = LocalDate.now().plusDays(2).toString();

        mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/availability")
                        .param("class", "3A")
                        .param("date", journeyDate))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Class '3A' not found for train")));
    }

    @Test
    @DisplayName("RRS-F-005: Availability endpoint returns 400 for missing class parameter")
    void getSeatAvailability_missingClass() throws Exception {
        String journeyDate = LocalDate.now().plusDays(2).toString();

        mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/availability")
                        .param("date", journeyDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Class name ('class') parameter is required")));
    }

    @Test
    @DisplayName("RRS-F-005: Availability endpoint returns 400 for missing date parameter")
    void getSeatAvailability_missingDate() throws Exception {
        mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/availability")
                        .param("class", "CC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Journey date ('date') parameter is required")));
    }

    @Test
    @DisplayName("RRS-F-005: Availability endpoint returns 400 for past date")
    void getSeatAvailability_pastDate() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/availability")
                        .param("class", "CC")
                        .param("date", yesterday))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Journey date cannot be in the past")));
    }

    @Test
    @DisplayName("TC-SCH-01: GET /api/v1/trains/stations returns 200 OK with master station catalog")
    void getMasterStations_returnsCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/trains/stations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(20))))
                .andExpect(jsonPath("$[0].code").isNotEmpty())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    @DisplayName("TC-SCH-03: Search trains by station codes (MAS -> SBC) returns matching train")
    void searchTrains_byStationCodes() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "MAS")
                        .param("dst", "SBC")
                        .param("date", tomorrow)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trainNo").value("12675"));
    }

    @Test
    @DisplayName("RRS-F-004-UI: GET /api/v1/trains/{id}/route returns 200 OK with ordered stops and timetable")
    void getTrainRoute_successful() throws Exception {
        savedTrain.addStop(new TrainStop(1, "MAS", "Chennai Central", null, LocalTime.of(6, 10), 0, 4, 0));
        savedTrain.addStop(new TrainStop(2, "KPD", "Katpadi Junction", LocalTime.of(7, 58), LocalTime.of(8, 0), 2, 1, 130));
        savedTrain.addStop(new TrainStop(3, "SBC", "KSR Bengaluru", LocalTime.of(11, 30), null, 0, 3, 359));
        trainRepository.save(savedTrain);

        mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/route")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].stopOrder").value(1))
                .andExpect(jsonPath("$[0].stationCode").value("MAS"))
                .andExpect(jsonPath("$[0].stationName").value("Chennai Central"))
                .andExpect(jsonPath("$[0].departureTime").value("06:10:00"))
                .andExpect(jsonPath("$[1].stationCode").value("KPD"))
                .andExpect(jsonPath("$[1].haltMinutes").value(2))
                .andExpect(jsonPath("$[1].platformNo").value(1))
                .andExpect(jsonPath("$[1].distanceKm").value(130))
                .andExpect(jsonPath("$[2].stationCode").value("SBC"))
                .andExpect(jsonPath("$[2].distanceKm").value(359));
    }

    @Test
    @DisplayName("RRS-F-004-UI: GET /api/v1/trains/{id}/route returns 404 Not Found for non-existent train")
    void getTrainRoute_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/trains/99999/route")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Train not found with ID: 99999")));
    }
}
