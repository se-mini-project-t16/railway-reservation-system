package com.railway.reservation.service;

import com.railway.reservation.model.CoachClass;
import com.railway.reservation.model.Quota;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainType;
import com.railway.reservation.service.rail.FareCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Fare rules behind every price shown and charged (RRS-F-006, RRS-F-010). */
class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator();
    private final Train superfast = new Train("12657", "Test Mail", TrainType.MAIL_EXPRESS, "1111111", false);
    private final Train ordinary = new Train("16526", "Test Express", TrainType.MAIL_EXPRESS, "1111111", false);
    private final Train shatabdi = new Train("12007", "Test Shatabdi", TrainType.SHATABDI, "1111111", false);

    @Test
    @DisplayName("TC-FARE-01: Sleeper fare is distance x rate plus reservation and superfast, with no GST")
    void sleeperFareComponents() {
        FareCalculator.Fare f = calculator.perPassenger(superfast, CoachClass.SLEEPER, 358, Quota.GENERAL);
        assertThat(f.base()).isEqualTo(186);          // 358 km x 0.52
        assertThat(f.reservation()).isEqualTo(20);
        assertThat(f.superfast()).isEqualTo(30);
        assertThat(f.gst()).isZero();
        assertThat(f.total()).isEqualTo(236);
    }

    @Test
    @DisplayName("TC-FARE-02: AC classes pay 5% GST on the fare before tax")
    void acClassesPayGst() {
        FareCalculator.Fare f = calculator.perPassenger(superfast, CoachClass.THIRD_AC, 358, Quota.GENERAL);
        int beforeTax = f.base() + f.reservation() + f.superfast();
        assertThat(f.gst()).isEqualTo(Math.round(beforeTax * 0.05f));
        assertThat(f.total()).isEqualTo(beforeTax + f.gst());
    }

    @Test
    @DisplayName("TC-FARE-03: Short journeys are charged the class's minimum distance")
    void minimumChargeableDistance() {
        FareCalculator.Fare short1 = calculator.perPassenger(superfast, CoachClass.SLEEPER, 50, Quota.GENERAL);
        FareCalculator.Fare atMin = calculator.perPassenger(superfast, CoachClass.SLEEPER, 200, Quota.GENERAL);
        assertThat(short1.base()).isEqualTo(atMin.base());
    }

    @Test
    @DisplayName("TC-FARE-04: Long journeys taper: the second 1000 km costs less per km than the first")
    void telescopicRates() {
        int first = calculator.perPassenger(ordinary, CoachClass.SLEEPER, 1000, Quota.GENERAL).base();
        int both = calculator.perPassenger(ordinary, CoachClass.SLEEPER, 2000, Quota.GENERAL).base();
        assertThat(both - first).isLessThan(first);
    }

    @Test
    @DisplayName("TC-FARE-05: Non-superfast express trains carry no superfast charge")
    void noSuperfastChargeOnOrdinaryExpress() {
        assertThat(calculator.perPassenger(ordinary, CoachClass.SLEEPER, 500, Quota.GENERAL).superfast()).isZero();
    }

    @Test
    @DisplayName("TC-FARE-06: Tatkal charge is a percentage of base fare, held between the class minimum and maximum")
    void tatkalChargeClamped() {
        int shortTq = calculator.perPassenger(superfast, CoachClass.SLEEPER, 200, Quota.TATKAL).tatkal();
        int longTq = calculator.perPassenger(superfast, CoachClass.SLEEPER, 2400, Quota.TATKAL).tatkal();
        assertThat(shortTq).isEqualTo(CoachClass.SLEEPER.tatkalMin());
        assertThat(longTq).isEqualTo(CoachClass.SLEEPER.tatkalMax());
        assertThat(calculator.perPassenger(superfast, CoachClass.SLEEPER, 200, Quota.GENERAL).tatkal()).isZero();
    }

    @Test
    @DisplayName("TC-FARE-07: Premium trains include catering and a higher base rate")
    void premiumTrainsIncludeCatering() {
        FareCalculator.Fare premium = calculator.perPassenger(shatabdi, CoachClass.CHAIR_CAR, 358, Quota.GENERAL);
        FareCalculator.Fare plain = calculator.perPassenger(superfast, CoachClass.CHAIR_CAR, 358, Quota.GENERAL);
        assertThat(premium.catering()).isEqualTo(CoachClass.CHAIR_CAR.cateringCharge());
        assertThat(premium.base()).isGreaterThan(plain.base());
        assertThat(plain.catering()).isZero();
    }

    @Test
    @DisplayName("TC-FARE-08: A booking's total is the per-traveller fare times the travellers")
    void timesMultipliesEveryComponent() {
        FareCalculator.Fare one = calculator.perPassenger(superfast, CoachClass.SECOND_AC, 700, Quota.GENERAL);
        assertThat(one.times(3).total()).isEqualTo(one.total() * 3);
    }
}
