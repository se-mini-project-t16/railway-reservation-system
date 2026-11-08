package com.railway.reservation.support;

import com.railway.reservation.service.payment.GatewayException;
import com.railway.reservation.service.payment.PaymentGateway;
import com.railway.reservation.service.payment.RazorpaySignature;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for Razorpay in tests. It keeps orders, payments and refunds in memory, signs payments
 * the way Razorpay does (with {@link #SECRET}), and lets a test play the passenger in Checkout:
 * {@link #pay}, {@link #decline} or {@link #authorize}. {@link #down} makes every call fail.
 */
public class FakeRazorpay implements PaymentGateway {

    public static final String SECRET = "test_secret_for_signatures";

    public record Paid(String orderId, String paymentId, String signature) {}
    public record RefundCall(String paymentId, long amountPaise, String id) {}

    private final Map<String, Long> orders = new ConcurrentHashMap<>();
    private final Map<String, GatewayPayment> payments = new ConcurrentHashMap<>();
    private final List<RefundCall> refunds = new CopyOnWriteArrayList<>();
    private final AtomicInteger seq = new AtomicInteger();
    private volatile boolean enabled = true;
    private volatile boolean down;
    private volatile boolean refundsFail;

    public void reset() {
        orders.clear();
        payments.clear();
        refunds.clear();
        enabled = true;
        down = false;
        refundsFail = false;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDown(boolean down) { this.down = down; }
    public void setRefundsFail(boolean fail) { this.refundsFail = fail; }
    public List<RefundCall> refunds() { return refunds; }
    public int orderCount() { return orders.size(); }

    private String id(String prefix) {
        return prefix + "Test" + String.format("%010d", seq.incrementAndGet());
    }

    private void check() {
        if (down) throw new GatewayException("Razorpay could not be reached");
    }

    /** The passenger completes payment in Checkout; Razorpay captures it. */
    public Paid pay(String orderId, String method, String instrument) {
        return add(orderId, "captured", method, instrument, null);
    }

    /** The bank or UPI app refuses the payment. */
    public Paid decline(String orderId, String method, String instrument, String reason) {
        return add(orderId, "failed", method, instrument, reason);
    }

    /** Authorised but not yet captured (Razorpay accounts set to manual capture). */
    public Paid authorize(String orderId) {
        return add(orderId, "authorized", "upi", "UPI asha@okaxis", null);
    }

    private Paid add(String orderId, String status, String method, String instrument, String reason) {
        Long amount = orders.get(orderId);
        if (amount == null) throw new IllegalArgumentException("Unknown order " + orderId);
        String pid = id("pay_");
        payments.put(pid, new GatewayPayment(pid, orderId, amount, status, method, instrument, reason));
        return new Paid(orderId, pid, RazorpaySignature.sign(SECRET, orderId, pid));
    }

    @Override public boolean enabled() { return enabled; }
    @Override public String keyId() { return "rzp_test_FakeKey"; }
    @Override public boolean testMode() { return true; }

    @Override
    public GatewayOrder createOrder(long amountPaise, String receipt, Map<String, String> notes) {
        check();
        String oid = id("order_");
        orders.put(oid, amountPaise);
        return new GatewayOrder(oid, amountPaise, "created");
    }

    @Override
    public GatewayPayment fetchPayment(String paymentId) {
        check();
        GatewayPayment p = payments.get(paymentId);
        if (p == null) throw new GatewayException("The id provided does not exist");
        return p;
    }

    @Override
    public List<GatewayPayment> paymentsForOrder(String orderId) {
        check();
        return payments.values().stream().filter(p -> orderId.equals(p.orderId()))
                .sorted(Comparator.comparing(GatewayPayment::id)).toList();
    }

    @Override
    public GatewayPayment capture(String paymentId, long amountPaise) {
        check();
        GatewayPayment p = fetchPayment(paymentId);
        GatewayPayment captured = new GatewayPayment(p.id(), p.orderId(), p.amountPaise(), "captured", p.method(), p.instrument(), null);
        payments.put(paymentId, captured);
        return captured;
    }

    @Override
    public GatewayRefund refund(String paymentId, long amountPaise, Map<String, String> notes) {
        check();
        if (refundsFail) throw new GatewayException("The refund could not be processed");
        GatewayPayment p = fetchPayment(paymentId);
        long already = refunds.stream().filter(r -> r.paymentId().equals(paymentId)).mapToLong(RefundCall::amountPaise).sum();
        if (amountPaise <= 0 || already + amountPaise > p.amountPaise()) {
            throw new GatewayException("The refund amount provided is greater than amount captured");
        }
        String rid = id("rfnd_");
        refunds.add(new RefundCall(paymentId, amountPaise, rid));
        return new GatewayRefund(rid, amountPaise, "processed");
    }

    @Override
    public boolean signatureValid(String orderId, String paymentId, String signature) {
        return RazorpaySignature.matches(SECRET, orderId, paymentId, signature);
    }

    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        public FakeRazorpay fakeRazorpay() {
            return new FakeRazorpay();
        }
    }
}
