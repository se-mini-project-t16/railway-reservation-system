package com.railway.reservation.service;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.exception.CancellationNotAllowedException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.CancellationAuditLogRepository;
import com.railway.reservation.service.impl.CancellationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        booking.addPassenger(new Passenger("Alice Smith", 28, "Female", "AC3-1"));
        booking.addPassenger(new Passenger("Bob Smith", 32, "Male", "AC3-2"));
    }

    private void fixNow(LocalDateTime now) {
        ReflectionTestUtils.setField(cancellationService, "clock",
                Clock.fixed(now.atZone(ZONE).toInstant(), ZONE));
    }

    @Test
    @DisplayName("TC-CAN-01: Cancelling a confirmed booking sets CANCELLED and writes an audit entry")
    void testCancelBooking_Success() {
        LocalDateTime now = DEPARTURE.minusDays(2);
        fixNow(now);
        when(bookingRepository.findByPnrForUpdate("PNRCAN0001")).thenReturn(Optional.of(booking));

        CancellationResponseDto response = cancellationService.cancelBooking(" pnrcan0001 ", 1L);

        assertEquals("PNRCAN0001", response.getPnr());
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        assertEquals(now, response.getCancelledAt());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);

        ArgumentCaptor<CancellationAuditLog> audit = ArgumentCaptor.forClass(CancellationAuditLog.class);
        verify(auditLogRepository).save(audit.capture());
        assertEquals(50L, audit.getValue().getBookingId());
        assertEquals("PNRCAN0001", audit.getValue().getPnr());
        assertEquals(1L, audit.getValue().getUserId());
        assertEquals(now, audit.getValue().getCancelledAt());
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
}
