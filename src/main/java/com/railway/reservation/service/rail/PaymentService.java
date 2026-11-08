package com.railway.reservation.service.rail;

import com.railway.reservation.dto.BookingDtos.BookingView;
import com.railway.reservation.dto.BookingDtos.CheckoutOrder;
import com.railway.reservation.dto.BookingDtos.PaymentConfirmation;
import com.railway.reservation.dto.BookingDtos.PaymentResult;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.*;
import com.railway.reservation.service.payment.GatewayException;
import com.railway.reservation.service.payment.PaymentGateway;
import com.railway.reservation.service.payment.PaymentGateway.GatewayOrder;
import com.railway.reservation.service.payment.PaymentGateway.GatewayPayment;
import com.railway.reservation.service.payment.PaymentGateway.GatewayRefund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Online payment for a held booking through Razorpay (RRS-F-008, RRS-F-009).
 *
 * <p>The server creates a Razorpay order for the exact fare, the browser opens Razorpay Checkout,
 * and the result is trusted only after the server has checked Checkout's signature and fetched the
 * payment from Razorpay itself. No card number, UPI PIN or bank credential ever reaches this server.</p>
 *
 * <p>If the passenger pays and closes the tab before the page reports back, the payment is found
 * when the pay page is reopened or, at the latest, when the hold would lapse: a captured payment then
 * books the ticket instead of the hold expiring. Money that arrives for a booking that can no longer
 * use it (the hold had already ended) is refunded in full automatically.</p>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    /** How long past the hold a lapsed booking waits for Razorpay to answer before it is released anyway. */
    static final int GATEWAY_GRACE_MINUTES = 30;

    private final BookingService bookingService;
    private final SeatOperations seatOperations;
    private final InventoryService inventoryService;
    private final BookingMapper mapper;
    private final PaymentGateway gateway;

    public PaymentService(BookingService bookingService, SeatOperations seatOperations,
                          InventoryService inventoryService, BookingMapper mapper, PaymentGateway gateway) {
        this.bookingService = bookingService;
        this.seatOperations = seatOperations;
        this.inventoryService = inventoryService;
        this.mapper = mapper;
        this.gateway = gateway;
    }

    /** Creates (once) the Razorpay order for this booking's fare and returns what Checkout needs. */
    @Transactional(noRollbackFor = ApiException.class)
    public CheckoutOrder checkout(String pnr, Long userId) {
        Booking b = bookingService.lockOwned(pnr, userId, false);
        requireOpen(b);
        if (!gateway.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENTS_UNAVAILABLE",
                    "Online payment is not available right now. Your berths stay held until "
                            + b.getPaymentDueAt().toLocalTime().withNano(0) + ".");
        }
        LocalDateTime now = inventoryService.now();
        if (now.isAfter(b.getPaymentDueAt())) {
            closeLapsedHold(b);
            requireOpen(b);
            throw ApiException.conflict("HOLD_EXPIRED", "Your seat hold has ended. Search again to book.");
        }
        long amount = paise(b.getTotalFare());
        if (b.getPaymentOrderId() == null) {
            GatewayOrder order = gw(() -> gateway.createOrder(amount, b.getPnr(),
                    Map.of("pnr", b.getPnr(), "train", b.getTrain().getNumber())));
            b.setPaymentOrderId(order.id());
            log.info("Payment order created pnr={} order={} amount={}", b.getPnr(), order.id(), b.getTotalFare());
        }
        User u = b.getUser();
        return new CheckoutOrder(gateway.keyId(), b.getPaymentOrderId(), amount, "INR", "Railway Reservation",
                "PNR " + b.getPnr() + ", train " + b.getTrain().getNumber() + ", " + b.getFromCode() + " to " + b.getToCode(),
                Map.of("name", u.getName(), "email", u.getEmail(), "contact", b.getContactMobile()),
                Math.max(0, Duration.between(now, b.getPaymentDueAt()).toSeconds()), gateway.testMode());
    }

    /** Checkout's success callback: verified by signature, then confirmed with Razorpay before booking. */
    @Transactional(noRollbackFor = ApiException.class)
    public PaymentResult confirm(String pnr, Long userId, PaymentConfirmation c) {
        Booking b = bookingService.lockOwned(pnr, userId, false);
        Payment known = b.payment(c.paymentId());
        if (known != null) return result(b, known);
        if (!c.orderId().equals(b.getPaymentOrderId())) throw mismatch();
        if (!gateway.signatureValid(c.orderId(), c.paymentId(), c.signature())) {
            log.warn("Payment signature did not match pnr={} payment={}", b.getPnr(), c.paymentId());
            throw ApiException.badRequest("PAYMENT_UNVERIFIED", "We could not verify this payment. If you were charged, "
                    + "your ticket is booked or the money refunded automatically within a few minutes.");
        }
        GatewayPayment p = gw(() -> gateway.fetchPayment(c.paymentId()));
        if (!c.orderId().equals(p.orderId())) throw mismatch();
        return settle(b, p);
    }

    /** Checkout reported a failed attempt. The attempt is looked up at Razorpay, never taken from the browser. */
    @Transactional(noRollbackFor = ApiException.class)
    public BookingView reportFailure(String pnr, Long userId, String paymentId) {
        Booking b = bookingService.lockOwned(pnr, userId, false);
        if (b.payment(paymentId) == null && b.getPaymentOrderId() != null && gateway.enabled()) {
            GatewayPayment p = gw(() -> gateway.fetchPayment(paymentId));
            if (!b.getPaymentOrderId().equals(p.orderId())) throw mismatch();
            settle(b, p);
        }
        return mapper.toView(b);
    }

    /** Picks up a payment made in an earlier visit whose result never reached this server. */
    @Transactional
    public BookingView sync(String pnr, Long userId) {
        Booking b = bookingService.lockOwned(pnr, userId, false);
        if (b.getStatus() == BookingStatus.PAYMENT_PENDING && b.getPaymentOrderId() != null && gateway.enabled()) {
            try {
                reconcile(b);
            } catch (ApiException e) {
                log.warn("Could not check Razorpay for pnr={}: {}", b.getPnr(), e.getMessage());
            }
        }
        return mapper.toView(b);
    }

    /** The passenger gives up on paying: the hold is released at once instead of waiting for expiry. */
    @Transactional
    public BookingView abandon(String pnr, Long userId) {
        Booking b = bookingService.lockOwned(pnr, userId, false);
        if (b.getStatus() == BookingStatus.PAYMENT_PENDING) {
            if (b.getPaymentOrderId() != null && gateway.enabled()) {
                // Never release berths someone has just paid for
                reconcile(b);
                if (b.getStatus() != BookingStatus.PAYMENT_PENDING) return mapper.toView(b);
            }
            seatOperations.release(b, b.activePassengers());
            b.setStatus(BookingStatus.PAYMENT_FAILED);
            b.setClosedAt(inventoryService.now());
        }
        return mapper.toView(b);
    }

    /**
     * Ends a hold whose time has run out. A payment captured at Razorpay books the ticket instead;
     * if Razorpay cannot be asked, the hold is kept a little longer and tried again. The booking must be locked.
     *
     * @return true when the hold ended (released, or booked by a payment found at Razorpay)
     */
    public boolean closeLapsedHold(Booking b) {
        if (b.getStatus() != BookingStatus.PAYMENT_PENDING) return false;
        if (b.getPaymentOrderId() != null && gateway.enabled()) {
            try {
                reconcile(b);
            } catch (ApiException e) {
                if (inventoryService.now().isBefore(b.getPaymentDueAt().plusMinutes(GATEWAY_GRACE_MINUTES))) {
                    log.warn("Razorpay unavailable, keeping the lapsed hold for now pnr={}", b.getPnr());
                    return false;
                }
                log.error("Releasing pnr={} without a Razorpay check after {} minutes", b.getPnr(), GATEWAY_GRACE_MINUTES);
            }
            if (b.getStatus() != BookingStatus.PAYMENT_PENDING) return true;
        }
        expire(b);
        return true;
    }

    /** Releases an unpaid hold. The booking must be locked. */
    void expire(Booking b) {
        if (b.getStatus() != BookingStatus.PAYMENT_PENDING) return;
        seatOperations.release(b, b.activePassengers());
        b.setStatus(BookingStatus.EXPIRED);
        b.setClosedAt(inventoryService.now());
        log.info("Seat hold expired pnr={}", b.getPnr());
    }

    // ------------------------------------------------------------------ settling a Razorpay payment

    private void reconcile(Booking b) {
        for (GatewayPayment p : gw(() -> gateway.paymentsForOrder(b.getPaymentOrderId()))) {
            if (b.payment(p.id()) == null && (p.captured() || p.authorized() || p.failed())) settle(b, p);
        }
    }

    private PaymentResult settle(Booking b, GatewayPayment found) {
        Payment known = b.payment(found.id());
        if (known != null) return result(b, known);
        GatewayPayment p = found.authorized() ? gw(() -> gateway.capture(found.id(), found.amountPaise())) : found;
        LocalDateTime now = inventoryService.now();

        if (p.failed()) {
            String reason = clip(p.errorDescription() == null ? "The payment was declined." : p.errorDescription());
            b.addPayment(record(b, p, PaymentStatus.FAILED, reason, now));
            log.warn("Payment failed pnr={} payment={} reason={}", b.getPnr(), p.id(), reason);
            return result(b, b.payment(p.id()));
        }
        if (!p.captured()) {
            return new PaymentResult("PENDING", "Razorpay has not confirmed this payment yet. Check My trips in a minute.",
                    p.id(), mapper.toView(b));
        }
        if (b.getStatus() == BookingStatus.PAYMENT_PENDING && p.amountPaise() == paise(b.getTotalFare())) {
            b.addPayment(record(b, p, PaymentStatus.SUCCESS, null, now));
            b.setStatus(BookingStatus.BOOKED);
            b.setBookedAt(now);
            log.info("Payment received pnr={} payment={} amount={}", b.getPnr(), p.id(), b.getTotalFare());
            return result(b, b.payment(p.id()));
        }

        // The money cannot be used (the hold had ended, or the ticket was already paid): return all of it
        String reason = b.getStatus().isLive() ? "This booking was already paid, so this payment was refunded in full."
                : "Paid after the seat hold ended, so it was refunded in full.";
        GatewayRefund refund = gw(() -> gateway.refund(p.id(), p.amountPaise(),
                Map.of("pnr", b.getPnr(), "reason", "unusable payment")));
        Payment returned = record(b, p, PaymentStatus.REFUNDED, null, now);
        returned.markRefunded(clip(reason + " Refund " + refund.id() + "."));
        b.addPayment(returned);
        log.warn("Unusable payment refunded pnr={} payment={} refund={} status={}", b.getPnr(), p.id(), refund.id(), b.getStatus());
        return result(b, returned);
    }

    private Payment record(Booking b, GatewayPayment p, PaymentStatus status, String reason, LocalDateTime now) {
        return new Payment(p.id(), p.orderId(), PaymentMethod.fromGateway(p.method()), p.instrument(),
                BigDecimal.valueOf(p.amountPaise(), 2), status, reason, now);
    }

    private PaymentResult result(Booking b, Payment pay) {
        BookingView view = mapper.toView(b);
        return switch (pay.getStatus()) {
            case SUCCESS -> new PaymentResult("SUCCESS", "Payment received. Your ticket is booked.", pay.getTxnRef(), view);
            case REFUNDED -> new PaymentResult("REFUNDED", pay.getFailureReason(), pay.getTxnRef(), view);
            case FAILED -> new PaymentResult("FAILED", pay.getFailureReason()
                    + (b.getStatus() == BookingStatus.PAYMENT_PENDING
                    ? " Your berths stay held until " + b.getPaymentDueAt().toLocalTime().withNano(0) + "." : ""),
                    pay.getTxnRef(), view);
        };
    }

    private static void requireOpen(Booking b) {
        if (b.getStatus().isLive()) {
            throw ApiException.conflict("ALREADY_PAID", "This booking is already paid.");
        }
        if (b.getStatus() == BookingStatus.EXPIRED) {
            throw ApiException.conflict("HOLD_EXPIRED",
                    "Your seat hold expired before payment, so the seats were released. Search again to book.");
        }
        if (b.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw ApiException.conflict("PAYMENT_CLOSED",
                    "This booking can no longer be paid because its seats were released. Search again to book.");
        }
    }

    private static ApiException mismatch() {
        return ApiException.badRequest("PAYMENT_MISMATCH", "This payment does not belong to this booking.");
    }

    /** Runs a Razorpay call, turning a provider failure into a message the passenger can act on. */
    private static <T> T gw(Supplier<T> call) {
        try {
            return call.get();
        } catch (GatewayException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY_ERROR",
                    "We could not reach Razorpay just now. Try again in a moment.");
        }
    }

    static long paise(BigDecimal rupees) {
        return rupees.movePointRight(2).longValueExact();
    }

    private static String clip(String s) {
        return s.length() > 120 ? s.substring(0, 117) + "..." : s;
    }
}
