package com.railway.reservation.service.rail;

import com.railway.reservation.dto.BookingDtos.CancelPreview;
import com.railway.reservation.dto.BookingDtos.RefundLine;
import com.railway.reservation.dto.TrainDtos.*;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.CancellationAuditLogRepository;
import com.railway.reservation.repository.CancellationRuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Refunds and cancellation (RRS-F-012, RRS-F-013). Confirmed tickets follow
 * the rule table stored in the database; RAC and waitlisted travellers lose
 * only a clerkage charge; confirmed Tatkal tickets are not refunded.
 */
@Service
public class CancellationService {

    public static final int CLERKAGE = 60;
    public static final int QUEUED_CUTOFF_MINUTES = 30;

    private final CancellationRuleRepository ruleRepository;
    private final CancellationAuditLogRepository auditRepository;
    private final SeatOperations seatOperations;
    private final InventoryService inventoryService;

    public CancellationService(CancellationRuleRepository ruleRepository, CancellationAuditLogRepository auditRepository,
                               SeatOperations seatOperations, InventoryService inventoryService) {
        this.ruleRepository = ruleRepository;
        this.auditRepository = auditRepository;
        this.seatOperations = seatOperations;
        this.inventoryService = inventoryService;
    }

    public record Refund(int fare, int deduction, int refund, String rule) {}

    public boolean canCancel(Booking b, Passenger p, LocalDateTime now) {
        if (!b.getStatus().isLive() || p.getCurrentStatus() == SeatStatus.CAN) return false;
        long minutes = Duration.between(now, b.getDepartureAt()).toMinutes();
        return p.getCurrentStatus() == SeatStatus.CNF ? minutes > 0 : minutes >= QUEUED_CUTOFF_MINUTES;
    }

    public Refund refundFor(Booking b, Passenger p, LocalDateTime now) {
        int fare = p.getFare().intValue();
        long minutes = Duration.between(now, b.getDepartureAt()).toMinutes();
        if (p.getCurrentStatus() != SeatStatus.CNF) {
            int deduction = Math.min(CLERKAGE, fare);
            return new Refund(fare, deduction, fare - deduction,
                    "RAC or waitlisted: clerkage of Rs " + CLERKAGE + " per traveller");
        }
        if (b.getQuota() == Quota.TATKAL) {
            return new Refund(fare, fare, 0, "Confirmed Tatkal tickets are not refunded");
        }
        CoachClass cc = b.getCoachClass();
        for (CancellationRule rule : ruleRepository.findAllByOrderByMinHoursBeforeDesc()) {
            if (minutes < rule.getMinHoursBefore() * 60L) continue;
            int pct = BigDecimal.valueOf(fare).multiply(rule.getDeductionPercent())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();
            int flat = 0;
            if (rule.isFlatMinimum()) {
                flat = cc.cancellationFlatCharge();
                if (cc.airConditioned()) flat = (int) Math.round(flat * 1.05);
            }
            int deduction = Math.min(fare, Math.max(pct, flat));
            return new Refund(fare, deduction, fare - deduction, rule.getDescription());
        }
        return new Refund(fare, fare, 0, "Under 4 hours to departure: no refund");
    }

    public CancelPreview preview(Booking b, List<Long> passengerIds) {
        LocalDateTime now = inventoryService.now();
        List<Passenger> chosen = chosen(b, passengerIds, now);
        List<RefundLine> lines = new ArrayList<>();
        int fare = 0, deduction = 0, refund = 0;
        for (Passenger p : chosen) {
            Refund r = refundFor(b, p, now);
            lines.add(new RefundLine(p.getId(), p.getName(), p.getCurrentStatus().name(), r.fare(), r.deduction(),
                    r.refund(), r.rule()));
            fare += r.fare();
            deduction += r.deduction();
            refund += r.refund();
        }
        return new CancelPreview(b.getPnr(), lines, fare, deduction, refund,
                Math.max(0, Duration.between(now, b.getDepartureAt()).toMinutes()));
    }

    /** Cancels the chosen travellers of a locked booking, refunds them and promotes the queues. */
    public int cancel(Booking b, List<Long> passengerIds, Long actorId, String reason) {
        LocalDateTime now = inventoryService.now();
        List<Passenger> chosen = chosen(b, passengerIds, now);
        Map<Passenger, Refund> refunds = new LinkedHashMap<>();
        for (Passenger p : chosen) refunds.put(p, refundFor(b, p, now));
        return apply(b, refunds, actorId, reason, now);
    }

