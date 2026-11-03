package com.railway.reservation.model;

public enum Quota {
    GENERAL("GN", "General"), TATKAL("TQ", "Tatkal");

    private final String code;
    private final String label;

    Quota(String code, String label) { this.code = code; this.label = label; }

    public String code() { return code; }
    public String label() { return label; }

    public static Quota fromCode(String value) {
        if (value == null || value.isBlank()) return GENERAL;
        for (Quota q : values()) {
            if (q.code.equalsIgnoreCase(value) || q.name().equalsIgnoreCase(value)) return q;
        }
        throw new IllegalArgumentException("Unknown quota: " + value);
    }
}
