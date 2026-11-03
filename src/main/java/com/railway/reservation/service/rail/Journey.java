package com.railway.reservation.service.rail;

import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainStop;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A traveller's stretch of one train's run, fixed to dates. */
public record Journey(Train train, TrainStop from, TrainStop to, LocalDate trainStartDate,
                      LocalDateTime departureAt, LocalDateTime arrivalAt) {

    /** Builds the journey for a traveller boarding on {@code boardingDate}, or null if the stops are out of order. */
    public static Journey of(Train train, TrainStop from, TrainStop to, LocalDate boardingDate) {
        if (from.getDepartureMinute() == null || to.getArrivalMinute() == null || from.getSeq() >= to.getSeq()) {
            return null;
        }
        LocalDate start = boardingDate.minusDays(from.getDepartureDay() - 1L);
        LocalDateTime base = start.atStartOfDay();
        return new Journey(train, from, to, start,
                base.plusMinutes(from.getDepartureMinute()), base.plusMinutes(to.getArrivalMinute()));
    }

    public boolean runs() { return train.runsOn(trainStartDate); }
    public int distanceKm() { return to.getDistanceKm() - from.getDistanceKm(); }
    public int durationMinutes() { return to.getArrivalMinute() - from.getDepartureMinute(); }
    public int stopsBetween() { return to.getSeq() - from.getSeq() - 1; }
    public LocalDate boardingDate() { return departureAt.toLocalDate(); }
}
