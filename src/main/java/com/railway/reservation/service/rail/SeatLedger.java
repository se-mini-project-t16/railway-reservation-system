package com.railway.reservation.service.rail;

import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.*;

import java.util.*;

/**
 * Seats for one class of one train on one departure, across both quotas.
 *
 * <p>A berth is free for a journey when no other channel holds it and no
 * confirmed traveller on this system overlaps the journey's stretch of the
 * route, so a berth vacated at Katpadi can be sold again from Katpadi.
 * Travellers who cannot be confirmed join RAC (classes that have it) and then
 * the waiting list. Other channels' RAC and WL queues were formed earlier, so
 * they clear first.</p>
 *
 * <p>Callers must hold the class lock (see {@code TrainClassRepository#findByIdForUpdate}).</p>
 */
public class SeatLedger {

    private final TrainClass trainClass;
    private final CoachClass cc;
    private final Map<Quota, ClassInventory> inventories;
    private final Map<Quota, BitSet> external = new EnumMap<>(Quota.class);
    private final List<Passenger> active = new ArrayList<>();
    private final Map<Integer, List<Passenger>> byBerth = new HashMap<>();

    public SeatLedger(TrainClass trainClass, Map<Quota, ClassInventory> inventories, Collection<Passenger> activePassengers) {
        this.trainClass = trainClass;
        this.cc = trainClass.getCoachClass();
        this.inventories = inventories;
        inventories.forEach((q, inv) -> external.put(q, inv.externalSet()));
        for (Passenger p : activePassengers) {
            if (p.getCurrentStatus() == SeatStatus.CAN) continue;
            active.add(p);
            if (p.getCurrentStatus() == SeatStatus.CNF && p.getBerthIndex() != null) {
                byBerth.computeIfAbsent(p.getBerthIndex(), k -> new ArrayList<>()).add(p);
            }
        }
    }

    // ---------------------------------------------------------------- availability

    public enum State { AVAILABLE, RAC, WAITLIST, REGRET }

    public record Availability(State state, int count) {
        public String label() {
            return switch (state) {
                case AVAILABLE -> "AVL " + count;
                case RAC -> "RAC " + count;
                case WAITLIST -> "WL " + count;
                case REGRET -> "REGRET";
            };
        }
    }

    public Availability availability(Quota quota, int fromSeq, int toSeq, boolean chartPrepared) {
        int free = countFree(quota, fromSeq, toSeq);
        if (free > 0) return new Availability(State.AVAILABLE, free);
        if (chartPrepared) return new Availability(State.REGRET, 0);
        int rac = racUsed(quota);
        if (rac < trainClass.racLimit(quota)) return new Availability(State.RAC, rac + 1);
        int wl = wlUsed(quota);
        if (wl < trainClass.waitlistLimit(quota)) return new Availability(State.WAITLIST, wl + 1);
        return new Availability(State.REGRET, 0);
    }

    public int countFree(Quota quota, int fromSeq, int toSeq) {
        int n = 0;
        for (int i = poolStart(quota); i < poolEnd(quota); i++) {
            if (isFree(i, fromSeq, toSeq)) n++;
        }
        return n;
    }

    /** Berths held for the whole run by other channels, across both quotas. */
    public boolean isExternal(int index) {
        for (BitSet set : external.values()) if (set.get(index)) return true;
        return false;
    }

    public List<Passenger> occupantsOf(int index) {
        return byBerth.getOrDefault(index, List.of());
    }

    private boolean isFree(int index, int fromSeq, int toSeq) {
        if (isExternal(index)) return false;
        for (Passenger p : byBerth.getOrDefault(index, List.of())) {
            Booking b = p.getBooking();
            if (b.getFromSeq() < toSeq && fromSeq < b.getToSeq()) return false;
        }
        return true;
    }

    private boolean isFreeWholeRun(int index) {
        return !isExternal(index) && byBerth.getOrDefault(index, List.of()).isEmpty();
    }

    int poolStart(Quota quota) { return quota == Quota.TATKAL ? trainClass.generalBerths() : 0; }
    int poolEnd(Quota quota) { return quota == Quota.TATKAL ? trainClass.capacity() : trainClass.generalBerths(); }

    private ClassInventory inv(Quota quota) {
        ClassInventory inv = inventories.get(quota);
        if (inv == null) throw new IllegalStateException("Inventory for " + quota + " not loaded");
        return inv;
    }

