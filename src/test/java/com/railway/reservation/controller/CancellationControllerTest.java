package com.railway.reservation.controller;

import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.CancellationAuditLog;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.Payment;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.CancellationAuditLogRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Issue #12 ticket cancellation (RRS-F-012, TC-CAN-01).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CancellationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CancellationAuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private User otherUser;
    private Train savedTrain;
    private TrainClass ccClass;
    private LocalDate journeyDate;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        trainRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(new User("Booking Owner", "owner@example.com",
                "9876543210", "hashedpwd123", Role.ROLE_PASSENGER));
        otherUser = userRepository.save(new User("Other Passenger", "other@example.com",
                "9876543211", "hashedpwd123", Role.ROLE_PASSENGER));

        Train train = new Train("12675", "Kovai Express", "Chennai", "Bengaluru",
                LocalTime.of(6, 10), LocalTime.of(11, 30));
        ccClass = new TrainClass("CC", 100, new BigDecimal("450.00"));
        train.addClass(ccClass);
        savedTrain = trainRepository.save(train);
        ccClass = savedTrain.getClasses().get(0);

        journeyDate = LocalDate.now().plusDays(3);
    }

    private Booking saveConfirmedBooking(String pnr, LocalDate date) {
        Booking booking = new Booking(pnr, owner, savedTrain, ccClass, date, new BigDecimal("900.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.addPassenger(new Passenger("Alice Smith", 28, "Female", "CC-1"));
        booking.addPassenger(new Passenger("Bob Smith", 32, "Male", "CC-2"));
        booking.setPayment(new Payment(booking, new BigDecimal("900.00"), "TXN-CANCELTEST1", "SUCCESS"));
        return bookingRepository.save(booking);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private int availableCcSeats() throws Exception {
        String json = mockMvc.perform(get("/api/v1/trains/" + savedTrain.getId() + "/availability")
                        .param("class", "CC")
                        .param("date", journeyDate.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.availableSeats");
    }

    @Test
    @DisplayName("TC-CAN-01: Owner cancels confirmed booking -> CANCELLED, seats released, audit logged")
    void testCancelBooking_Success() throws Exception {
        saveConfirmedBooking("PNRCAN0001", journeyDate);
        assertEquals(98, availableCcSeats());

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0001/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNRCAN0001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());

        assertEquals(BookingStatus.CANCELLED,
                bookingRepository.findByPnr("PNRCAN0001").orElseThrow().getStatus());
        assertEquals(100, availableCcSeats());

        List<CancellationAuditLog> audit = auditLogRepository.findByPnr("PNRCAN0001");
        assertEquals(1, audit.size());
        assertEquals(owner.getId(), audit.get(0).getUserId());
    }

    @Test
    @DisplayName("TC-CAN-01: Cancelling twice returns 400 Bad Request")
    void testCancelBooking_AlreadyCancelled() throws Exception {
        saveConfirmedBooking("PNRCAN0002", journeyDate);

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0002/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0002/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Booking is already cancelled."));
    }

    @Test
    @DisplayName("TC-CAN-01: Unknown PNR returns 404 Not Found")
    void testCancelBooking_UnknownPnr() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/NOSUCHPNR1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-CAN-01: Another user's booking returns 404 and stays CONFIRMED")
    void testCancelBooking_NotOwner() throws Exception {
        saveConfirmedBooking("PNRCAN0003", journeyDate);

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0003/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser)))
                .andExpect(status().isNotFound());

        assertEquals(BookingStatus.CONFIRMED,
                bookingRepository.findByPnr("PNRCAN0003").orElseThrow().getStatus());
    }

    @Test
    @DisplayName("TC-CAN-01: Booking whose train already departed returns 400")
    void testCancelBooking_AfterDeparture() throws Exception {
        saveConfirmedBooking("PNRCAN0004", LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0004/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RRS-SR-004: Cancel without a token returns 401 Unauthorized")
    void testCancelBooking_Unauthenticated() throws Exception {
        saveConfirmedBooking("PNRCAN0005", journeyDate);

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0005/cancel"))
                .andExpect(status().isUnauthorized());

        assertEquals(BookingStatus.CONFIRMED,
                bookingRepository.findByPnr("PNRCAN0005").orElseThrow().getStatus());
    }

    @Test
    @DisplayName("RRS-SR-003: Script payload in the PNR path is rejected with 400")
    void testCancelBooking_UnsafePnr() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/{pnr}/cancel", "<script>alert(1)")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest());
    }
}
