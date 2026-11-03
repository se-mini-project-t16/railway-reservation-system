package com.railway.reservation.service.rail;

import com.railway.reservation.model.CoachClass;
import com.railway.reservation.model.Quota;
import com.railway.reservation.model.Train;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Distance-based fares. The base fare tapers with distance (telescopic
 * slabs), premium trains carry a higher rate plus catering, and AC classes
 * pay 5% GST. Every component is a whole rupee so the ticket adds up.
 */
@Component
public class FareCalculator {

    private static final int[][] SLABS = {{500, 100}, {1000, 85}, {2500, 70}, {Integer.MAX_VALUE, 60}};
    private static final BigDecimal PREMIUM_RATE = new BigDecimal("1.20");
    private static final BigDecimal GST_RATE = new BigDecimal("0.05");

    public record Fare(int base, int reservation, int superfast, int tatkal, int catering, int gst) {
        public int total() { return base + reservation + superfast + tatkal + catering + gst; }

        public Fare times(int n) {
            return new Fare(base * n, reservation * n, superfast * n, tatkal * n, catering * n, gst * n);
        }

        public BigDecimal totalAmount() { return BigDecimal.valueOf(total()); }
    }

    public Fare perPassenger(Train train, CoachClass cc, int distanceKm, Quota quota) {
        int chargeable = Math.max(distanceKm, cc.minimumChargeableKm());
        BigDecimal rate = cc.ratePerKm();
        if (train.getType().premium()) {
            rate = rate.multiply(PREMIUM_RATE);
        }
        int base = BigDecimal.valueOf(weightedKm(chargeable)).multiply(rate)
                .setScale(0, RoundingMode.HALF_UP).intValue();
        int reservation = cc.reservationCharge();
        int superfast = train.isSuperfast() ? cc.superfastCharge() : 0;
        int tatkal = 0;
        if (quota == Quota.TATKAL) {
            int pct = base * cc.tatkalPercent() / 100;
            tatkal = Math.min(cc.tatkalMax(), Math.max(cc.tatkalMin(), pct));
        }
        int catering = train.getType().premium() ? cc.cateringCharge() : 0;
        int gst = 0;
        if (cc.airConditioned()) {
            gst = BigDecimal.valueOf(base + reservation + superfast + tatkal + catering)
                    .multiply(GST_RATE).setScale(0, RoundingMode.HALF_UP).intValue();
        }
        return new Fare(base, reservation, superfast, tatkal, catering, gst);
    }

    /** Kilometres weighted by the slab they fall in: long journeys cost less per km. */
    static double weightedKm(int km) {
        double total = 0;
        int previous = 0;
        for (int[] slab : SLABS) {
            if (km <= previous) break;
            int inSlab = Math.min(km, slab[0]) - previous;
            total += inSlab * slab[1] / 100.0;
            previous = slab[0];
        }
        return total;
    }
}
