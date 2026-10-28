package com.railway.reservation.exception;

/**
 * Raised when a full ticket or PDF is requested before the server-side payment
 * record has a verified successful state.
 */
public class PaymentNotVerifiedException extends RuntimeException {

    public PaymentNotVerifiedException(String message) {
        super(message);
    }
}
