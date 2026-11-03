package com.railway.reservation.service.rail;

import com.railway.reservation.dto.BookingDtos.*;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.SavedTravellerRepository;
import com.railway.reservation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Booking a journey (RRS-F-006, RRS-F-007), the passenger's own bookings
 * (RRS-F-014) and the public PNR enquiry (RRS-F-011).
 *
 * <p>Seats are allocated when the booking is created and held for
 * {@link #HOLD_MINUTES} minutes while the passenger pays.</p>
 */
@Service
public class BookingService {

    public static final int HOLD_MINUTES = 10;
    public static final int MAX_GENERAL = 6;
    public static final int MAX_TATKAL = 4;
    public static final int MAX_SAVED_TRAVELLERS = 20;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SavedTravellerRepository travellerRepository;
    private final TimetableService timetableService;
    private final InventoryService inventoryService;
    private final SeatOperations seatOperations;
    private final FareCalculator fareCalculator;
    private final CancellationService cancellationService;
    private final BookingMapper mapper;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository,
                          SavedTravellerRepository travellerRepository, TimetableService timetableService,
                          InventoryService inventoryService, SeatOperations seatOperations,
                          FareCalculator fareCalculator, CancellationService cancellationService, BookingMapper mapper) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.travellerRepository = travellerRepository;
        this.timetableService = timetableService;
        this.inventoryService = inventoryService;
        this.seatOperations = seatOperations;
        this.fareCalculator = fareCalculator;
        this.cancellationService = cancellationService;
        this.mapper = mapper;
    }

    /** What a train, class and journey resolves to, after every rule that does not depend on seats. */
    private record Plan(Train train, TrainClass trainClass, Journey journey, Quota quota) {}

    private Plan plan(String trainNumber, String fromCode, String toCode, LocalDate date, String classCode,
                      String quotaCode, int travellers) {
        Train train = timetableService.findTrain(trainNumber);
        if (!train.isActive()) {
            throw ApiException.conflict("TRAIN_INACTIVE", train.getNumber() + " is not taking reservations.");
        }
        CoachClass cc = CoachClass.fromCode(classCode)
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_CLASS", "Choose a valid class."));
        TrainClass tc = train.classOf(cc).orElseThrow(() ->
                ApiException.badRequest("CLASS_NOT_ON_TRAIN", train.getNumber() + " has no " + cc.label() + " coaches."));
        TrainStop from = train.stopFor(fromCode).orElseThrow(() ->
                ApiException.badRequest("STOP_NOT_ON_ROUTE", train.getNumber() + " does not stop at " + fromCode + "."));
        TrainStop to = train.stopFor(toCode).orElseThrow(() ->
                ApiException.badRequest("STOP_NOT_ON_ROUTE", train.getNumber() + " does not stop at " + toCode + "."));
        Journey journey = Journey.of(train, from, to, date);
        if (journey == null) {
            throw ApiException.badRequest("WRONG_DIRECTION", "This train reaches " + toCode + " before " + fromCode + ".");
        }
        if (!journey.runs()) {
            throw ApiException.badRequest("NOT_RUNNING", train.getNumber() + " does not leave " + from.getStation().getName()
                    + " on " + date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + "s.");
        }
        LocalDateTime now = inventoryService.now();
        if (!journey.departureAt().isAfter(now)) {
            throw ApiException.conflict("DEPARTED", "This train has already left " + from.getStation().getName() + ".");
        }
        if (date.isAfter(inventoryService.today().plusDays(InventoryService.ADVANCE_BOOKING_DAYS))) {
            throw ApiException.badRequest("DATE_TOO_FAR", "Reservations open " + InventoryService.ADVANCE_BOOKING_DAYS + " days ahead.");
        }
        Quota quota = Quota.fromCode(quotaCode);
        if (quota == Quota.TATKAL) {
            if (!cc.supportsTatkal()) {
                throw ApiException.badRequest("NO_TATKAL", cc.label() + " has no Tatkal quota.");
            }
            if (!inventoryService.tatkalOpen(cc, journey.trainStartDate())) {
                LocalDateTime opens = inventoryService.tatkalOpensAt(cc, journey.trainStartDate());
                throw ApiException.conflict("TATKAL_NOT_OPEN", "Tatkal booking for this train opens at "
                        + opens.toLocalTime() + " on " + opens.toLocalDate() + ".");
            }
            if (travellers > MAX_TATKAL) {
                throw ApiException.badRequest("TOO_MANY_TRAVELLERS", "A Tatkal ticket can carry at most " + MAX_TATKAL + " travellers.");
            }
        }
        if (travellers > MAX_GENERAL) {
            throw ApiException.badRequest("TOO_MANY_TRAVELLERS", "A ticket can carry at most " + MAX_GENERAL + " travellers.");
        }
        return new Plan(train, tc, journey, quota);
    }

    // ------------------------------------------------------------------ quote

    @Transactional(readOnly = true)
    public Quote quote(QuoteRequest r) {
        Plan p = plan(r.trainNumber(), r.from(), r.to(), r.date(), r.classCode(), r.quota(), r.passengers());
        FareCalculator.Fare fare = fareCalculator.perPassenger(p.train(), p.trainClass().getCoachClass(),
                p.journey().distanceKm(), p.quota());
        CoachClass cc = p.trainClass().getCoachClass();
        List<String> prefs = Arrays.stream(BerthPreference.values()).filter(b -> b.appliesTo(cc)).map(Enum::name).toList();
        return new Quote(BookingMapper.trainRef(p.train()), endPoint(p.journey(), true), endPoint(p.journey(), false),
                cc.code(), cc.label(), p.quota().code(), p.journey().distanceKm(), p.journey().durationMinutes(),
                TimetableService.toDto(fare), TimetableService.toDto(fare.times(r.passengers())), r.passengers(),
                timetableService.classAvailability(p.journey(), p.trainClass(), p.quota()),
                p.train().isPantry() || p.train().getType().premium(),
                p.quota() == Quota.TATKAL ? MAX_TATKAL : MAX_GENERAL, prefs,
                cc.supportsTatkal() ? inventoryService.tatkalOpensAt(cc, p.journey().trainStartDate()) : null);
    }

    private com.railway.reservation.dto.TrainDtos.EndPoint endPoint(Journey j, boolean from) {
        TrainStop s = from ? j.from() : j.to();
        LocalDateTime at = from ? j.departureAt() : j.arrivalAt();
        Integer day = from ? s.getDepartureDay() : s.getArrivalDay();
        return new com.railway.reservation.dto.TrainDtos.EndPoint(s.getStation().getCode(), s.getStation().getName(),
                s.getStation().getCity(), at.toLocalTime().toString(), at.toLocalDate(), day == null ? 1 : day);
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public BookingView create(Long userId, BookingRequest r) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Sign in again to continue."));
        Plan p = plan(r.trainNumber(), r.from(), r.to(), r.date(), r.classCode(), r.quota(), r.passengers().size());
        CoachClass cc = p.trainClass().getCoachClass();
        boolean meals = p.train().isPantry() || p.train().getType().premium();

        Booking booking = new Booking(newPnr(), user, p.train(), cc, p.quota());
        Journey j = p.journey();
        booking.setSegment(j.from().getSeq(), j.from().getStation().getCode(), j.to().getSeq(), j.to().getStation().getCode());
        booking.setJourneyDate(j.boardingDate());
        booking.setTrainStartDate(j.trainStartDate());
        booking.setDepartureAt(j.departureAt());
        booking.setArrivalAt(j.arrivalAt());
        booking.setDistanceKm(j.distanceKm());
        booking.setContactMobile(r.mobile());
        booking.setOnlyIfConfirmed(r.confirmedOnly());
        LocalDateTime now = inventoryService.now();
        booking.setCreatedAt(now);
        booking.setPaymentDueAt(now.plusMinutes(HOLD_MINUTES));

        List<Passenger> travellers = new ArrayList<>();
        int serial = 1;
        for (PassengerInput in : r.passengers()) {
            BerthPreference pref = in.berthPreference() == null || !in.berthPreference().appliesTo(cc)
                    ? BerthPreference.NONE : in.berthPreference();
            FoodChoice food = meals ? (in.food() == null ? FoodChoice.NO_FOOD : in.food()) : null;
            Passenger passenger = new Passenger(serial++, tidyName(in.name()), in.age(), in.gender(), pref, food);
            booking.addPassenger(passenger);
            travellers.add(passenger);
        }

        FareCalculator.Fare fare = fareCalculator.perPassenger(p.train(), cc, j.distanceKm(), p.quota());
        travellers.forEach(t -> t.setFare(fare.totalAmount()));
        FareCalculator.Fare total = fare.times(travellers.size());
        booking.setBaseFare(BigDecimal.valueOf(total.base()));
        booking.setReservationCharge(BigDecimal.valueOf(total.reservation()));
        booking.setSuperfastCharge(BigDecimal.valueOf(total.superfast()));
        booking.setTatkalCharge(BigDecimal.valueOf(total.tatkal()));
        booking.setCateringCharge(BigDecimal.valueOf(total.catering()));
        booking.setGst(BigDecimal.valueOf(total.gst()));
        booking.setTotalFare(BigDecimal.valueOf(total.total()));

        SeatLedger ledger = inventoryService.writeLedger(seatOperations.lock(p.trainClass()), j.trainStartDate());
        ledger.allocate(travellers, p.quota(), j.from().getSeq(), j.to().getSeq(), r.confirmedOnly(),
                inventoryService.chartPrepared(p.train(), j.trainStartDate()),
                inventoryService.now().atZone(com.railway.reservation.config.TimeConfig.IST).toInstant().toEpochMilli() * 10);

        Booking saved = bookingRepository.save(booking);
        if (r.rememberTravellers()) saveTravellers(user, r.passengers());
        return mapper.toView(saved);
    }

    private void saveTravellers(User user, List<PassengerInput> inputs) {
        List<SavedTraveller> existing = travellerRepository.findByUserIdOrderByNameAsc(user.getId());
        for (PassengerInput in : inputs) {
            String name = tidyName(in.name());
            Optional<SavedTraveller> match = existing.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();
            if (match.isPresent()) {
                match.get().update(name, in.age(), in.gender(), in.berthPreference(), in.food());
            } else if (existing.size() < MAX_SAVED_TRAVELLERS) {
                SavedTraveller t = travellerRepository.save(
                        new SavedTraveller(user, name, in.age(), in.gender(), in.berthPreference(), in.food()));
                existing = new ArrayList<>(existing);
                existing.add(t);
            }
        }
    }

    static String tidyName(String raw) {
        return raw.trim().replaceAll("\\s+", " ");
    }

    private String newPnr() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String pnr = (2 + RANDOM.nextInt(7)) + String.format("%09d", RANDOM.nextInt(1_000_000_000));
            if (!bookingRepository.existsByPnr(pnr)) return pnr;
        }
        throw new IllegalStateException("Could not allocate a unique PNR");
    }

    // ------------------------------------------------------------------ reads

    public Booking findOwned(String pnr, Long userId, boolean admin) {
        Booking b = bookingRepository.findByPnr(normalisePnr(pnr))
                .orElseThrow(() -> ApiException.notFound("PNR_NOT_FOUND", "No booking found for PNR " + pnr + "."));
        if (!admin && !b.getUser().getId().equals(userId)) {
            // Same answer as a missing PNR, so other people's PNRs cannot be confirmed by probing.
            throw ApiException.notFound("PNR_NOT_FOUND", "No booking found for PNR " + pnr + ".");
        }
        return b;
    }

    public Booking lockOwned(String pnr, Long userId, boolean admin) {
        Booking b = bookingRepository.findByPnrForUpdate(normalisePnr(pnr))
                .orElseThrow(() -> ApiException.notFound("PNR_NOT_FOUND", "No booking found for PNR " + pnr + "."));
        if (!admin && !b.getUser().getId().equals(userId)) {
            throw ApiException.notFound("PNR_NOT_FOUND", "No booking found for PNR " + pnr + ".");
        }
        return b;
    }

    static String normalisePnr(String pnr) {
        String p = pnr == null ? "" : pnr.replaceAll("\\D", "");
        if (p.length() != 10) {
            throw ApiException.badRequest("INVALID_PNR", "A PNR is 10 digits.");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public BookingView view(String pnr, Long userId, boolean admin) {
        return mapper.toView(findOwned(pnr, userId, admin));
    }

    @Transactional(readOnly = true)
    public PnrStatus pnrStatus(String pnr) {
        Booking b = bookingRepository.findByPnr(normalisePnr(pnr))
                .orElseThrow(() -> ApiException.notFound("PNR_NOT_FOUND", "No booking found for PNR " + pnr + "."));
        if (b.getStatus() == BookingStatus.PAYMENT_PENDING || b.getStatus() == BookingStatus.PAYMENT_FAILED
                || b.getStatus() == BookingStatus.EXPIRED) {
            throw ApiException.notFound("PNR_NOT_FOUND", "No booking found for PNR " + pnr + ".");
        }
        return mapper.toPnrStatus(b);
    }

    /** The traveller's bookings, newest journey first; {@code scope} is upcoming, past, cancelled or all. */
    @Transactional(readOnly = true)
    public List<BookingSummary> history(Long userId, String scope) {
        LocalDateTime now = inventoryService.now();
        String s = scope == null ? "all" : scope.toLowerCase(Locale.ROOT);
        EnumSet<BookingStatus> statuses = EnumSet.of(BookingStatus.BOOKED, BookingStatus.PARTIALLY_CANCELLED,
                BookingStatus.CANCELLED, BookingStatus.PAYMENT_PENDING);
        List<Booking> all = bookingRepository.findForUser(userId, statuses);
        List<Booking> chosen = switch (s) {
            case "upcoming" -> all.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED
                    && b.getArrivalAt().isAfter(now)).sorted(Comparator.comparing(Booking::getDepartureAt)).toList();
            case "past" -> all.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED
                    && b.getStatus() != BookingStatus.PAYMENT_PENDING && !b.getArrivalAt().isAfter(now)).toList();
            case "cancelled" -> all.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).toList();
            default -> all;
        };
        return chosen.stream().map(mapper::toSummary).toList();
    }

    // ------------------------------------------------------------------ cancellation

    @Transactional(readOnly = true)
    public CancelPreview previewCancel(String pnr, Long userId, boolean admin, List<Long> passengerIds) {
        return cancellationService.preview(findOwned(pnr, userId, admin), passengerIds);
    }

    @Transactional
    public BookingView cancel(String pnr, Long userId, boolean admin, List<Long> passengerIds) {
        Booking b = lockOwned(pnr, userId, admin);
        boolean byOwner = b.getUser().getId().equals(userId);
        cancellationService.cancel(b, passengerIds, userId, byOwner ? "PASSENGER" : "ADMIN");
        return mapper.toView(b);
    }
}
