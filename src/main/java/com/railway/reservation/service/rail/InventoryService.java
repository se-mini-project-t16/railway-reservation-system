package com.railway.reservation.service.rail;

import com.railway.reservation.model.*;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.ClassInventoryRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link SeatLedger}s and owns the timing rules around them: the
 * 60-day booking window, Tatkal opening (10:00 for AC, 11:00 for non-AC, the
 * day before the train leaves its origin) and chart preparation four hours
 * before the train leaves its origin.
 */
@Service
public class InventoryService {

    public static final int ADVANCE_BOOKING_DAYS = 60;
    public static final int CHART_HOURS_BEFORE_DEPARTURE = 4;
    public static final EnumSet<BookingStatus> HOLDING = EnumSet.of(
            BookingStatus.PAYMENT_PENDING, BookingStatus.BOOKED, BookingStatus.PARTIALLY_CANCELLED);

    private final ClassInventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final DemandSimulator demandSimulator;
    private final Clock clock;

    public InventoryService(ClassInventoryRepository inventoryRepository, BookingRepository bookingRepository,
                            DemandSimulator demandSimulator, Clock clock) {
        this.inventoryRepository = inventoryRepository;
        this.bookingRepository = bookingRepository;
        this.demandSimulator = demandSimulator;
        this.clock = clock;
    }

    public LocalDateTime now() { return LocalDateTime.now(clock); }
    public LocalDate today() { return LocalDate.now(clock); }

    public LocalDateTime tatkalOpensAt(CoachClass cc, LocalDate trainStartDate) {
        return trainStartDate.minusDays(1).atTime(cc.airConditioned() ? LocalTime.of(10, 0) : LocalTime.of(11, 0));
    }

    public boolean tatkalOpen(CoachClass cc, LocalDate trainStartDate) {
        return !now().isBefore(tatkalOpensAt(cc, trainStartDate));
    }

    public LocalDateTime chartPreparedAt(Train train, LocalDate trainStartDate) {
        return trainStartDate.atStartOfDay().plusMinutes(train.origin().getDepartureMinute())
                .minusHours(CHART_HOURS_BEFORE_DEPARTURE);
    }

    public boolean chartPrepared(Train train, LocalDate trainStartDate) {
        return !now().isBefore(chartPreparedAt(train, trainStartDate));
    }

    /**
     * Ledger for reading availability. Inventory not yet stored is generated on
     * the fly (the simulation is deterministic) and nothing is written.
     */
    public SeatLedger readLedger(TrainClass tc, LocalDate trainStartDate) {
        return build(tc, trainStartDate, false);
    }

    /** Ledger for changing seats. The caller must hold the class lock; generated inventory is stored. */
    public SeatLedger writeLedger(TrainClass tc, LocalDate trainStartDate) {
        return build(tc, trainStartDate, true);
    }

    private SeatLedger build(TrainClass tc, LocalDate start, boolean persist) {
        Map<Quota, ClassInventory> inventories = new EnumMap<>(Quota.class);
        inventories.put(Quota.GENERAL, inventory(tc, start, Quota.GENERAL, persist));
        if (tc.getCoachClass().supportsTatkal()) {
            inventories.put(Quota.TATKAL, tatkalOpen(tc.getCoachClass(), start)
                    ? inventory(tc, start, Quota.TATKAL, persist)
                    : new ClassInventory(tc, start, Quota.TATKAL));
        }
        List<Passenger> passengers = bookingRepository
                .findForInventory(tc.getTrain().getId(), tc.getCoachClass(), start, HOLDING)
                .stream().flatMap(b -> b.getPassengers().stream()).toList();
        return new SeatLedger(tc, inventories, passengers);
    }

    private ClassInventory inventory(TrainClass tc, LocalDate start, Quota quota, boolean persist) {
        return inventoryRepository.findByTrainClassAndTrainStartDateAndQuota(tc, start, quota).orElseGet(() -> {
            ClassInventory generated = demandSimulator.generate(tc, start, quota, today());
            return persist ? inventoryRepository.save(generated) : generated;
        });
    }
}
