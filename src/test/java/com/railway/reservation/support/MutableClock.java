package com.railway.reservation.support;

import com.railway.reservation.config.TimeConfig;

import java.time.*;

/** A clock tests can move, so Tatkal opening, chart time and hold expiry can be exercised. */
public class MutableClock extends Clock {

    private Instant instant;

    public MutableClock(LocalDateTime start) {
        this.instant = start.atZone(TimeConfig.IST).toInstant();
    }

    public void set(LocalDateTime time) { this.instant = time.atZone(TimeConfig.IST).toInstant(); }
    public void advance(Duration d) { this.instant = instant.plus(d); }
    public void reset() { this.instant = Instant.now(); }

    @Override public ZoneId getZone() { return TimeConfig.IST; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return instant; }
}
