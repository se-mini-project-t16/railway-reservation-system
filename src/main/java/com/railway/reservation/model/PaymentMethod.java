package com.railway.reservation.model;

import java.util.Locale;

public enum PaymentMethod {
    UPI, CARD, NETBANKING, WALLET, OTHER;

    /** Maps the provider's method name (upi, card, netbanking, wallet, ...) to ours. */
    public static PaymentMethod fromGateway(String method) {
        if (method == null) return OTHER;
        return switch (method.toLowerCase(Locale.ROOT)) {
            case "upi" -> UPI;
            case "card", "emi" -> CARD;
            case "netbanking" -> NETBANKING;
            case "wallet" -> WALLET;
            default -> OTHER;
        };
    }
}
