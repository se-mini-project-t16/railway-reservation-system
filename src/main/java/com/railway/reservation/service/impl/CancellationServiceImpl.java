package com.railway.reservation.service.impl;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.dto.PartialCancellationResponseDto;
import com.railway.reservation.dto.PartialRefundPreviewDto;
import com.railway.reservation.dto.RefundPreviewDto;
import com.railway.reservation.exception.CancellationNotAllowedException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.CancellationAuditLog;
import com.railway.reservation.model.CancellationRule;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.TrainClass;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    @Transactional(readOnly = true)
    public PartialRefundPreviewDto previewPartialRefund(String pnr, List<Long> passengerIds, Long userId) {
        if (passengerIds == null || passengerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger ID must be selected for cancellation.");
        }
        LocalDateTime now = LocalDateTime.now(clock);

        Booking booking = findOwnedBooking(pnr, userId, bookingRepository::findByPnr);
        requireCancellable(booking, now);

        List<Passenger> selectedPassengers = resolvePassengers(booking, passengerIds);

        BigDecimal grossFare = BigDecimal.ZERO;
        List<String> passengerNames = new ArrayList<>();
        for (Passenger p : selectedPassengers) {
            grossFare = grossFare.add(calculatePassengerFare(booking, p));
            passengerNames.add(p.getName());
        }
        grossFare = grossFare.setScale(2, RoundingMode.HALF_UP);

        long hoursLeft = Duration.between(now, departureOf(booking)).toHours();
        BigDecimal refundPercent = lookupRefundPercent(hoursLeft);
        BigDecimal deductionPercent = HUNDRED.subtract(refundPercent);
        BigDecimal refundAmount = grossFare.multiply(refundPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal deductionAmount = grossFare.subtract(refundAmount);

        return new PartialRefundPreviewDto(
                booking.getPnr(),
                passengerIds,
                passengerNames,
                grossFare,
                hoursLeft,
                refundPercent,
                deductionPercent,
                refundAmount,
                deductionAmount
        );
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

        // 4. Mark all passengers as CANCELLED and exclude booking from availability (RRS-F-005)
        if (booking.getPassengers() != null) {
            for (Passenger p : booking.getPassengers()) {
                p.setStatus("CANCELLED");
            }
        }
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

    @Override
    @Transactional
    public PartialCancellationResponseDto cancelPassengers(String pnr, List<Long> passengerIds, Long userId) {
        if (passengerIds == null || passengerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger ID must be selected for cancellation.");
        }
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. Lock the booking row
        Booking booking = findOwnedBooking(pnr, userId, bookingRepository::findByPnrForUpdate);

        // 2. Only confirmed bookings prior to departure can be cancelled
        requireCancellable(booking, now);

        // 3. Resolve selected passengers
        List<Passenger> selectedPassengers = resolvePassengers(booking, passengerIds);

        BigDecimal grossFare = BigDecimal.ZERO;
        List<String> passengerNames = new ArrayList<>();
        for (Passenger p : selectedPassengers) {
            grossFare = grossFare.add(calculatePassengerFare(booking, p));
            passengerNames.add(p.getName());
            p.setStatus("CANCELLED");
        }
        grossFare = grossFare.setScale(2, RoundingMode.HALF_UP);

        long hoursLeft = Duration.between(now, departureOf(booking)).toHours();
        BigDecimal refundPercent = lookupRefundPercent(hoursLeft);
        BigDecimal refundAmount = grossFare.multiply(refundPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);

        long remainingActive = booking.getPassengers().stream()
                .filter(p -> !"CANCELLED".equalsIgnoreCase(p.getStatus()))
                .count();

        if (remainingActive == 0) {
            booking.setStatus(BookingStatus.CANCELLED);
        } else {
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        bookingRepository.save(booking);

        String refundRef = paymentService.initiateRefund(booking, refundAmount);

        auditLogRepository.save(new CancellationAuditLog(
                booking.getId(), booking.getPnr(), userId, refundAmount, now));
        logger.info("AUDIT: Partial cancellation processed. pnr={}, userId={}, cancelledPassengers={}, seatsReleased={}, remainingActive={}, refund={}, refundRef={} [RRS-F-012-EXT, TC-CAN-01]",
                booking.getPnr(), userId, passengerNames, selectedPassengers.size(), remainingActive, refundAmount, refundRef);

        return new PartialCancellationResponseDto(
                booking.getPnr(),
                booking.getStatus(),
                passengerIds,
                passengerNames,
                (int) remainingActive,
                refundAmount,
                refundPercent,
                refundRef,
                now,
                "Passenger(s) cancelled successfully. " + selectedPassengers.size() + " seat(s) released back to inventory."
        );
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

    private BigDecimal lookupRefundPercent(long hoursLeft) {
        return cancellationRuleRepository.findAllByOrderByHoursBeforeDesc().stream()
                .filter(rule -> rule.getHoursBefore() <= hoursLeft)
                .map(CancellationRule::getRefundPercent)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private List<Passenger> resolvePassengers(Booking booking, List<Long> passengerIds) {
        List<Passenger> passengers = booking.getPassengers();
        if (passengers == null || passengers.isEmpty()) {
            throw new ResourceNotFoundException("No passengers found for booking.");
        }

        List<Passenger> result = new ArrayList<>();
        for (Long pid : passengerIds) {
            Passenger found = passengers.stream()
                    .filter(p -> Objects.equals(p.getId(), pid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Passenger ID " + pid + " does not belong to booking " + booking.getPnr()));

            if ("CANCELLED".equalsIgnoreCase(found.getStatus())) {
                throw new CancellationNotAllowedException("Passenger " + found.getName() + " is already cancelled.");
            }
            result.add(found);
        }
        return result;
    }

    private BigDecimal calculatePassengerFare(Booking booking, Passenger passenger) {
        TrainClass tc = booking.getTrainClass();
        int totalPassengers = (booking.getPassengers() != null && !booking.getPassengers().isEmpty())
                ? booking.getPassengers().size()
                : 1;

        BigDecimal standardSeatFare = tc != null && tc.getFare() != null
                ? tc.getFare()
                : booking.getTotalFare().divide(BigDecimal.valueOf(totalPassengers), 2, RoundingMode.HALF_UP);

        if (Boolean.TRUE.equals(passenger.getSeniorConcession()) && passenger.getAge() >= 60) {
            String className = tc != null && tc.getClassName() != null ? tc.getClassName().toUpperCase() : "";
            boolean isAc = className.contains("AC") || className.contains("1A") || className.contains("2A")
                    || className.contains("3A") || className.contains("CC") || className.contains("EC")
                    || className.contains("EXECUTIVE");
            BigDecimal resFee = isAc ? BigDecimal.valueOf(40.00) : BigDecimal.valueOf(20.00);
            BigDecimal sfCharge = isAc ? BigDecimal.valueOf(30.00) : BigDecimal.valueOf(15.00);
            BigDecimal baseFare = standardSeatFare.compareTo(resFee.add(sfCharge)) > 0
                    ? standardSeatFare.subtract(resFee).subtract(sfCharge)
                    : standardSeatFare;
            BigDecimal discount = baseFare.multiply(BigDecimal.valueOf(0.40)).setScale(2, RoundingMode.HALF_UP);
            return standardSeatFare.subtract(discount);
        }
        return standardSeatFare;
    }

    private LocalDateTime departureOf(Booking booking) {
        return LocalDateTime.of(booking.getJourneyDate(), booking.getTrain().getDepartureTime());
    }

    private record RefundQuote(long hoursBeforeDeparture, BigDecimal refundPercent, BigDecimal refundAmount) {}
}
