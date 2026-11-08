package com.railway.reservation.service.rail;

import com.railway.reservation.dto.BookingDtos.*;
import com.railway.reservation.dto.TrainDtos.EndPoint;
import com.railway.reservation.dto.TrainDtos.FareDto;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.CancellationAuditLogRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Turns bookings into the views the passenger, the PNR enquiry and the admin console read. */
@Component
public class BookingMapper {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final InventoryService inventoryService;
    private final CancellationService cancellationService;
    private final CancellationAuditLogRepository auditRepository;

    public BookingMapper(InventoryService inventoryService, CancellationService cancellationService,
                         CancellationAuditLogRepository auditRepository) {
        this.inventoryService = inventoryService;
        this.cancellationService = cancellationService;
        this.auditRepository = auditRepository;
    }

    public BookingView toView(Booking b) {
        Map<Passenger, Integer> numbers = currentNumbers(b);
        LocalDateTime now = inventoryService.now();
        List<PassengerView> passengers = b.orderedPassengers().stream().map(p -> new PassengerView(
                p.getId(), p.getSerial(), p.getName(), p.getAge(), p.getGender().name(),
                p.getBerthPreference().name(), p.getFood() == null ? null : p.getFood().name(),
                bookingStatus(p), currentStatus(p, numbers.get(p)), p.getFare().intValue(),
                p.getRefundAmount() == null ? null : p.getRefundAmount().intValue(),
                cancellationService.canCancel(b, p, now))).toList();
        List<PaymentView> payments = b.getPayments().stream().map(pay -> new PaymentView(
                pay.getTxnRef(), pay.getMethod().name(), pay.getInstrument(), pay.getAmount().intValue(),
                pay.getStatus().name(), pay.getFailureReason(), pay.getCreatedAt(),
                pay.getRefundedAmount().intValue())).toList();
        List<CancellationView> cancellations = auditRepository.findByPnrOrderByCancelledAtAsc(b.getPnr()).stream()
                .map(a -> new CancellationView(a.getCancelledAt(), a.getReason(), a.getPassengersCancelled(),
                        a.getRefundAmount().intValue(), a.getRefundRef(), a.getRefundStatus())).toList();
        int travellers = Math.max(1, b.getPassengers().size());
        return new BookingView(b.getPnr(), b.getStatus().name(), statusLabel(b.getStatus()), trainRef(b.getTrain()),
                b.getCoachClass().code(), b.getCoachClass().label(), b.getQuota().code(), b.getQuota().label(),
                from(b), to(b), b.getJourneyDate(), minutes(b), b.getDistanceKm(), fare(b),
                b.getTotalFare().intValue() / travellers, b.getRefundTotal().intValue(), b.getContactMobile(),
                b.getUser().getEmail(), b.getUser().getName(), b.getCreatedAt(), b.getPaymentDueAt(), b.getBookedAt(),
                inventoryService.chartPrepared(b.getTrain(), b.getTrainStartDate()),
                inventoryService.chartPreparedAt(b.getTrain(), b.getTrainStartDate()),
                passengers.stream().anyMatch(PassengerView::cancellable), passengers, payments, cancellations);
    }

    public BookingSummary toSummary(Booking b) {
        Map<Passenger, Integer> numbers = currentNumbers(b);
        List<Passenger> active = b.activePassengers();
        String seats;
        if (active.isEmpty()) {
            seats = "Cancelled";
        } else {
            seats = String.join(", ", active.stream().map(p -> currentStatus(p, numbers.get(p)).text()).toList());
        }
        return new BookingSummary(b.getPnr(), b.getStatus().name(), statusLabel(b.getStatus()), trainRef(b.getTrain()),
                b.getCoachClass().code(), b.getQuota().code(), from(b), to(b), b.getDepartureAt(),
                b.getPassengers().size(), active.size(), seats, b.getTotalFare().intValue(),
                b.getRefundTotal().intValue(), b.getPaymentDueAt());
    }

    /** Public PNR enquiry: statuses only, never names, ages or contact details. */
    public PnrStatus toPnrStatus(Booking b) {
        Map<Passenger, Integer> numbers = currentNumbers(b);
        List<PnrPassenger> pax = b.orderedPassengers().stream()
                .map(p -> new PnrPassenger(p.getSerial(), bookingStatus(p), currentStatus(p, numbers.get(p))))
                .toList();
        return new PnrStatus(b.getPnr(), b.getStatus().name(), statusLabel(b.getStatus()), trainRef(b.getTrain()),
                b.getCoachClass().code(), b.getCoachClass().label(), b.getQuota().code(), from(b), to(b),
                b.getJourneyDate(), inventoryService.chartPrepared(b.getTrain(), b.getTrainStartDate()),
                inventoryService.chartPreparedAt(b.getTrain(), b.getTrainStartDate()), pax);
    }

