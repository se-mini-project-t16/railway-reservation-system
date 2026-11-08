package com.railway.reservation.service;

import com.railway.reservation.model.*;
import com.railway.reservation.repository.CancellationRuleRepository;
import com.railway.reservation.service.rail.CancellationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CancellationRefundTest {

    private final CancellationRuleRepository rules = mock(CancellationRuleRepository.class);
    private final CancellationService service = new CancellationService(rules, null, null, null, null, null);
    private final LocalDateTime departure = LocalDateTime.of(2026, 11, 20, 10, 0);

    @BeforeEach
    void rules() {
        when(rules.findAllByOrderByMinHoursBeforeDesc()).thenReturn(List.of(
                new CancellationRule(48, BigDecimal.ZERO, true, "48 h or more"),
                new CancellationRule(12, new BigDecimal("25"), true, "12 to 48 h"),
                new CancellationRule(4, new BigDecimal("50"), true, "4 to 12 h"),
                new CancellationRule(0, new BigDecimal("100"), false, "under 4 h")));
    }

    private int refund(CoachClass cc, Quota quota, SeatStatus status, int fare, Duration before) {
        Train t = new Train("12901", "Test", TrainType.SUPERFAST, "1111111", false);
        Booking b = new Booking("1234567890", null, t, cc, quota);
        b.setDepartureAt(departure);
        Passenger p = new Passenger(1, "Asha", 30, Gender.FEMALE, BerthPreference.NONE, FoodChoice.NO_FOOD);
        p.setFare(BigDecimal.valueOf(fare));
        p.setCurrentStatus(status);
        b.addPassenger(p);
        return service.refundFor(b, p, departure.minus(before)).refund();
    }

    @ParameterizedTest(name = "Time left: {0}h {1}m -> Expected refund: Rs {2}")
    @CsvSource({
            "48,  1, 880",
            "48,  0, 880",
            "47, 59, 750",
            "12,  0, 750",
            "11, 59, 500",
            " 4,  0, 500",
            " 3, 59,   0"
    })
    @DisplayName("TC-CAN-05: Confirmed Sleeper Rs 1000 ticket refund boundaries across 48h, 12h, and 4h tiers")
    void confirmedSleeperBoundaries(int hours, int minutes, int expectedRefund) {
        Duration before = Duration.ofHours(hours).plusMinutes(minutes);
        assertThat(refund(CoachClass.SLEEPER, Quota.GENERAL, SeatStatus.CNF, 1000, before))
                .isEqualTo(expectedRefund);
    }

    @Test
    @DisplayName("TC-CAN-06: 3A cancellation includes 5% GST on the flat cancellation charge beyond 48 hours")
    void thirdAcCancellationIncludesGstOnFlatCharge() {
        // 3A flat charge is Rs 180 + 5% GST (Rs 9) = Rs 189 deduction; 1000 - 189 = Rs 811 refund
        Duration before = Duration.ofHours(50);
        assertThat(refund(CoachClass.THIRD_AC, Quota.GENERAL, SeatStatus.CNF, 1000, before))
                .isEqualTo(811);
    }

    @Test
    @DisplayName("TC-CAN-07: Flat charge takes precedence when percentage deduction is lower")
    void flatChargeAppliesWhenPercentageIsLower() {
        // SL fare Rs 300 at 24 hours (12 to 48h tier, 25% = Rs 75). Flat charge Rs 120 is higher; 300 - 120 = Rs 180
        Duration before = Duration.ofHours(24);
        assertThat(refund(CoachClass.SLEEPER, Quota.GENERAL, SeatStatus.CNF, 300, before))
                .isEqualTo(180);
    }

    @Test
    @DisplayName("TC-CAN-08: RAC traveller cancellation retains only Rs 60 clerkage charge")
    void racTravellerLosesOnlyClerkage() {
        // RAC traveller with Rs 1000 fare, cancelled 2 hours before departure (in the 0% refund tier for CNF)
        Duration before = Duration.ofHours(2);
        assertThat(refund(CoachClass.SLEEPER, Quota.GENERAL, SeatStatus.RAC, 1000, before))
                .isEqualTo(940);
    }

    @Test
    @DisplayName("TC-CAN-09: Waitlisted traveller cancellation retains only Rs 60 clerkage charge")
    void waitlistedTravellerLosesOnlyClerkage() {
        // Waitlisted traveller with Rs 1000 fare, cancelled 2 hours before departure
        Duration before = Duration.ofHours(2);
        assertThat(refund(CoachClass.SLEEPER, Quota.GENERAL, SeatStatus.WL, 1000, before))
                .isEqualTo(940);
    }

    @Test
    @DisplayName("TC-CAN-10: Confirmed Tatkal tickets receive zero refund regardless of time remaining")
    void confirmedTatkalReceivesNoRefund() {
        // Confirmed Tatkal ticket cancelled 3 days (72 hours) before departure
        Duration before = Duration.ofDays(3);
        assertThat(refund(CoachClass.SLEEPER, Quota.TATKAL, SeatStatus.CNF, 1000, before))
                .isEqualTo(0);
    }

    @Test
    @DisplayName("TC-CAN-11: Train already departed receives zero refund")
    void departedTrainReceivesNoRefund() {
        // Negative duration represents cancellation attempted after the scheduled departure time
        Duration afterDeparture = Duration.ofMinutes(-15);
        assertThat(refund(CoachClass.SLEEPER, Quota.GENERAL, SeatStatus.CNF, 1000, afterDeparture))
                .isEqualTo(0);
    }
}

