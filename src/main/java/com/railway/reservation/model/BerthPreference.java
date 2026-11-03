package com.railway.reservation.model;

/** What a traveller asks for; allocation honours it when a matching berth is free. */
public enum BerthPreference {
    NONE(null), LOWER(BerthType.LOWER), MIDDLE(BerthType.MIDDLE), UPPER(BerthType.UPPER),
    SIDE_LOWER(BerthType.SIDE_LOWER), SIDE_UPPER(BerthType.SIDE_UPPER),
    WINDOW(BerthType.WINDOW), AISLE(BerthType.AISLE);

    private final BerthType matches;

    BerthPreference(BerthType matches) { this.matches = matches; }

    public boolean isSatisfiedBy(BerthType type) { return matches != null && matches == type; }
    public boolean appliesTo(CoachClass coachClass) {
        if (this == NONE) return true;
        boolean seat = this == WINDOW || this == AISLE;
        return seat != coachClass.hasBerths();
    }
}
