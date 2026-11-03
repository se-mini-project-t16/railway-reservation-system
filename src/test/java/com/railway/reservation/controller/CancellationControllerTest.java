package com.railway.reservation.controller;

import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.CancellationAuditLog;
import com.railway.reservation.model.CancellationRule;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.Payment;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.CancellationAuditLogRepository;
import com.railway.reservation.repository.CancellationRuleRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Issue #12 ticket cancellation (RRS-F-012, TC-CAN-01)
 * and Issue #13 rule-based refunds (RRS-F-013, TC-CAN-02).
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
    private CancellationRuleRepository cancellationRuleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

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

        // Departure is at least 54h away, so the 48h tier (90%) applies
        journeyDate = LocalDate.now().plusDays(3);

        cancellationRuleRepository.deleteAll();
        cancellationRuleRepository.saveAll(List.of(
                new CancellationRule(48, new BigDecimal("90.00")),
                new CancellationRule(24, new BigDecimal("75.00")),
                new CancellationRule(0, new BigDecimal("0.00"))));
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

    @Test
    @DisplayName("TC-CAN-02: Refund preview shows the rule-based amount before cancelling and changes nothing")
    void testPreviewRefund_ShownBeforeCancel() throws Exception {
        Booking booking = saveConfirmedBooking("PNRCAN0006", journeyDate);

        mockMvc.perform(get("/api/v1/bookings/PNRCAN0006/refund-preview")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNRCAN0006"))
                .andExpect(jsonPath("$.totalFare").value(900.0))
                .andExpect(jsonPath("$.refundPercent").value(90.0))
                .andExpect(jsonPath("$.refundAmount").value(810.0));

        assertEquals(BookingStatus.CONFIRMED,
                bookingRepository.findByPnr("PNRCAN0006").orElseThrow().getStatus());
        assertEquals("SUCCESS",
                paymentRepository.findByBookingId(booking.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("TC-CAN-01/TC-CAN-02: Cancel returns {pnr, status: CANCELLED, refund} and initiates the refund")
    void testCancelBooking_InitiatesRefund() throws Exception {
        Booking booking = saveConfirmedBooking("PNRCAN0007", journeyDate);

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0007/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNRCAN0007"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refund").value(810.0))
                .andExpect(jsonPath("$.refundPercent").value(90.0))
                .andExpect(jsonPath("$.refundRef", startsWith("RFD-")));

        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseThrow();
        assertEquals("REFUND_INITIATED", payment.getStatus());
        assertEquals(new BigDecimal("810.00"), payment.getRefundAmount());
        assertEquals(new BigDecimal("810.00"),
                auditLogRepository.findByPnr("PNRCAN0007").get(0).getRefundAmount());
    }

    @Test
    @DisplayName("TC-CAN-02: Refund preview for a cancelled booking returns 400")
    void testPreviewRefund_AlreadyCancelled() throws Exception {
        saveConfirmedBooking("PNRCAN0008", journeyDate);

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0008/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bookings/PNRCAN0008/refund-preview")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RRS-SR-004: Refund preview without a token returns 401 Unauthorized")
    void testPreviewRefund_Unauthenticated() throws Exception {
        saveConfirmedBooking("PNRCAN0009", journeyDate);

        mockMvc.perform(get("/api/v1/bookings/PNRCAN0009/refund-preview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RRS-F-012-EXT: POST /api/v1/bookings/{pnr}/cancel-passengers/preview calculates tiered refund preview")
    void testPreviewPartialRefund_Success() throws Exception {
        Booking booking = saveConfirmedBooking("PNRCAN0010", journeyDate);
        Long passengerId = booking.getPassengers().get(0).getId();

        String reqJson = "{\"passengerIds\": [" + passengerId + "]}";

        mockMvc.perform(post("/api/v1/bookings/PNRCAN0010/cancel-passengers/preview")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType("application/json")
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNRCAN0010"))
                .andExpect(jsonPath("$.grossFare").value(450.0))
                .andExpect(jsonPath("$.refundPercent").value(90.0))
                .andExpect(jsonPath("$.deductionPercent").value(10.0))
                .andExpect(jsonPath("$.refundAmount").value(405.0))
                .andExpect(jsonPath("$.deductionAmount").value(45.0));
    }

    @Test
    @DisplayName("RRS-F-012-EXT: POST /api/v1/bookings/{pnr}/cancel-passengers selectively cancels passenger and immediately releases seat")
    void testCancelPassengers_PartialAndImmediateSeatRelease() throws Exception {
        Booking booking = saveConfirmedBooking("PNRCAN0011", journeyDate);
        Long firstPassengerId = booking.getPassengers().get(0).getId();
        Long secondPassengerId = booking.getPassengers().get(1).getId();

        assertEquals(98, availableCcSeats());

        // Cancel first passenger only
        String reqJson1 = "{\"passengerIds\": [" + firstPassengerId + "]}";
        mockMvc.perform(post("/api/v1/bookings/PNRCAN0011/cancel-passengers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType("application/json")
                        .content(reqJson1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNRCAN0011"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.remainingActivePassengers").value(1))
                .andExpect(jsonPath("$.refundAmount").value(405.0))
                .andExpect(jsonPath("$.refundRef", startsWith("RFD-")));

        // Seat is immediately released back into inventory: 98 -> 99
        assertEquals(99, availableCcSeats());

        // Cancel remaining passenger
        String reqJson2 = "{\"passengerIds\": [" + secondPassengerId + "]}";
        mockMvc.perform(post("/api/v1/bookings/PNRCAN0011/cancel-passengers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType("application/json")
                        .content(reqJson2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.remainingActivePassengers").value(0));

        // Both seats released back into inventory: 99 -> 100
        assertEquals(100, availableCcSeats());
    }
}