    // ------------------------------------------------------------------ pieces

    private Map<Passenger, Integer> currentNumbers(Booking b) {
        Map<Passenger, Integer> out = new IdentityHashMap<>();
        boolean queued = b.getPassengers().stream()
                .anyMatch(p -> p.getCurrentStatus() == SeatStatus.RAC || p.getCurrentStatus() == SeatStatus.WL);
        if (!queued || !InventoryService.HOLDING.contains(b.getStatus())) return out;
        TrainClass tc = b.getTrain().classOf(b.getCoachClass()).orElse(null);
        if (tc == null) return out;
        SeatLedger ledger = inventoryService.readLedger(tc, b.getTrainStartDate());
        for (Passenger p : b.getPassengers()) {
            Integer n = ledger.currentNumber(p);
            if (n != null) out.put(p, n);
        }
        return out;
    }

    static StatusView bookingStatus(Passenger p) {
        return status(p.getBookingStatus(), p.getBookingCoach(), p.getBookingBerth(),
                p.getBookingBerth() == null ? null : p.getBooking().getCoachClass().berthTypeOf(p.getBookingBerth()),
                p.getBookingNumber());
    }

    static StatusView currentStatus(Passenger p, Integer number) {
        if (p.getCurrentStatus() == SeatStatus.CAN) return status(SeatStatus.CAN, null, null, null, null);
        Integer n = number != null ? number : p.getBookingNumber();
        return status(p.getCurrentStatus(), p.getCoach(), p.getBerthNo(), p.getBerthType(), n);
    }

    private static StatusView status(SeatStatus s, String coach, Integer berth, BerthType type, Integer number) {
        String text = switch (s) {
            case CNF -> coach == null ? "CNF" : "CNF " + coach + " " + berth + (type == null ? "" : " " + type.code());
            case RAC -> "RAC " + (number == null ? "" : number);
            case WL -> "WL " + (number == null ? "" : number);
            case CAN -> "Cancelled";
        };
        return new StatusView(s.name(), coach, berth, type == null ? null : type.code(),
                s == SeatStatus.RAC || s == SeatStatus.WL ? number : null, text.trim());
    }

    public static String statusLabel(BookingStatus s) {
        return switch (s) {
            case PAYMENT_PENDING -> "Awaiting payment";
            case BOOKED -> "Booked";
            case PARTIALLY_CANCELLED -> "Partly cancelled";
            case CANCELLED -> "Cancelled";
            case PAYMENT_FAILED -> "Payment not completed";
            case EXPIRED -> "Hold expired";
        };
    }

    static TrainRef trainRef(Train t) {
        return new TrainRef(t.getNumber(), t.getName(), t.getType().name(), t.getType().label());
    }

    static EndPoint from(Booking b) {
        TrainStop s = b.getTrain().stopFor(b.getFromCode()).orElseThrow();
        return new EndPoint(s.getStation().getCode(), s.getStation().getName(), s.getStation().getCity(),
                b.getDepartureAt().toLocalTime().format(HM), b.getDepartureAt().toLocalDate(),
                s.getDepartureDay() == null ? 1 : s.getDepartureDay());
    }

    static EndPoint to(Booking b) {
        TrainStop s = b.getTrain().stopFor(b.getToCode()).orElseThrow();
        return new EndPoint(s.getStation().getCode(), s.getStation().getName(), s.getStation().getCity(),
                b.getArrivalAt().toLocalTime().format(HM), b.getArrivalAt().toLocalDate(),
                s.getArrivalDay() == null ? 1 : s.getArrivalDay());
    }

    static int minutes(Booking b) {
        return (int) java.time.Duration.between(b.getDepartureAt(), b.getArrivalAt()).toMinutes();
    }

    static FareDto fare(Booking b) {
        return new FareDto(i(b.getBaseFare()), i(b.getReservationCharge()), i(b.getSuperfastCharge()),
                i(b.getTatkalCharge()), i(b.getCateringCharge()), i(b.getGst()), i(b.getTotalFare()));
    }

    private static int i(BigDecimal v) { return v == null ? 0 : v.intValue(); }
}
