package com.railway.reservation.service.rail;

import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.SeatStatus;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.service.UserRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumSet;

/**
 * Background work a reservation system does on its own: releasing unpaid
 * holds when they lapse, cancelling fully waitlisted tickets (with a
 * refund) once a train's chart is prepared, and forgetting sign-ups whose
 * email was never confirmed.
 */
@Component
public class Housekeeping {

    private static final Logger log = LoggerFactory.getLogger(Housekeeping.class);

    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;
    private final InventoryService inventoryService;
    private final TransactionTemplate tx;
    private final UserRegistrationService registrationService;

    public Housekeeping(BookingRepository bookingRepository, PaymentService paymentService,
                        CancellationService cancellationService, InventoryService inventoryService,
                        TransactionTemplate tx, UserRegistrationService registrationService) {
        this.bookingRepository = bookingRepository;
        this.paymentService = paymentService;
        this.cancellationService = cancellationService;
        this.inventoryService = inventoryService;
        this.tx = tx;
        this.registrationService = registrationService;
    }

    @Scheduled(fixedDelayString = "${app.housekeeping.interval-ms:30000}", initialDelay = 10000)
    public void run() {
        expireHolds();
        cancelWaitlistedAfterChart();
        registrationService.purgeAbandoned();
    }

    public int expireHolds() {
        int n = 0;
        for (String pnr : bookingRepository.findExpiredHolds(inventoryService.now())) {
            try {
                Boolean done = tx.execute(status -> bookingRepository.findByPnrForUpdate(pnr)
                        .map(paymentService::closeLapsedHold).orElse(false));
                if (Boolean.TRUE.equals(done)) n++;
            } catch (RuntimeException e) {
                log.error("Could not close the lapsed hold pnr={}: {}", pnr, e.getMessage());
            }
        }
        return n;
    }

    public int cancelWaitlistedAfterChart() {
        int n = 0;
        var statuses = EnumSet.of(BookingStatus.BOOKED, BookingStatus.PARTIALLY_CANCELLED);
        for (String pnr : bookingRepository.findLiveDepartingFrom(statuses, inventoryService.today().minusDays(3),
                inventoryService.now())) {
            try {
                Boolean done = tx.execute(status -> bookingRepository.findByPnrForUpdate(pnr).map(b -> {
                    if (!b.getStatus().isLive() || !inventoryService.chartPrepared(b.getTrain(), b.getTrainStartDate())) {
                        return false;
                    }
                    boolean allWaitlisted = !b.activePassengers().isEmpty()
                            && b.activePassengers().stream().allMatch(p -> p.getCurrentStatus() == SeatStatus.WL);
                    if (!allWaitlisted) return false;
                    cancellationService.cancelUnconfirmedAfterChart(b);
                    log.info("Waitlisted ticket cancelled after chart pnr={}", b.getPnr());
                    return true;
                }).orElse(false));
                if (Boolean.TRUE.equals(done)) n++;
            } catch (RuntimeException e) {
                // A refund Razorpay could not start leaves the ticket as it was; the next run tries again
                log.error("Could not cancel waitlisted ticket pnr={}: {}", pnr, e.getMessage());
            }
        }
        return n;
    }

    /** Exposed for tests that drive the clock. */
    public Booking reload(String pnr) {
        return bookingRepository.findByPnr(pnr).orElseThrow();
    }
}
