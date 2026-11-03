package com.railway.reservation.service.rail;

import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.repository.TrainClassRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;

/** Locks a class and returns seats to it, moving RAC and waiting lists forward. */
@Component
public class SeatOperations {

    private final TrainClassRepository trainClassRepository;
    private final InventoryService inventoryService;

    public SeatOperations(TrainClassRepository trainClassRepository, InventoryService inventoryService) {
        this.trainClassRepository = trainClassRepository;
        this.inventoryService = inventoryService;
    }

    public TrainClass lock(TrainClass tc) {
        return trainClassRepository.findByIdForUpdate(tc.getId())
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND", "That class is no longer on this train."));
    }

    public SeatLedger lockedLedger(Booking b) {
        TrainClass tc = b.getTrain().classOf(b.getCoachClass())
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND", "That class is no longer on this train."));
        return inventoryService.writeLedger(lock(tc), b.getTrainStartDate());
    }

    /** Releases the travellers' places and promotes whoever is waiting. */
    public void release(Booking b, Collection<Passenger> travellers) {
        SeatLedger ledger = lockedLedger(b);
        for (Passenger p : travellers) {
            ledger.release(p);
        }
        ledger.promote();
    }
}
