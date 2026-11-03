package com.railway.reservation.model;

public enum BerthType {
    LOWER("LB", "Lower"), MIDDLE("MB", "Middle"), UPPER("UB", "Upper"),
    SIDE_LOWER("SL", "Side lower"), SIDE_UPPER("SU", "Side upper"),
    WINDOW("WS", "Window"), MIDDLE_SEAT("MS", "Middle"), AISLE("AS", "Aisle");

    private final String code;
    private final String label;

    BerthType(String code, String label) { this.code = code; this.label = label; }

    public String code() { return code; }
    public String label() { return label; }
}