    private List<Passenger> queue(Quota quota, SeatStatus status) {
        return active.stream()
                .filter(p -> p.getCurrentStatus() == status && p.getBooking().getQuota() == quota)
                .sorted(Comparator.comparingLong(Passenger::getQueueSeq))
                .toList();
    }

    int racUsed(Quota quota) { return inv(quota).getExternalRac() + queue(quota, SeatStatus.RAC).size(); }
    int wlUsed(Quota quota) { return inv(quota).getExternalWl() + queue(quota, SeatStatus.WL).size(); }

    /** Position in the RAC or waiting list right now, counting other channels' earlier queue. */
    public Integer currentNumber(Passenger p) {
        SeatStatus s = p.getCurrentStatus();
        if (s != SeatStatus.RAC && s != SeatStatus.WL) return null;
        Quota quota = p.getBooking().getQuota();
        int ahead = s == SeatStatus.RAC ? inv(quota).getExternalRac() : inv(quota).getExternalWl();
        List<Passenger> q = queue(quota, s);
        return ahead + q.indexOf(p) + 1;
    }

    // ---------------------------------------------------------------- allocation

    /**
     * Gives each traveller a berth, an RAC place or a waiting-list place, in
     * that order, keeping a group in one coach where it can.
     *
     * @throws ApiException when the waiting list is full, or when the booking
     *                      asked for confirmed berths only and they are not all free
     */
    public void allocate(List<Passenger> travellers, Quota quota, int fromSeq, int toSeq,
                         boolean onlyIfConfirmed, boolean chartPrepared, long queueSeqStart) {
        int free = countFree(quota, fromSeq, toSeq);
        if (onlyIfConfirmed && free < travellers.size()) {
            throw ApiException.conflict("NOT_ALL_CONFIRMED",
                    "Confirmed berths are not available for all travellers, so nothing was booked.");
        }
        int need = travellers.size() - free;
        if (need > 0) {
            if (chartPrepared) {
                throw ApiException.conflict("REGRET", "The chart for this train is prepared and no berths are left.");
            }
            int racRoom = Math.max(0, trainClass.racLimit(quota) - racUsed(quota));
            int wlRoom = Math.max(0, trainClass.waitlistLimit(quota) - wlUsed(quota));
            if (need > racRoom + wlRoom) {
                throw ApiException.conflict("REGRET",
                        "The waiting list for this class is full. Try another class, quota or date.");
            }
        }

        Integer groupIndex = null;
        long seq = queueSeqStart;
        for (Passenger p : travellers) {
            p.setQueueSeq(seq++);
            int index = bestBerth(p, quota, fromSeq, toSeq, groupIndex);
            if (index >= 0) {
                confirm(p, index);
                if (groupIndex == null) groupIndex = index;
                p.freezeBookingStatus(null);
            } else if (racUsed(quota) < trainClass.racLimit(quota)) {
                p.queue(SeatStatus.RAC);
                active.add(p);
                p.freezeBookingStatus(currentNumber(p));
            } else {
                p.queue(SeatStatus.WL);
                active.add(p);
                p.freezeBookingStatus(currentNumber(p));
            }
        }
    }

