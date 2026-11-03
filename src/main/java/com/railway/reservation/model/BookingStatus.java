package com.railway.reservation.model;

public enum BookingStatus {
    /** Berths are held while the passenger pays; released when the hold expires. */
    PAYMENT_PENDING,
    /** Paid; individual travellers may be CNF, RAC or WL. */
    BOOKED,
    /** Paid, with at least one traveller cancelled and at least one still travelling. */
    PARTIALLY_CANCELLED,
    CANCELLED,
    /** The passenger abandoned payment, so the hold was released. */
    PAYMENT_FAILED,
    /** Payment was not completed before the hold expired. */
    EXPIRED;

    public boolean isLive() { return this == BOOKED || this == PARTIALLY_CANCELLED; }
    public boolean holdsInventory() { return this == PAYMENT_PENDING || isLive(); }
}