    /** Cancels a fully waitlisted ticket once the chart is prepared, as the railway does automatically. */
    public int cancelUnconfirmedAfterChart(Booking b) {
        LocalDateTime now = inventoryService.now();
        Map<Passenger, Refund> refunds = new LinkedHashMap<>();
        for (Passenger p : b.activePassengers()) {
            int fare = p.getFare().intValue();
            int deduction = Math.min(CLERKAGE, fare);
            refunds.put(p, new Refund(fare, deduction, fare - deduction, "Waitlisted after chart preparation"));
        }
        return apply(b, refunds, null, "AUTO_WAITLIST", now);
    }

    private int apply(Booking b, Map<Passenger, Refund> refunds, Long actorId, String reason, LocalDateTime now) {
        int total = 0;
        for (Map.Entry<Passenger, Refund> e : refunds.entrySet()) {
            Passenger p = e.getKey();
            p.setRefundAmount(BigDecimal.valueOf(e.getValue().refund()));
            p.setCancelledAt(now);
            total += e.getValue().refund();
        }
        seatOperations.release(b, refunds.keySet());

        b.setRefundTotal(b.getRefundTotal().add(BigDecimal.valueOf(total)));
        Payment paid = b.successfulPayment();
        if (paid != null) paid.setRefundedAmount(paid.getRefundedAmount().add(BigDecimal.valueOf(total)));
        if (b.activePassengers().isEmpty()) {
            b.setStatus(BookingStatus.CANCELLED);
            b.setClosedAt(now);
        } else {
            b.setStatus(BookingStatus.PARTIALLY_CANCELLED);
        }
        auditRepository.save(new CancellationAuditLog(b.getPnr(), actorId, reason, refunds.size(),
                BigDecimal.valueOf(total), now));
        return total;
    }

    private List<Passenger> chosen(Booking b, List<Long> passengerIds, LocalDateTime now) {
        if (b.getStatus() == BookingStatus.PAYMENT_PENDING) {
            throw ApiException.conflict("NOT_PAID", "This booking is not paid yet. Release the hold from the payment page instead.");
        }
        if (!b.getStatus().isLive()) {
            throw ApiException.conflict("NOT_CANCELLABLE", "This ticket is already " + BookingMapper.statusLabel(b.getStatus()).toLowerCase() + ".");
        }
        if (passengerIds == null || passengerIds.isEmpty()) {
            throw ApiException.badRequest("NO_TRAVELLERS", "Choose at least one traveller to cancel.");
        }
        Set<Long> wanted = new LinkedHashSet<>(passengerIds);
        List<Passenger> out = new ArrayList<>();
        for (Passenger p : b.orderedPassengers()) {
            if (!wanted.remove(p.getId())) continue;
            if (p.getCurrentStatus() == SeatStatus.CAN) {
                throw ApiException.conflict("ALREADY_CANCELLED", p.getName() + " is already cancelled.");
            }
            if (!canCancel(b, p, now)) {
                throw ApiException.conflict("TOO_LATE", p.getCurrentStatus() == SeatStatus.CNF
                        ? "The train has left " + b.getFromCode() + ", so this ticket can no longer be cancelled."
                        : "RAC and waitlisted tickets can be cancelled up to 30 minutes before departure.");
            }
            out.add(p);
        }
        if (!wanted.isEmpty()) {
            throw ApiException.badRequest("UNKNOWN_TRAVELLER", "One of the chosen travellers is not on this ticket.");
        }
        return out;
    }

    public RefundPolicy policy() {
        List<CancellationRuleDto> rules = ruleRepository.findAllByOrderByMinHoursBeforeDesc().stream()
                .map(r -> new CancellationRuleDto(r.getMinHoursBefore(), r.getDeductionPercent().stripTrailingZeros().toPlainString(),
                        r.isFlatMinimum(), r.getDescription())).toList();
        List<ClassChargeDto> charges = Arrays.stream(CoachClass.values())
                .map(c -> new ClassChargeDto(c.code(), c.label(),
                        c.airConditioned() ? (int) Math.round(c.cancellationFlatCharge() * 1.05) : c.cancellationFlatCharge()))
                .toList();
        return new RefundPolicy(rules, charges, CLERKAGE, "Confirmed Tatkal tickets are not refunded.");
    }
}
