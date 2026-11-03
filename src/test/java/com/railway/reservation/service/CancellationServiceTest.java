package com.railway.reservation.service;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.dto.PartialCancellationResponseDto;
import com.railway.reservation.dto.PartialRefundPreviewDto;
import com.railway.reservation.dto.RefundPreviewDto;
import com.railway.reservation.exception.CancellationNotAllowedException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.CancellationAuditLogRepository;
import com.railway.reservation.repository.CancellationRuleRepository;
import com.railway.reservation.service.impl.CancellationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancellationServiceTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    // Train departs 2026-11-05 20:15
    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 11, 5, 20, 15);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CancellationAuditLogRepository auditLogRepository;

    @Mock
    private CancellationRuleRepository cancellationRuleRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private CancellationServiceImpl cancellationService;

    private User owner;
    private Booking booking;

    @BeforeEach
    void setUp() {
        owner = new User("Likhith S", "likhith@example.com", "9876543210", "hashedPassword", Role.ROLE_PASSENGER);
        owner.setId(1L);

        Train train = new Train("12601", "Mangalore Mail", "Chennai Central", "Mangalore Central",
                DEPARTURE.toLocalTime(), LocalTime.of(12, 5));
        train.setId(10L);

        TrainClass trainClass = new TrainClass("AC 3 Tier", 64, BigDecimal.valueOf(1150.00));
        trainClass.setId(100L);

        booking = new Booking("PNRCAN0001", owner, train, trainClass,
                DEPARTURE.toLocalDate(), BigDecimal.valueOf(2300.00));
        booking.setId(50L);
        booking.setStatus(BookingStatus.CONFIRMED);
        Passenger p1 = new Passenger("Alice Smith", 28, "Female", "AC3-1");
        p1.setId(101L);
        booking.addPassenger(p1);
        Passenger p2 = new Passenger("Bob Smith", 32, "Male", "AC3-2");
        p2.setId(102L);
        booking.addPassenger(p2);
    }

    // Same tiers as CancellationRuleInitializer, ordered as the repository returns them
    private static final List<CancellationRule> DEFAULT_RULES = List.of(
            new CancellationRule(48, new BigDecimal("90.00")),
            new CancellationRule(24, new BigDecimal("75.00")),
            new CancellationRule(12, new BigDecimal("50.00")),
            new CancellationRule(4, new BigDecimal("25.00")),
            new CancellationRule(0, new BigDecimal("0.00")));

    private void fixNow(LocalDateTime now) {
        ReflectionTestUtils.setField(cancellationService, "clock",
                Clock.fixed(now.atZone(ZONE).toInstant(), ZONE));
    }

    @Test
    @DisplayName("TC-CAN-01: Cancelling a confirmed booking sets CANCELLED, initiates the refund and writes an audit entry")
    void testCancelBooking_Success() {
        LocalDateTime now = DEPARTURE.minusDays(2);
        fixNow(now);
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(DEFAULT_RULES);
        when(paymentService.initiateRefund(eq(booking), any(BigDecimal.class))).thenReturn("RFD-TEST00000001");

        CancellationResponseDto response = cancellationService.cancelBooking(" pnrcan0001 ", 1L);

        assertEquals("PNRCAN0001", response.getPnr());
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        assertEquals(now, response.getCancelledAt());
        assertEquals(new BigDecimal("2070.00"), response.getRefund());
        assertEquals(new BigDecimal("90.00"), response.getRefundPercent());
        assertEquals("RFD-TEST00000001", response.getRefundRef());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
        verify(paymentService).initiateRefund(booking, new BigDecimal("2070.00"));

        ArgumentCaptor<CancellationAuditLog> audit = ArgumentCaptor.forClass(CancellationAuditLog.class);
        verify(auditLogRepository).save(audit.capture());
        assertEquals(50L, audit.getValue().getBookingId());
        assertEquals("PNRCAN0001", audit.getValue().getPnr());
        assertEquals(1L, audit.getValue().getUserId());
        assertEquals(now, audit.getValue().getCancelledAt());
        assertEquals(new BigDecimal("2070.00"), audit.getValue().getRefundAmount());
    }

    @Test
    @DisplayName("TC-CAN-01: Unknown PNR returns not found")
    void testCancelBooking_UnknownPnr() {
        when(bookingRepository.findByPnrForUpdate("NOSUCHPNR1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cancellationService.cancelBooking("NOSUCHPNR1", 1L));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("TC-CAN-01: Another user's booking is reported as not found and left unchanged")
    void testCancelBooking_OtherUsersBooking() {
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));

        assertThrows(ResourceNotFoundException.class,
                () -> cancellationService.cancelBooking("PNRCAN0001", 2L));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("TC-CAN-01: Already cancelled booking is rejected")
    void testCancelBooking_AlreadyCancelled() {
        fixNow(DEPARTURE.minusDays(2));
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));

        CancellationNotAllowedException ex = assertThrows(CancellationNotAllowedException.class,
                () -> cancellationService.cancelBooking("PNRCAN0001", 1L));
        assertTrue(ex.getMessage().contains("already cancelled"));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("TC-CAN-01: Booking awaiting payment cannot be cancelled")
    void testCancelBooking_PendingPayment() {
        fixNow(DEPARTURE.minusDays(2));
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));

        assertThrows(CancellationNotAllowedException.class,
                () -> cancellationService.cancelBooking("PNRCAN0001", 1L));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    @DisplayName("TC-CAN-01: Booking cannot be cancelled at or after departure")
    void testCancelBooking_AfterDeparture() {
        fixNow(DEPARTURE);
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));

        assertThrows(CancellationNotAllowedException.class,
                () -> cancellationService.cancelBooking("PNRCAN0001", 1L));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("RRS-SR-003: Unsafe PNR input is rejected before any lookup")
    void testCancelBooking_UnsafePnr() {
        assertThrows(IllegalArgumentException.class,
                () -> cancellationService.cancelBooking("<script>alert(1)</script>", 1L));
        verifyNoInteractions(bookingRepository);
    }

    @ParameterizedTest(name = "{0} min before departure -> {1}% refund = {2}")
    @CsvSource({
            "4320, 90.00, 2070.00",   // 72h
            "2880, 90.00, 2070.00",   // exactly 48h
            "2879, 75.00, 1725.00",   // 47h59m
            "780,  50.00, 1150.00",   // 13h
            "300,  25.00, 575.00",    // 5h
            "120,  0.00,  0.00"       // 2h
    })
    @DisplayName("TC-CAN-02: Refund preview applies the cancellation_rules tier for the time left")
    void testPreviewRefund_RuleTiers(long minutesBefore, BigDecimal expectedPercent, BigDecimal expectedAmount) {
        fixNow(DEPARTURE.minusMinutes(minutesBefore));
        when(bookingRepository.findByPnr("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(DEFAULT_RULES);

        RefundPreviewDto preview = cancellationService.previewRefund("PNRCAN0001", 1L);

        assertEquals("PNRCAN0001", preview.getPnr());
        assertEquals(minutesBefore / 60, preview.getHoursBeforeDeparture());
        assertEquals(0, expectedPercent.compareTo(preview.getRefundPercent()));
        assertEquals(0, expectedAmount.compareTo(preview.getRefundAmount()));
    }

    @Test
    @DisplayName("TC-CAN-02: Refund preview leaves the booking and payment untouched")
    void testPreviewRefund_DoesNotChangeBooking() {
        fixNow(DEPARTURE.minusDays(3));
        when(bookingRepository.findByPnr("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(DEFAULT_RULES);

        cancellationService.previewRefund("PNRCAN0001", 1L);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(paymentService, auditLogRepository);
    }

    @Test
    @DisplayName("TC-CAN-02: Refund preview is rejected for an already cancelled booking")
    void testPreviewRefund_AlreadyCancelled() {
        fixNow(DEPARTURE.minusDays(3));
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByPnr("PNRCAN0001")).thenReturn(Optional.of(booking));

        assertThrows(CancellationNotAllowedException.class,
                () -> cancellationService.previewRefund("PNRCAN0001", 1L));
    }

    @Test
    @DisplayName("TC-CAN-02: With no cancellation rules configured the refund is zero")
    void testPreviewRefund_NoRules() {
        fixNow(DEPARTURE.minusDays(3));
        when(bookingRepository.findByPnr("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(List.of());

        RefundPreviewDto preview = cancellationService.previewRefund("PNRCAN0001", 1L);

        assertEquals(0, BigDecimal.ZERO.compareTo(preview.getRefundPercent()));
        assertEquals(new BigDecimal("0.00"), preview.getRefundAmount());
    }

    @Test
    @DisplayName("TC-CAN-02: Refund amount is rounded half-up to two decimals")
    void testPreviewRefund_RoundsHalfUp() {
        fixNow(DEPARTURE.minusHours(5));
        booking.setTotalFare(new BigDecimal("1150.55"));
        when(bookingRepository.findByPnr("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(DEFAULT_RULES);

        // 25% of 1150.55 = 287.6375
        assertEquals(new BigDecimal("287.64"),
                cancellationService.previewRefund("PNRCAN0001", 1L).getRefundAmount());
    }

    @Test
    @DisplayName("TC-CAN-02: Cancelling inside the no-refund window still cancels with a zero refund")
    void testCancelBooking_ZeroRefund() {
        fixNow(DEPARTURE.minusHours(2));
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(DEFAULT_RULES);
        when(paymentService.initiateRefund(eq(booking), any(BigDecimal.class))).thenReturn(null);

        CancellationResponseDto response = cancellationService.cancelBooking("PNRCAN0001", 1L);

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRefund()));
        assertNull(response.getRefundRef());
    }

    @Test
    @DisplayName("RRS-F-012-EXT: previewPartialRefund calculates tiered deduction for chosen passenger")
    void testPreviewPartialRefund_Success() {
        fixNow(DEPARTURE.minusDays(2)); // 48h before -> 90%
        when(bookingRepository.findByPnr("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(DEFAULT_RULES);

        PartialRefundPreviewDto preview = cancellationService.previewPartialRefund("PNRCAN0001", List.of(101L), 1L);

        assertNotNull(preview);
        assertEquals("PNRCAN0001", preview.getPnr());
        assertEquals(List.of(101L), preview.getSelectedPassengerIds());
        assertEquals(List.of("Alice Smith"), preview.getSelectedPassengerNames());
        assertEquals(new BigDecimal("1150.00"), preview.getGrossFare());
        assertEquals(new BigDecimal("90.00"), preview.getRefundPercent());
        assertEquals(new BigDecimal("10.00"), preview.getDeductionPercent());
        assertEquals(new BigDecimal("1035.00"), preview.getRefundAmount());
        assertEquals(new BigDecimal("115.00"), preview.getDeductionAmount());
    }

    @Test
    @DisplayName("RRS-F-012-EXT: cancelPassengers cancels chosen passenger and keeps remaining confirmed")
    void testCancelPassengers_Partial_RemainingConfirmed() {
        fixNow(DEPARTURE.minusDays(2)); // 48h before -> 90%
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(DEFAULT_RULES);
        when(paymentService.initiateRefund(eq(booking), any(BigDecimal.class))).thenReturn("RFD-PARTIAL1");

        PartialCancellationResponseDto response = cancellationService.cancelPassengers("PNRCAN0001", List.of(101L), 1L);

        assertNotNull(response);
        assertEquals("PNRCAN0001", response.getPnr());
        assertEquals(BookingStatus.CONFIRMED, response.getBookingStatus());
        assertEquals(1, response.getRemainingActivePassengers());
        assertEquals(new BigDecimal("1035.00"), response.getRefundAmount());
        assertEquals("RFD-PARTIAL1", response.getRefundRef());

        assertEquals("CANCELLED", booking.getPassengers().get(0).getStatus());
        assertEquals("CONFIRMED", booking.getPassengers().get(1).getStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        verify(auditLogRepository).save(any(CancellationAuditLog.class));
        verify(paymentService).initiateRefund(booking, new BigDecimal("1035.00"));
    }

    @Test
    @DisplayName("RRS-F-012-EXT: cancelPassengers cancels all passengers and transitions booking to CANCELLED")
    void testCancelPassengers_All_TransitionsToCancelled() {
        fixNow(DEPARTURE.minusDays(2));
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));
        when(cancellationRuleRepository.findAllByOrderByHoursBeforeDesc()).thenReturn(DEFAULT_RULES);
        when(paymentService.initiateRefund(eq(booking), any(BigDecimal.class))).thenReturn("RFD-ALL1");

        PartialCancellationResponseDto response = cancellationService.cancelPassengers("PNRCAN0001", List.of(101L, 102L), 1L);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.getBookingStatus());
        assertEquals(0, response.getRemainingActivePassengers());
        assertEquals(new BigDecimal("2070.00"), response.getRefundAmount());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    @DisplayName("RRS-F-012-EXT: cancelPassengers rejects already cancelled passenger")
    void testCancelPassengers_AlreadyCancelled_ThrowsException() {
        fixNow(DEPARTURE.minusDays(2));
        booking.getPassengers().get(0).setStatus("CANCELLED");
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));

        assertThrows(CancellationNotAllowedException.class, () ->
                cancellationService.cancelPassengers("PNRCAN0001", List.of(101L), 1L));
    }

    @Test
    @DisplayName("RRS-F-012-EXT: cancelPassengers rejects passenger ID not belonging to booking")
    void testCancelPassengers_UnknownPassengerId_ThrowsException() {
        fixNow(DEPARTURE.minusDays(2));
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () ->
                cancellationService.cancelPassengers("PNRCAN0001", List.of(999L), 1L));
    }
}
