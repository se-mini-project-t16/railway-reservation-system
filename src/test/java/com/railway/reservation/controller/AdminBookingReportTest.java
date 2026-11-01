package com.railway.reservation.controller;

import com.railway.reservation.model.*;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class AdminBookingReportTest {

    private static final String REPORT_URL = "/api/v1/admin/bookings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String passengerToken;
    private LocalDate day1;
    private LocalDate day2;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        trainRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(new User("Admin User", "admin@railway.com", "9999999999",
                passwordEncoder.encode("AdminPass123!"), Role.ROLE_ADMIN));
        User passenger = userRepository.save(new User("Passenger User", "passenger@railway.com", "8888888888",
                passwordEncoder.encode("Pass1234!"), Role.ROLE_PASSENGER));
        adminToken = jwtService.generateToken(admin);
        passengerToken = jwtService.generateToken(passenger);

        Train kovai = new Train("12675", "Kovai Express", "Chennai", "Bengaluru",
                LocalTime.of(6, 10), LocalTime.of(11, 30));
        kovai.addClass(new TrainClass("CC", 100, new BigDecimal("450.00")));
        kovai = trainRepository.save(kovai);
        Train shatabdi = new Train("12007", "Shatabdi Express", "Chennai", "Mysuru",
                LocalTime.of(6, 0), LocalTime.of(13, 0));
        shatabdi.addClass(new TrainClass("CC", 100, new BigDecimal("800.00")));
        shatabdi = trainRepository.save(shatabdi);

        day1 = LocalDate.now().plusDays(5);
        day2 = LocalDate.now().plusDays(6);

        // Matching filter (12675 on day1): confirmed, failed payment, cancelled with refund
        Booking confirmed = booking("PNRCONF001", passenger, kovai, day1, "900.00", BookingStatus.CONFIRMED);
        confirmed.addPassenger(new Passenger("Alice Smith", 28, "Female", "CC-1"));
        confirmed.addPassenger(new Passenger("Bob O'Brien", 32, "Male", "CC-2"));
        confirmed.setPayment(new Payment(confirmed, new BigDecimal("900.00"), "TXN-RPT-1", "SUCCESS"));

        Booking failed = booking("PNRFAIL002", passenger, kovai, day1, "450.00", BookingStatus.FAILED);
        failed.addPassenger(new Passenger("Carol Diaz", 40, "Female", null));
        failed.setPayment(new Payment(failed, new BigDecimal("450.00"), "TXN-RPT-2", "FAILED"));

        Booking cancelled = booking("PNRCANC003", passenger, kovai, day1, "450.00", BookingStatus.CANCELLED);
        cancelled.addPassenger(new Passenger("Dan Lee", 51, "Male", "CC-3"));
        Payment refunded = new Payment(cancelled, new BigDecimal("450.00"), "TXN-RPT-3", "REFUND_INITIATED");
        refunded.setRefundAmount(new BigDecimal("337.50"));
        cancelled.setPayment(refunded);

        // Non-matching: same train other day, other train same day
        booking("PNROTHR004", passenger, kovai, day2, "450.00", BookingStatus.CONFIRMED)
                .addPassenger(new Passenger("Eve Park", 22, "Female", "CC-4"));
        booking("PNROTHR005", passenger, shatabdi, day1, "800.00", BookingStatus.CONFIRMED)
                .addPassenger(new Passenger("Frank Moss", 35, "Male", "CC-1"));

        bookingRepository.flush();
    }

    private Booking booking(String pnr, User user, Train train, LocalDate date, String fare, BookingStatus status) {
        Booking b = new Booking(pnr, user, train, train.getClasses().get(0), date, new BigDecimal(fare));
        b.setStatus(status);
        return bookingRepository.save(b);
    }

    @Test
    @DisplayName("TC-ADM-02: Filtered report by train and date returns matching rows and summary metrics")
    void filteredReportReturnsMatchingRowsAndMetrics() throws Exception {
        mockMvc.perform(get(REPORT_URL)
                        .param("trainNo", "12675")
                        .param("date", day1.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainNo").value("12675"))
                .andExpect(jsonPath("$.date").value(day1.toString()))
                .andExpect(jsonPath("$.totalBookings").value(3))
                .andExpect(jsonPath("$.failedPayments").value(1))
                .andExpect(jsonPath("$.cancellations").value(1))
                .andExpect(jsonPath("$.totalRefunds").value(337.50))
                .andExpect(jsonPath("$.bookings", hasSize(3)))
                .andExpect(jsonPath("$.bookings[*].pnr", containsInAnyOrder("PNRCONF001", "PNRFAIL002", "PNRCANC003")))
                .andExpect(jsonPath("$.bookings[?(@.pnr == 'PNRCONF001')].status").value(hasItem("CONFIRMED")))
                .andExpect(jsonPath("$.bookings[?(@.pnr == 'PNRCONF001')].fare").value(hasItem(900.00)))
                .andExpect(jsonPath("$.bookings[?(@.pnr == 'PNRCONF001')].passengers[*].name",
                        containsInAnyOrder("Alice Smith", "Bob O'Brien")))
                .andExpect(jsonPath("$.bookings[?(@.pnr == 'PNRCONF001')].passengers[*].seatNo",
                        containsInAnyOrder("CC-1", "CC-2")));
    }

    @Test
    @DisplayName("TC-ADM-02: Filters are optional (none, train only, date only)")
    void filtersAreOptional() throws Exception {
        mockMvc.perform(get(REPORT_URL).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(5));

        mockMvc.perform(get(REPORT_URL).param("trainNo", "12675").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(4));

        mockMvc.perform(get(REPORT_URL).param("date", day1.toString()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(4));

        mockMvc.perform(get(REPORT_URL).param("trainNo", "99999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(0))
                .andExpect(jsonPath("$.bookings", hasSize(0)));
    }

    @Test
    @DisplayName("RRS-SR-004: Report requires ADMIN role (passenger 403, no token 401)")
    void reportRequiresAdminRole() throws Exception {
        mockMvc.perform(get(REPORT_URL).header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get(REPORT_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("RRS-SR-003: Unsafe trainNo and malformed date are rejected with 400 without echoing input")
    void unsafeFilterRejected() throws Exception {
        mockMvc.perform(get(REPORT_URL)
                        .param("trainNo", "' OR 1=1 --")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("OR 1=1"))));

        mockMvc.perform(get(REPORT_URL)
                        .param("trainNo", "<script>alert(1)</script>")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("<script>"))));

        mockMvc.perform(get(REPORT_URL)
                        .param("date", "30-10-2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }
}
