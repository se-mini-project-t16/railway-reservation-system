package com.railway.reservation.service.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Razorpay's payment signature: HMAC-SHA256 of {@code order_id + "|" + payment_id} with the key secret,
 * as lowercase hex. Only the server knows the secret, so a matching signature proves Razorpay sent it.
 */
public final class RazorpaySignature {

    private RazorpaySignature() {}

    public static String sign(String secret, String orderId, String paymentId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    public static boolean matches(String secret, String orderId, String paymentId, String signature) {
        if (secret == null || orderId == null || paymentId == null || signature == null) return false;
        byte[] expected = sign(secret, orderId, paymentId).getBytes(StandardCharsets.US_ASCII);
        byte[] given = signature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, given);
    }
}