    private int bestBerth(Passenger p, Quota quota, int fromSeq, int toSeq, Integer groupIndex) {
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        int perCoach = cc.berthsPerCoach();
        for (int i = poolStart(quota); i < poolEnd(quota); i++) {
            if (!isFree(i, fromSeq, toSeq)) continue;
            BerthType type = cc.berthTypeOf(i % perCoach + 1);
            double score = 0;
            if (p.getBerthPreference().isSatisfiedBy(type)) {
                score += 100;
            } else if (p.getBerthPreference() == BerthPreference.NONE && p.prefersLowerByRule() && type == BerthType.LOWER) {
                score += 60;
            }
            if (groupIndex != null) {
                if (groupIndex / perCoach == i / perCoach) score += 40;
                score -= Math.abs(groupIndex - i) / 10.0;
            }
            score -= i / 100000.0;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private void confirm(Passenger p, int index) {
        int perCoach = cc.berthsPerCoach();
        int coachNo = index / perCoach + 1;
        int berthNo = index % perCoach + 1;
        boolean queued = p.getCurrentStatus() == SeatStatus.RAC || p.getCurrentStatus() == SeatStatus.WL;
        p.confirm(index, DemandSimulator.coachLabel(cc, coachNo), berthNo, cc.berthTypeOf(berthNo));
        byBerth.computeIfAbsent(index, k -> new ArrayList<>()).add(p);
        if (!queued) active.add(p);
    }

    // ---------------------------------------------------------------- release and promotion

    /** Takes a traveller out of the ledger (cancelled, or the hold lapsed). */
    public void release(Passenger p) {
        if (p.getBerthIndex() != null) {
            List<Passenger> list = byBerth.get(p.getBerthIndex());
            if (list != null) list.remove(p);
        }
        active.remove(p);
        p.queue(SeatStatus.CAN);
    }

    /**
     * Moves queues forward after berths free up: other channels' RAC first,
     * then this system's RAC, then the waiting list into RAC (or straight to a
     * berth in classes without RAC). Returns this system's travellers whose
     * status changed.
     */
    public Set<Passenger> promote() {
        Set<Passenger> changed = new LinkedHashSet<>();
        for (Quota quota : inventories.keySet()) {
            boolean progress = true;
            while (progress) {
                progress = false;
                ClassInventory inv = inv(quota);
                BitSet ext = external.get(quota);

                while (inv.getExternalRac() > 0) {
                    int index = firstFreeWholeRun(quota);
                    if (index < 0) break;
                    ext.set(index);
                    inv.setExternalRac(inv.getExternalRac() - 1);
                    progress = true;
                }
                for (Passenger p : queue(quota, SeatStatus.RAC)) {
                    Booking b = p.getBooking();
                    int index = bestBerth(p, quota, b.getFromSeq(), b.getToSeq(), null);
                    if (index < 0) continue;
                    confirm(p, index);
                    changed.add(p);
                    progress = true;
                }
                progress |= fillRacFromWaitlist(quota, changed);

                if (!cc.supportsRac() || trainClass.racLimit(quota) == 0) {
                    while (inv.getExternalWl() > 0) {
                        int index = firstFreeWholeRun(quota);
                        if (index < 0) break;
                        ext.set(index);
                        inv.setExternalWl(inv.getExternalWl() - 1);
                        progress = true;
                    }
                    if (inv.getExternalWl() == 0) {
                        for (Passenger p : queue(quota, SeatStatus.WL)) {
                            Booking b = p.getBooking();
                            int index = bestBerth(p, quota, b.getFromSeq(), b.getToSeq(), null);
                            if (index < 0) continue;
                            confirm(p, index);
                            changed.add(p);
                            progress = true;
                        }
                    }
                }
            }
            inv(quota).setExternalSet(external.get(quota));
        }
        return changed;
    }

    private boolean fillRacFromWaitlist(Quota quota, Set<Passenger> changed) {
        boolean moved = false;
        ClassInventory inv = inv(quota);
        while (racUsed(quota) < trainClass.racLimit(quota)) {
            if (inv.getExternalWl() > 0) {
                inv.setExternalWl(inv.getExternalWl() - 1);
                inv.setExternalRac(inv.getExternalRac() + 1);
            } else {
                List<Passenger> wl = queue(quota, SeatStatus.WL);
                if (wl.isEmpty()) break;
                Passenger p = wl.getFirst();
                p.setCurrentStatus(SeatStatus.RAC);
                changed.add(p);
            }
            moved = true;
        }
        return moved;
    }

    private int firstFreeWholeRun(Quota quota) {
        for (int i = poolStart(quota); i < poolEnd(quota); i++) {
            if (isFreeWholeRun(i)) return i;
        }
        return -1;
    }

    public TrainClass getTrainClass() { return trainClass; }
    public int externalRac() { return inventories.values().stream().mapToInt(ClassInventory::getExternalRac).sum(); }
    public int externalWl() { return inventories.values().stream().mapToInt(ClassInventory::getExternalWl).sum(); }
    public int externalCount() { return external.values().stream().mapToInt(BitSet::cardinality).sum(); }
    public int freeWholeRun() {
        int n = 0;
        for (int i = 0; i < trainClass.capacity(); i++) if (isFreeWholeRun(i)) n++;
        return n;
    }
    public List<Passenger> activePassengers() { return Collections.unmodifiableList(active); }
}
