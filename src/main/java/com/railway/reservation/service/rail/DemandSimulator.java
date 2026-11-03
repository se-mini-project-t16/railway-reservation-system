package com.railway.reservation.service.rail;

import com.railway.reservation.model.ClassInventory;
import com.railway.reservation.model.CoachClass;
import com.railway.reservation.model.Quota;
import com.railway.reservation.model.TrainClass;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates the seats other channels have already sold for a departure, so a
 * search shows the mix a real network shows: plenty of berths a month out,
 * waiting lists on a Friday night train tomorrow. The result is deterministic
 * for a train, class, quota and date, and is persisted the first time it is
 * needed, after which real bookings and cancellations move it.
 */
@Component
public class DemandSimulator {

    private final boolean enabled;

    public DemandSimulator(@org.springframework.beans.factory.annotation.Value("${app.simulated-demand:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public ClassInventory generate(TrainClass tc, LocalDate trainStartDate, Quota quota, LocalDate today) {
        ClassInventory inv = new ClassInventory(tc, trainStartDate, quota);
        String trainNo = tc.getTrain().getNumber();
        Random random = new Random((trainNo + "|" + tc.getCoachClass().name() + "|" + trainStartDate + "|" + quota).hashCode());

        int poolStart = quota == Quota.TATKAL ? tc.generalBerths() : 0;
        int poolEnd = quota == Quota.TATKAL ? tc.capacity() : tc.generalBerths();
        int pool = poolEnd - poolStart;
        if (pool <= 0 || !enabled) {
            return inv;
        }

        double load = quota == Quota.TATKAL
                ? 0.75 + random.nextDouble() * 0.75
                : regularLoad(tc, trainStartDate, today, random);

        int occupied = (int) Math.round(Math.min(1.0, Math.max(0, load)) * pool);
        List<Integer> indices = new ArrayList<>(pool);
        for (int i = poolStart; i < poolEnd; i++) indices.add(i);
        Collections.shuffle(indices, random);
        BitSet set = new BitSet(tc.capacity());
        for (int i = 0; i < occupied; i++) set.set(indices.get(i));
        inv.setExternalSet(set);

        if (load > 1.0) {
            int overflow = (int) Math.round((load - 1.0) * pool * 0.5);
            int rac = Math.min(tc.racLimit(quota), overflow);
            int wl = Math.min(tc.waitlistLimit(quota), Math.max(0, overflow - rac));
            inv.setExternalRac(rac);
            inv.setExternalWl(wl);
        }
        return inv;
    }

    private double regularLoad(TrainClass tc, LocalDate start, LocalDate today, Random random) {
        long daysAhead = Math.max(0, ChronoUnit.DAYS.between(today, start));
        double horizon;
        if (daysAhead <= 1) horizon = 1.04;
        else if (daysAhead <= 3) horizon = 0.94;
        else if (daysAhead <= 7) horizon = 0.84;
        else if (daysAhead <= 15) horizon = 0.70;
        else if (daysAhead <= 30) horizon = 0.52;
        else if (daysAhead <= 45) horizon = 0.38;
        else horizon = 0.26;

        double popularity = switch (tc.getTrain().getType()) {
            case RAJDHANI, VANDE_BHARAT -> 1.0;
            case SHATABDI, DURONTO, SUPERFAST -> 0.95;
            case MAIL_EXPRESS, JAN_SHATABDI -> 0.9;
            case GATIMAAN -> 0.75;
        };
        popularity += ((Math.abs(tc.getTrain().getNumber().hashCode()) % 31) - 15) / 100.0;

        double classFactor = switch (tc.getCoachClass()) {
            case SLEEPER -> 1.08;
            case THIRD_AC -> 1.05;
            case SECOND_AC -> 0.92;
            case FIRST_AC -> 0.80;
            case EXEC_CHAIR -> 0.78;
            default -> 1.0;
        };

        DayOfWeek dow = start.getDayOfWeek();
        double weekend = dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SUNDAY ? 0.06 : 0;
        return popularity * horizon * classFactor + weekend + random.nextGaussian() * 0.09;
    }

    /** Coach code shown to passengers, for example S4 or B2. */
    public static String coachLabel(CoachClass cc, int coachNo) {
        return cc.coachPrefix() + coachNo;
    }
}
