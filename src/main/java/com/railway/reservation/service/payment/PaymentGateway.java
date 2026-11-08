package com.railway.reservation.service.payment;

import java.util.List;
import java.util.Map;

/**
 * The payment provider, seen from the booking side. Amounts are in paise. Every method that talks to
 * the provider throws {@link GatewayException} when it cannot get a definite answer.
 */
public interface PaymentGateway {

    /** True when API keys are configured. */
    boolean enabled();

    /** The public key the browser opens Checkout with. */
    String keyId();

    /** True for the provider's test mode, where no real money moves. */
    boolean testMode();

    GatewayOrder createOrder(long amountPaise, String receipt, Map<String, String> notes);

    GatewayPayment fetchPayment(String paymentId);

    List<GatewayPayment> paymentsForOrder(String orderId);

    GatewayPayment capture(String paymentId, long amountPaise);

    GatewayRefund refund(String paymentId, long amountPaise, Map<String, String> notes);

    /** Checks the signature Checkout returns with a successful payment. */
    boolean signatureValid(String orderId, String paymentId, String signature);

    record GatewayOrder(String id, long amountPaise, String status) {}

    /**
     * One payment attempt. {@code status} is the provider's: created, authorized, captured, refunded or failed.
     * {@code method} is card, upi, netbanking, wallet and so on; {@code instrument} is a masked description.
     */
    record GatewayPayment(String id, String orderId, long amountPaise, String status, String method,
                          String instrument, String errorDescription) {
        public boolean captured() { return "captured".equals(status) || "refunded".equals(status); }
        public boolean authorized() { return "authorized".equals(status); }
        public boolean failed() { return "failed".equals(status); }
    }

    record GatewayRefund(String id, long amountPaise, String status) {}
}
