package com.railway.reservation.service;

import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.*;
import com.railway.reservation.service.rail.SeatLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Berth allocation, waiting lists and promotion (RRS-F-005, RRS-F-006, RRS-F-012).
 * A single sleeper coach: 72 berths, 58 general and 14 Tatkal, 14 RAC places and a waiting list of 14.
 */
class SeatLedgerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 1);

    private Train train;
    private TrainClass sleeper;
    private Map<Quota, ClassInventory> inventories;
    private final List<Passenger> all = new ArrayList<>();
    private long seq = 1000;

    @BeforeEach
    void setUp() {
        train = new Train("12901", "Test", TrainType.SUPERFAST, "1111111", false);
        sleeper = new TrainClass(CoachClass.SLEEPER, 1);
        train.addClass(sleeper);
        inventories = new EnumMap<>(Quota.class);
        inventories.put(Quota.GENERAL, new ClassInventory(sleeper, DATE, Quota.GENERAL));
        inventories.put(Quota.TATKAL, new ClassInventory(sleeper, DATE, Quota.TATKAL));
    }

    private SeatLedger ledger() {
        return new SeatLedger(sleeper, inventories, all);
    }

    private List<Passenger> book(SeatLedger ledger, int count, int from, int to, Quota quota, BerthPreference pref, int age, Gender g) {
        Booking b = new Booking("PNR" + seq, null, train, CoachClass.SLEEPER, quota);
        b.setSegment(from, "S" + from, to, "S" + to);
        List<Passenger> ps = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Passenger p = new Passenger(i + 1, "Traveller " + i, age, g, pref, null);
            b.addPassenger(p);
            ps.add(p);
        }
        ledger.allocate(ps, quota, from, to, false, false, seq);
        seq += count + 1;
        all.addAll(ps);
        return ps;
    }

    private List<Passenger> book(SeatLedger ledger, int count) {
        return book(ledger, count, 1, 4, Quota.GENERAL, BerthPreference.NONE, 30, Gender.MALE);
    }

    @Test
    @DisplayName("TC-SEAT-01: An empty coach offers all 58 general berths")
    void emptyCoachAvailability() {
        SeatLedger.Availability a = ledger().availability(Quota.GENERAL, 1, 4, false);
        assertThat(a.state()).isEqualTo(SeatLedger.State.AVAILABLE);
        assertThat(a.count()).isEqualTo(58);
        assertThat(sleeper.tatkalBerths()).isEqualTo(14);
    }

    @Test
    @DisplayName("TC-SEAT-02: A berth preference is honoured when such a berth is free")
    void honoursPreference() {
        Passenger p = book(ledger(), 1, 1, 4, Quota.GENERAL, BerthPreference.SIDE_UPPER, 30, Gender.MALE).getFirst();
        assertThat(p.getCurrentStatus()).isEqualTo(SeatStatus.CNF);
        assertThat(p.getBerthType()).isEqualTo(BerthType.SIDE_UPPER);
        assertThat(p.getCoach()).isEqualTo("S1");
    }

    @Test
    @DisplayName("TC-SEAT-03: Travellers aged 60 or more are given a lower berth when they state no preference")
    void seniorsGetLowerBerths() {
        Passenger p = book(ledger(), 1, 1, 4, Quota.GENERAL, BerthPreference.NONE, 66, Gender.MALE).getFirst();
        assertThat(p.getBerthType()).isEqualTo(BerthType.LOWER);
    }

    @Test
    @DisplayName("TC-SEAT-04: A group is seated together, close to the first traveller")
    void groupsSitTogether() {
        List<Passenger> group = book(ledger(), 4);
        int first = group.getFirst().getBerthIndex();
        assertThat(group).allSatisfy(p -> assertThat(Math.abs(p.getBerthIndex() - first)).isLessThan(8));
    }

    @Test
    @DisplayName("TC-SEAT-05: A berth freed at an intermediate station is sold again for the rest of the route")
    void segmentReuse() {
        SeatLedger ledger = ledger();
        book(ledger, 58, 1, 2, Quota.GENERAL, BerthPreference.NONE, 30, Gender.MALE);   // every general berth taken to stop 2
        assertThat(ledger.availability(Quota.GENERAL, 1, 3, false).state()).isNotEqualTo(SeatLedger.State.AVAILABLE);
        assertThat(ledger.availability(Quota.GENERAL, 2, 4, false).count()).isEqualTo(58);
        Passenger later = book(ledger, 1, 2, 4, Quota.GENERAL, BerthPreference.NONE, 30, Gender.MALE).getFirst();
        assertThat(later.getCurrentStatus()).isEqualTo(SeatStatus.CNF);
    }

    @Test
    @DisplayName("TC-SEAT-06: When berths run out travellers join RAC, then the waiting list, then the class regrets")
    void racThenWaitlistThenRegret() {
        SeatLedger ledger = ledger();
        book(ledger, 58);
        Passenger rac = book(ledger, 1).getFirst();
        assertThat(rac.getCurrentStatus()).isEqualTo(SeatStatus.RAC);
        assertThat(rac.getBookingNumber()).isEqualTo(1);
        book(ledger, 13);                                 // RAC full at 14
        Passenger wl = book(ledger, 1).getFirst();
        assertThat(wl.getCurrentStatus()).isEqualTo(SeatStatus.WL);
        assertThat(wl.getBookingNumber()).isEqualTo(1);
        book(ledger, 13);                                 // waiting list full at 14
        assertThat(ledger.availability(Quota.GENERAL, 1, 4, false).state()).isEqualTo(SeatLedger.State.REGRET);
        assertThatThrownBy(() -> book(ledger, 1)).isInstanceOf(ApiException.class).hasMessageContaining("waiting list");
    }

    @Test
    @DisplayName("TC-SEAT-07: 'Book only if confirmed' refuses a booking that would not be fully confirmed")
    void onlyIfConfirmed() {
        SeatLedger ledger = ledger();
        book(ledger, 56);
        Booking b = new Booking("X", null, train, CoachClass.SLEEPER, Quota.GENERAL);
        b.setSegment(1, "A", 4, "D");
        List<Passenger> three = List.of(new Passenger(1, "A", 30, Gender.MALE, null, null),
                new Passenger(2, "B", 30, Gender.MALE, null, null), new Passenger(3, "C", 30, Gender.MALE, null, null));
        three.forEach(b::addPassenger);
        assertThatThrownBy(() -> ledger.allocate(three, Quota.GENERAL, 1, 4, true, false, 1))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("NOT_ALL_CONFIRMED");
    }

    @Test
    @DisplayName("TC-SEAT-08: After chart preparation no RAC or waiting-list tickets are issued")
    void noQueueAfterChart() {
        SeatLedger ledger = ledger();
        book(ledger, 58);
        assertThat(ledger.availability(Quota.GENERAL, 1, 4, true).state()).isEqualTo(SeatLedger.State.REGRET);
    }

    @Test
    @DisplayName("TC-SEAT-09: A cancellation confirms the first RAC traveller and moves the first waitlisted one into RAC")
    void cancellationPromotesQueues() {
        SeatLedger ledger = ledger();
        List<Passenger> confirmed = book(ledger, 58);
        Passenger rac1 = book(ledger, 1).getFirst();
        book(ledger, 13);
        Passenger wl1 = book(ledger, 1).getFirst();
        Passenger wl2 = book(ledger, 1).getFirst();

        ledger.release(confirmed.getFirst());
        Set<Passenger> changed = ledger.promote();

        assertThat(rac1.getCurrentStatus()).isEqualTo(SeatStatus.CNF);
        assertThat(wl1.getCurrentStatus()).isEqualTo(SeatStatus.RAC);
        assertThat(ledger.currentNumber(wl1)).isEqualTo(14);
        assertThat(ledger.currentNumber(wl2)).isEqualTo(1);
        assertThat(changed).contains(rac1, wl1);
        assertThat(confirmed.getFirst().getCurrentStatus()).isEqualTo(SeatStatus.CAN);
    }

    @Test
    @DisplayName("TC-SEAT-10: Other channels' earlier RAC passengers clear before this system's")
    void externalQueueClearsFirst() {
        ClassInventory gn = inventories.get(Quota.GENERAL);
        gn.setExternalRac(2);
        SeatLedger ledger = ledger();
        List<Passenger> confirmed = book(ledger, 58);
        Passenger ours = book(ledger, 1).getFirst();
        assertThat(ledger.currentNumber(ours)).isEqualTo(3);

        ledger.release(confirmed.get(0));
        ledger.promote();
        assertThat(gn.getExternalRac()).isEqualTo(1);
        assertThat(ours.getCurrentStatus()).isEqualTo(SeatStatus.RAC);
        assertThat(ledger.currentNumber(ours)).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-SEAT-11: Tatkal and general quotas draw on separate berth pools")
    void quotasAreSeparate() {
        SeatLedger ledger = ledger();
        book(ledger, 58);
        Passenger tq = book(ledger, 1, 1, 4, Quota.TATKAL, BerthPreference.NONE, 30, Gender.MALE).getFirst();
        assertThat(tq.getCurrentStatus()).isEqualTo(SeatStatus.CNF);
        assertThat(tq.getBerthIndex()).isGreaterThanOrEqualTo(sleeper.generalBerths());
    }
}
