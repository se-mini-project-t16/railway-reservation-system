package com.railway.reservation.service.payment;

/** The payment provider could not be reached or refused the request. */
public class GatewayException extends RuntimeException {

    /** HTTP status Razorpay answered with, or 0 when it could not be reached. */
    private final int status;

    public GatewayException(String message) {
        this(message, 0, null);
    }

    public GatewayException(String message, Throwable cause) {
        this(message, 0, cause);
    }

    public GatewayException(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() { return status; }

    /** Razorpay looked and said no (an unknown id, for example), as opposed to being unreachable or misconfigured. */
    public boolean rejected() {
        return status == 400 || status == 404;
    }
}
