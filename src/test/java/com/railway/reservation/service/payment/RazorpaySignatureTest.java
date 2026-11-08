package com.railway.reservation.service.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpaySignatureTest {

    private static final String SECRET = "Ab12Cd34Ef56Gh78";
    private static final String ORDER = "order_RZtTest0001";
    private static final String PAYMENT = "pay_RZtTest0002";
    /** HMAC-SHA256("order_RZtTest0001|pay_RZtTest0002") with the secret above, computed independently. */
    private static final String EXPECTED = "bf9cdba74d5523b36add94b9637f486e67989cfe1abd9b8d48c581a5ffd13338";

    @Test
    @DisplayName("TC-PAY-10: The payment signature is HMAC-SHA256 of order id | payment id with the key secret")
    void signsLikeRazorpay() {
        assertThat(RazorpaySignature.sign(SECRET, ORDER, PAYMENT)).isEqualTo(EXPECTED);
        assertThat(RazorpaySignature.matches(SECRET, ORDER, PAYMENT, EXPECTED)).isTrue();
        assertThat(RazorpaySignature.matches(SECRET, ORDER, PAYMENT, EXPECTED.toUpperCase())).isTrue();
    }

    @Test
    @DisplayName("TC-PAY-11: Any change to the order, payment, secret or signature fails the check")
    void rejectsTampering() {
        assertThat(RazorpaySignature.matches(SECRET, "order_RZtTest9999", PAYMENT, EXPECTED)).isFalse();
        assertThat(RazorpaySignature.matches(SECRET, ORDER, "pay_RZtTest9999", EXPECTED)).isFalse();
        assertThat(RazorpaySignature.matches("another-secret", ORDER, PAYMENT, EXPECTED)).isFalse();
        assertThat(RazorpaySignature.matches(SECRET, ORDER, PAYMENT, EXPECTED.substring(1) + "0")).isFalse();
        assertThat(RazorpaySignature.matches(SECRET, ORDER, PAYMENT, null)).isFalse();
        assertThat(RazorpaySignature.matches(null, ORDER, PAYMENT, EXPECTED)).isFalse();
    }
}
