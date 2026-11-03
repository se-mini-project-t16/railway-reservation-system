package com.railway.reservation.service;

import com.railway.reservation.model.*;
import com.railway.reservation.service.rail.DemandSimulator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Simulated sales from other channels: repeatable, busier close to departure, inside the right pool. */
class DemandSimulatorTest {

    private final DemandSimulator simulator = new DemandSimulator(true);
    private final LocalDate today = LocalDate.of(2026, 11, 3);

    private TrainClass sleeper() {
        Train t = new Train("12627", "Test", TrainType.SUPERFAST, "1111111", false);
        TrainClass tc = new TrainClass(CoachClass.SLEEPER, 10);
        t.addClass(tc);
        return tc;
    }

    @Test
    @DisplayName("TC-DEM-01: The same departure always gets the same simulated sales")
    void deterministic() {
        TrainClass tc = sleeper();
        ClassInventory a = simulator.generate(tc, today.plusDays(5), Quota.GENERAL, today);
        ClassInventory b = simulator.generate(tc, today.plusDays(5), Quota.GENERAL, today);
        assertThat(a.externalSet()).isEqualTo(b.externalSet());
        assertThat(a.getExternalWl()).isEqualTo(b.getExternalWl());
    }

    @Test
    @DisplayName("TC-DEM-02: A departure tomorrow is busier than one seven weeks away")
    void busierCloseToDeparture() {
        TrainClass tc = sleeper();
        int soon = simulator.generate(tc, today.plusDays(1), Quota.GENERAL, today).externalSet().cardinality();
        int far = simulator.generate(tc, today.plusDays(50), Quota.GENERAL, today).externalSet().cardinality();
        assertThat(soon).isGreaterThan(far);
    }

    @Test
    @DisplayName("TC-DEM-03: Simulated Tatkal sales stay inside the Tatkal berths")
    void tatkalPool() {
        TrainClass tc = sleeper();
        ClassInventory tq = simulator.generate(tc, today.plusDays(1), Quota.TATKAL, today);
        assertThat(tq.externalSet().nextSetBit(0)).isGreaterThanOrEqualTo(tc.generalBerths());
        assertThat(tq.externalSet().length()).isLessThanOrEqualTo(tc.capacity());
    }

    @Test
    @DisplayName("TC-DEM-04: Switched off, nothing is pre-sold")
    void disabled() {
        assertThat(new DemandSimulator(false).generate(sleeper(), today.plusDays(1), Quota.GENERAL, today).externalSet().isEmpty()).isTrue();
    }
}
