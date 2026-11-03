package com.railway.reservation.model;

public enum TrainType {
    RAJDHANI("Rajdhani", true), SHATABDI("Shatabdi", true), VANDE_BHARAT("Vande Bharat", true),
    DURONTO("Duronto", true), GATIMAAN("Gatimaan", true), JAN_SHATABDI("Jan Shatabdi", false),
    SUPERFAST("Superfast", false), MAIL_EXPRESS("Mail/Express", false);

    private final String label;
    /** Premium trains price catering into the fare and charge a higher base rate. */
    private final boolean premium;

    TrainType(String label, boolean premium) { this.label = label; this.premium = premium; }

    public String label() { return label; }
    public boolean premium() { return premium; }
}
