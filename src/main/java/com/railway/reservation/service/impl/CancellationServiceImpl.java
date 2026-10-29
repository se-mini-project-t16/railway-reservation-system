package com.railway.reservation.service.impl;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.exception.CancellationNotAllowedException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.CancellationAuditLog;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.CancellationAuditLogRepository;
import com.railway.reservation.service.CancellationService;
import com.railway.reservation.validation.SafeTextRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CancellationServiceImpl implements CancellationService {

    private static final Logger logger = LoggerFactory.getLogger(CancellationServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final CancellationAuditLogRepository auditLogRepository;

    // Replaced with a fixed clock in tests so departure checks are deterministic
    private Clock clock = Clock.systemDefaultZone();

    public CancellationServiceImpl(
            BookingRepository bookingRepository,
            CancellationAuditLogRepository auditLogRepository) {
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public CancellationResponseDto cancelBooking(String pnr, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. Lock the booking row so two concurrent cancels cannot both succeed
        Booking booking = findOwnedBookingForUpdate(pnr, userId);

        // 2. Only a confirmed booking whose train has not yet departed can be cancelled
        requireCancellable(booking, now);

        // 3. CANCELLED bookings are excluded from availability, which releases the seats (RRS-F-005)
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 4. Audit trail with user id and timestamp (RRS-F-012)
        auditLogRepository.save(new CancellationAuditLog(
                booking.getId(), booking.getPnr(), userId, null, now));
        logger.info("AUDIT: Ticket CANCELLED. pnr={}, userId={}, cancelledAt={}, seatsReleased={} [RRS-F-012, TC-CAN-01]",
                booking.getPnr(), userId, now, booking.getPassengers().size());

        return new CancellationResponseDto(booking.getPnr(), BookingStatus.CANCELLED, now);
    }

    private Booking findOwnedBookingForUpdate(String pnr, Long userId) {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new ResourceNotFoundException("Booking not found.");
        }
        SafeTextRules.requireSafe("PNR", pnr);

        // Another user's booking is reported as not found so PNRs cannot be probed
        return bookingRepository.findByPnrForUpdate(pnr.trim().toUpperCase())
                .filter(b -> b.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
    }

    private void requireCancellable(Booking booking, LocalDateTime now) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new CancellationNotAllowedException("Booking is already cancelled.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new CancellationNotAllowedException("Only confirmed bookings can be cancelled.");
        }
        if (!now.isBefore(departureOf(booking))) {
            throw new CancellationNotAllowedException("Booking cannot be cancelled after the train has departed.");
        }
    }

    private LocalDateTime departureOf(Booking booking) {
        return LocalDateTime.of(booking.getJourneyDate(), booking.getTrain().getDepartureTime());
    }
}
