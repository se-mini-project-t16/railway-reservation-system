package com.railway.reservation.service.notify;

/** Things that happened to a booking that the passenger is told about by email, once they are committed. */
public final class BookingEvents {

    private BookingEvents() {}

    /** Payment was confirmed and the ticket is booked. */
    public record Paid(String pnr) {}

    /** Travellers were cancelled; {@code refundRef} is the payment provider's refund id when there is one. */
    public record Cancelled(String pnr, int travellers, int refund, String refundRef, String reason) {}
}
