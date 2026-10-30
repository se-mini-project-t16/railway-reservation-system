package com.railway.reservation.service.impl;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.dto.RefundPreviewDto;
import com.railway.reservation.exception.CancellationNotAllowedException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.CancellationAuditLog;
import com.railway.reservation.model.CancellationRule;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.CancellationAuditLogRepository;
import com.railway.reservation.repository.CancellationRuleRepository;
import com.railway.reservation.service.CancellationService;
import com.railway.reservation.service.PaymentService;
import com.railway.reservation.validation.SafeTextRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

@Service
public class CancellationServiceImpl implements CancellationService {

    private static final Logger logger = LoggerFactory.getLogger(CancellationServiceImpl.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BookingRepository bookingRepository;
    private final CancellationAuditLogRepository auditLogRepository;
    private final CancellationRuleRepository cancellationRuleRepository;
    private final PaymentService paymentService;

    // Replaced with a fixed clock in tests so departure checks are deterministic
    private Clock clock = Clock.systemDefaultZone();

    public CancellationServiceImpl(
            BookingRepository bookingRepository,
            CancellationAuditLogRepository auditLogRepository,
            CancellationRuleRepository cancellationRuleRepository,
            PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
        this.cancellationRuleRepository = cancellationRuleRepository;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional(readOnly = true)
    public RefundPreviewDto previewRefund(String pnr, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);

        Booking booking = findOwnedBooking(pnr, userId, bookingRepository::findByPnr);
        requireCancellable(booking, now);

        RefundQuote quote = quoteRefund(booking, now);
        return new RefundPreviewDto(booking.getPnr(), booking.getTotalFare(),
                quote.hoursBeforeDeparture(), quote.refundPercent(), quote.refundAmount());
    }

    @Override
    @Transactional
    public CancellationResponseDto cancelBooking(String pnr, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. Lock the booking row so two concurrent cancels cannot both succeed
        Booking booking = findOwnedBooking(pnr, userId, bookingRepository::findByPnrForUpdate);

        // 2. Only a confirmed booking whose train has not yet departed can be cancelled
        requireCancellable(booking, now);

        // 3. Refund from the cancellation_rules table, using the same quote as the preview (RRS-F-013)
        RefundQuote quote = quoteRefund(booking, now);

        // 4. CANCELLED bookings are excluded from availability, which releases the seats (RRS-F-005)
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 5. Initiate the refund with the payment gateway
        String refundRef = paymentService.initiateRefund(booking, quote.refundAmount());

        // 6. Audit trail with user id and timestamp (RRS-F-012)
        auditLogRepository.save(new CancellationAuditLog(
                booking.getId(), booking.getPnr(), userId, quote.refundAmount(), now));
        logger.info("AUDIT: Ticket CANCELLED. pnr={}, userId={}, cancelledAt={}, seatsReleased={}, refund={}, refundRef={} [RRS-F-012, RRS-F-013, TC-CAN-01]",
                booking.getPnr(), userId, now, booking.getPassengers().size(), quote.refundAmount(), refundRef);

        return new CancellationResponseDto(booking.getPnr(), BookingStatus.CANCELLED,
                quote.refundAmount(), quote.refundPercent(), refundRef, now);
    }

    private Booking findOwnedBooking(String pnr, Long userId, Function<String, Optional<Booking>> lookup) {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new ResourceNotFoundException("Booking not found.");
        }
        SafeTextRules.requireSafe("PNR", pnr);

        // Another user's booking is reported as not found so PNRs cannot be probed
        return lookup.apply(pnr.trim().toUpperCase())
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

    /**
     * Picks the rule with the largest hours_before that the time left still satisfies.
     * With no matching rule the refund is zero (RRS-F-013, TC-CAN-02).
     */
    private RefundQuote quoteRefund(Booking booking, LocalDateTime now) {
        long hoursLeft = Duration.between(now, departureOf(booking)).toHours();

        BigDecimal percent = cancellationRuleRepository.findAllByOrderByHoursBeforeDesc().stream()
                .filter(rule -> rule.getHoursBefore() <= hoursLeft)
                .map(CancellationRule::getRefundPercent)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal amount = booking.getTotalFare()
                .multiply(percent)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        return new RefundQuote(hoursLeft, percent, amount);
    }

    private LocalDateTime departureOf(Booking booking) {
        return LocalDateTime.of(booking.getJourneyDate(), booking.getTrain().getDepartureTime());
    }

    private record RefundQuote(long hoursBeforeDeparture, BigDecimal refundPercent, BigDecimal refundAmount) {}
}
