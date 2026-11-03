package com.railway.reservation.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Travel classes with the operating rules a reservation needs: coach layout,
 * fare rates, fixed charges, RAC capacity, Tatkal charges and the flat
 * cancellation charge. Values follow the structure of Indian Railways rules
 * and are simulation figures, not an official tariff.
 */
public enum CoachClass {

    //            code  label                   prefix perCoach layout          ac     rate   minKm res  sf  racPerCoach tqPct tqMin tqMax cancelFlat catering
    FIRST_AC     ("1A", "First AC",             "H",   24,  BerthLayout.CABIN,   true,  "3.40", 300, 60, 75, 0,  0,  0,   0,   240, 350),
    SECOND_AC    ("2A", "AC 2 Tier",            "A",   48,  BerthLayout.TWO_TIER, true, "1.95", 300, 50, 45, 4,  30, 400, 500, 200, 300),
    THIRD_AC     ("3A", "AC 3 Tier",            "B",   72,  BerthLayout.THREE_TIER, true, "1.38", 300, 40, 45, 8, 30, 300, 400, 180, 250),
    THIRD_ECONOMY("3E", "AC 3 Economy",         "M",   80,  BerthLayout.THREE_TIER, true, "1.26", 300, 40, 45, 8, 30, 300, 400, 180, 250),
    SLEEPER      ("SL", "Sleeper",              "S",   72,  BerthLayout.THREE_TIER, false, "0.52", 200, 20, 30, 14, 30, 100, 200, 120, 0),
    CHAIR_CAR    ("CC", "AC Chair Car",         "C",   78,  BerthLayout.SEAT_3_2, true,  "1.12", 150, 40, 45, 0,  30, 125, 225, 180, 220),
    EXEC_CHAIR   ("EC", "Executive Chair Car",  "E",   56,  BerthLayout.SEAT_2_2, true,  "2.30", 150, 60, 75, 0,  30, 400, 500, 240, 300),
    SECOND_SITTING("2S","Second Sitting",       "D",   108, BerthLayout.SEAT_3_3, false, "0.30", 50,  15, 15, 0,  10, 10,  15,  60,  0);

    private final String code;
    private final String label;
    private final String coachPrefix;
    private final int berthsPerCoach;
    private final BerthLayout layout;
    private final boolean airConditioned;
    private final BigDecimal ratePerKm;
    private final int minimumChargeableKm;
    private final int reservationCharge;
    private final int superfastCharge;
    private final int racPerCoach;
    private final int tatkalPercent;
    private final int tatkalMin;
    private final int tatkalMax;
    private final int cancellationFlatCharge;
    private final int cateringCharge;

    CoachClass(String code, String label, String coachPrefix, int berthsPerCoach, BerthLayout layout,
               boolean airConditioned, String ratePerKm, int minimumChargeableKm, int reservationCharge,
               int superfastCharge, int racPerCoach, int tatkalPercent, int tatkalMin, int tatkalMax,
               int cancellationFlatCharge, int cateringCharge) {
        this.code = code;
        this.label = label;
        this.coachPrefix = coachPrefix;
        this.berthsPerCoach = berthsPerCoach;
        this.layout = layout;
        this.airConditioned = airConditioned;
        this.ratePerKm = new BigDecimal(ratePerKm);
        this.minimumChargeableKm = minimumChargeableKm;
        this.reservationCharge = reservationCharge;
        this.superfastCharge = superfastCharge;
        this.racPerCoach = racPerCoach;
        this.tatkalPercent = tatkalPercent;
        this.tatkalMin = tatkalMin;
        this.tatkalMax = tatkalMax;
        this.cancellationFlatCharge = cancellationFlatCharge;
        this.cateringCharge = cateringCharge;
    }

    public static Optional<CoachClass> fromCode(String code) {
        if (code == null) return Optional.empty();
        String c = code.trim().toUpperCase();
        return Arrays.stream(values()).filter(v -> v.code.equals(c) || v.name().equals(c)).findFirst();
    }

    /** Berth type for a 1-based berth number inside one coach. */
    public BerthType berthTypeOf(int berthNo) {
        return layout.typeOf(berthNo);
    }

    public boolean hasBerths() { return layout.sleeper; }
    public boolean supportsRac() { return racPerCoach > 0; }
    public boolean supportsTatkal() { return tatkalPercent > 0; }

    public String code() { return code; }
    public String label() { return label; }
    public String coachPrefix() { return coachPrefix; }
    public int berthsPerCoach() { return berthsPerCoach; }
    public BerthLayout layout() { return layout; }
    public boolean airConditioned() { return airConditioned; }
    public BigDecimal ratePerKm() { return ratePerKm; }
    public int minimumChargeableKm() { return minimumChargeableKm; }
    public int reservationCharge() { return reservationCharge; }
    public int superfastCharge() { return superfastCharge; }
    public int racPerCoach() { return racPerCoach; }
    public int tatkalPercent() { return tatkalPercent; }
    public int tatkalMin() { return tatkalMin; }
    public int tatkalMax() { return tatkalMax; }
    public int cancellationFlatCharge() { return cancellationFlatCharge; }
    public int cateringCharge() { return cateringCharge; }

    public enum BerthLayout {
        CABIN(true, new BerthType[]{BerthType.LOWER, BerthType.UPPER, BerthType.LOWER, BerthType.UPPER}),
        TWO_TIER(true, new BerthType[]{BerthType.LOWER, BerthType.UPPER, BerthType.LOWER, BerthType.UPPER,
                BerthType.SIDE_LOWER, BerthType.SIDE_UPPER}),
        THREE_TIER(true, new BerthType[]{BerthType.LOWER, BerthType.MIDDLE, BerthType.UPPER, BerthType.LOWER,
                BerthType.MIDDLE, BerthType.UPPER, BerthType.SIDE_LOWER, BerthType.SIDE_UPPER}),
        SEAT_3_2(false, new BerthType[]{BerthType.WINDOW, BerthType.MIDDLE_SEAT, BerthType.AISLE, BerthType.AISLE,
                BerthType.WINDOW}),
        SEAT_2_2(false, new BerthType[]{BerthType.WINDOW, BerthType.AISLE, BerthType.AISLE, BerthType.WINDOW}),
        SEAT_3_3(false, new BerthType[]{BerthType.WINDOW, BerthType.MIDDLE_SEAT, BerthType.AISLE, BerthType.AISLE,
                BerthType.MIDDLE_SEAT, BerthType.WINDOW});

        private final boolean sleeper;
        private final BerthType[] cycle;

        BerthLayout(boolean sleeper, BerthType[] cycle) {
            this.sleeper = sleeper;
            this.cycle = cycle;
        }

        public BerthType typeOf(int berthNo) {
            return cycle[(berthNo - 1) % cycle.length];
        }

        public int bayLength() { return cycle.length; }
        public boolean isSleeper() { return sleeper; }
    }
}
