package com.railway.reservation.service.rail;

import com.railway.reservation.dto.TrainDtos.*;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.StationRepository;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Stations, timetables, journey search and seat availability (RRS-F-004, RRS-F-005). */
@Service
@Transactional(readOnly = true)
public class TimetableService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final InventoryService inventoryService;
    private final FareCalculator fareCalculator;

    public TimetableService(StationRepository stationRepository, TrainRepository trainRepository,
                            InventoryService inventoryService, FareCalculator fareCalculator) {
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
        this.inventoryService = inventoryService;
        this.fareCalculator = fareCalculator;
    }

    // ------------------------------------------------------------------ stations

    public List<StationDto> stations() {
        return stationRepository.findAllByOrderByNameAsc().stream().map(TimetableService::toDto).toList();
    }

    public Station resolveStation(String input, String field) {
        if (input == null || input.isBlank()) {
            throw ApiException.badRequest("STATION_REQUIRED", "Choose a " + field + " station.");
        }
        String q = input.trim();
        Optional<Station> byCode = stationRepository.findByCodeIgnoreCase(q);
        if (byCode.isPresent()) return byCode.get();
        String lower = q.toLowerCase(Locale.ROOT);
        return stationRepository.findAllByOrderByNameAsc().stream()
                .filter(s -> s.getName().equalsIgnoreCase(q) || s.getCity().equalsIgnoreCase(q)
                        || s.getName().toLowerCase(Locale.ROOT).startsWith(lower))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_STATION",
                        "We do not have a station called \"" + q + "\". Pick one from the list."));
    }

    private List<Station> withCity(Station station, boolean nearby) {
        if (!nearby) return List.of(station);
        List<Station> all = new ArrayList<>(stationRepository.findByCityIgnoreCase(station.getCity()));
        all.removeIf(s -> s.getId().equals(station.getId()));
        all.addFirst(station);
        return all;
    }

    // ------------------------------------------------------------------ search

    public SearchResponse search(String fromInput, String toInput, LocalDate date, String quotaCode, boolean nearby) {
        Station from = resolveStation(fromInput, "departure");
        Station to = resolveStation(toInput, "arrival");
        if (from.getId().equals(to.getId())) {
            throw ApiException.badRequest("SAME_STATION", "Departure and arrival stations must be different.");
        }
        if (date == null) {
            throw ApiException.badRequest("DATE_REQUIRED", "Choose a journey date.");
        }
        LocalDate today = inventoryService.today();
        if (date.isBefore(today)) {
            throw ApiException.badRequest("DATE_IN_PAST", "Journey date cannot be in the past.");
        }
        if (date.isAfter(today.plusDays(InventoryService.ADVANCE_BOOKING_DAYS))) {
            throw ApiException.badRequest("DATE_TOO_FAR", "Reservations open " + InventoryService.ADVANCE_BOOKING_DAYS
                    + " days ahead. Choose a date on or before " + today.plusDays(InventoryService.ADVANCE_BOOKING_DAYS) + ".");
        }
        Quota quota = Quota.fromCode(quotaCode);

        List<Station> fromSet = withCity(from, nearby);
        List<Station> toSet = withCity(to, nearby);
        Set<Long> fromIds = new HashSet<>();
        fromSet.forEach(s -> fromIds.add(s.getId()));
        Set<Long> toIds = new HashSet<>();
        toSet.forEach(s -> toIds.add(s.getId()));

        List<TrainResult> results = new ArrayList<>();
        for (Train train : trainRepository.findServing(fromIds, toIds)) {
            List<TrainStop> stops = train.orderedStops();
            TrainStop board = stops.stream().filter(s -> fromIds.contains(s.getStation().getId())).findFirst().orElse(null);
            if (board == null) continue;
            TrainStop alight = stops.stream()
                    .filter(s -> s.getSeq() > board.getSeq() && toIds.contains(s.getStation().getId()))
                    .findFirst().orElse(null);
            if (alight == null) continue;
            Journey journey = Journey.of(train, board, alight, date);
            if (journey == null || !journey.runs()) continue;
            results.add(toResult(journey, quota));
        }
        results.sort(Comparator.comparing((TrainResult r) -> r.from().time()));
        return new SearchResponse(toDto(from), toDto(to), date, quota.code(),
                fromSet.stream().map(TimetableService::toDto).toList(),
                toSet.stream().map(TimetableService::toDto).toList(), results);
    }

    private TrainResult toResult(Journey j, Quota quota) {
        Train t = j.train();
        List<ClassAvailability> classes = t.getClasses().stream()
                .sorted(Comparator.comparingInt(c -> c.getCoachClass().ordinal()))
                .map(tc -> classAvailability(j, tc, quota))
                .toList();
        return new TrainResult(t.getNumber(), t.getName(), t.getType().name(), t.getType().label(),
                t.getRunningDays(), t.isPantry(),
                endPoint(j.from(), j.departureAt(), false), endPoint(j.to(), j.arrivalAt(), true),
                j.durationMinutes(), j.distanceKm(), j.stopsBetween(),
                t.origin().getStation().getCode(), t.terminus().getStation().getCode(), classes);
    }

    private EndPoint endPoint(TrainStop stop, LocalDateTime at, boolean arrival) {
        Integer day = arrival ? stop.getArrivalDay() : stop.getDepartureDay();
        return new EndPoint(stop.getStation().getCode(), stop.getStation().getName(), stop.getStation().getCity(),
                at.toLocalTime().format(HM), at.toLocalDate(), day == null ? 1 : day);
    }

    ClassAvailability classAvailability(Journey j, TrainClass tc, Quota quota) {
        CoachClass cc = tc.getCoachClass();
        int fare = fareCalculator.perPassenger(j.train(), cc, j.distanceKm(), quota).total();
        if (quota == Quota.TATKAL && !cc.supportsTatkal()) {
            return new ClassAvailability(cc.code(), cc.label(), null, "NOT_OFFERED", 0, "No Tatkal", null);
        }
        if (!j.departureAt().isAfter(inventoryService.now())) {
            return new ClassAvailability(cc.code(), cc.label(), fare, "DEPARTED", 0, "Departed", null);
        }
        if (quota == Quota.TATKAL && !inventoryService.tatkalOpen(cc, j.trainStartDate())) {
            LocalDateTime opens = inventoryService.tatkalOpensAt(cc, j.trainStartDate());
            return new ClassAvailability(cc.code(), cc.label(), fare, "NOT_OPEN", 0,
                    "Opens " + opens.toLocalTime().format(HM), opens);
        }
        SeatLedger ledger = inventoryService.readLedger(tc, j.trainStartDate());
        SeatLedger.Availability a = ledger.availability(quota, j.from().getSeq(), j.to().getSeq(),
                inventoryService.chartPrepared(j.train(), j.trainStartDate()));
        String status = switch (a.state()) {
            case AVAILABLE -> "AVAILABLE";
            case RAC -> "RAC";
            case WAITLIST -> "WAITLIST";
            case REGRET -> "REGRET";
        };
        return new ClassAvailability(cc.code(), cc.label(), fare, status, a.count(), a.label(), null);
    }

    // ------------------------------------------------------------------ train pages

    public Train findTrain(String number) {
        return trainRepository.findByNumber(number == null ? "" : number.trim())
                .orElseThrow(() -> ApiException.notFound("TRAIN_NOT_FOUND", "No train numbered " + number + "."));
    }

    public List<TrainSummary> trains() {
        return trainRepository.findAllByOrderByNumberAsc().stream().filter(Train::isActive).map(t -> {
            TrainStop o = t.origin(), e = t.terminus();
            return new TrainSummary(t.getNumber(), t.getName(), t.getType().label(), t.getRunningDays(),
                    o.getStation().getCode(), o.getStation().getName(), fmt(o.getDepartureTime()),
                    e.getStation().getCode(), e.getStation().getName(), fmt(e.getArrivalTime()), e.getArrivalDay(),
                    e.getDistanceKm(), t.getClasses().stream().sorted(Comparator.comparingInt(c -> c.getCoachClass().ordinal()))
                    .map(c -> c.getCoachClass().code()).toList());
        }).toList();
    }

    public TrainDetail trainDetail(String number) {
        return toDetail(findTrain(number));
    }

    public static TrainDetail toDetail(Train t) {
        List<TrainStop> stops = t.orderedStops();
        List<StopDto> stopDtos = stops.stream().map(TimetableService::toStopDto).toList();
        List<ClassInfo> classes = t.getClasses().stream()
                .sorted(Comparator.comparingInt(c -> c.getCoachClass().ordinal()))
                .map(c -> new ClassInfo(c.getCoachClass().code(), c.getCoachClass().label(), c.getCoaches(),
                        c.getCoachClass().berthsPerCoach()))
                .toList();
        int distance = stops.isEmpty() ? 0 : stops.getLast().getDistanceKm();
        int duration = stops.size() < 2 ? 0 : stops.getLast().getArrivalMinute() - stops.getFirst().getDepartureMinute();
        return new TrainDetail(t.getNumber(), t.getName(), t.getType().name(), t.getType().label(), t.getRunningDays(),
                t.isPantry(), t.isActive(), distance, duration, classes, stopDtos);
    }

    static StopDto toStopDto(TrainStop s) {
        Integer day = s.getArrivalDay() != null ? s.getArrivalDay() : s.getDepartureDay();
        return new StopDto(s.getSeq(), s.getStation().getCode(), s.getStation().getName(), s.getStation().getCity(),
                fmt(s.getArrivalTime()), fmt(s.getDepartureTime()), day, s.getHaltMinutes(), s.getDistanceKm());
    }

    private static String fmt(java.time.LocalTime t) { return t == null ? null : t.format(HM); }

    public AvailabilityCalendar calendar(String number, String fromCode, String toCode, String classCode,
                                         String quotaCode, LocalDate date, int days) {
        Train train = findTrain(number);
        Quota quota = Quota.fromCode(quotaCode);
        CoachClass cc = CoachClass.fromCode(classCode)
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_CLASS", "Unknown class " + classCode + "."));
        TrainClass tc = train.classOf(cc)
                .orElseThrow(() -> ApiException.badRequest("CLASS_NOT_ON_TRAIN", train.getNumber() + " has no " + cc.label() + "."));
        TrainStop from = train.stopFor(fromCode).orElseThrow(() -> ApiException.badRequest("STOP_NOT_ON_ROUTE",
                train.getNumber() + " does not stop at " + fromCode + "."));
        TrainStop to = train.stopFor(toCode).orElseThrow(() -> ApiException.badRequest("STOP_NOT_ON_ROUTE",
                train.getNumber() + " does not stop at " + toCode + "."));
        if (from.getSeq() >= to.getSeq()) {
            throw ApiException.badRequest("WRONG_DIRECTION", "This train reaches " + toCode + " before " + fromCode + ".");
        }
        LocalDate today = inventoryService.today();
        LocalDate lastDay = today.plusDays(InventoryService.ADVANCE_BOOKING_DAYS);
        LocalDate first = date == null || date.isBefore(today) ? today : date;
        int span = Math.max(1, Math.min(days, 14));
        List<DayAvailability> out = new ArrayList<>();
        for (LocalDate d = first; out.size() < span && !d.isAfter(lastDay); d = d.plusDays(1)) {
            Journey j = Journey.of(train, from, to, d);
            boolean runs = j != null && j.runs();
            out.add(new DayAvailability(d, runs, runs ? classAvailability(j, tc, quota) : null));
        }
        FareCalculator.Fare fare = fareCalculator.perPassenger(train, cc, to.getDistanceKm() - from.getDistanceKm(), quota);
        return new AvailabilityCalendar(train.getNumber(), cc.code(), quota.code(), from.getStation().getCode(),
                to.getStation().getCode(), toDto(fare), out);
    }

    public StationBoard departures(String stationCode, LocalDate date) {
        Station station = resolveStation(stationCode, "departure");
        LocalDate day = date == null ? inventoryService.today() : date;
        List<DepartureRow> rows = new ArrayList<>();
        for (Train t : trainRepository.findCallingAt(station.getId())) {
            TrainStop stop = t.stopFor(station.getCode()).orElse(null);
            if (stop == null || stop.getDepartureMinute() == null) continue;
            LocalDate start = day.minusDays(stop.getDepartureDay() - 1L);
            if (!t.runsOn(start)) continue;
            TrainStop end = t.terminus();
            rows.add(new DepartureRow(t.getNumber(), t.getName(), t.getType().label(),
                    fmt(stop.getDepartureTime()), day, end.getStation().getCode(), end.getStation().getName(),
                    end.getStation().getCity(), stop.getSeq() == t.origin().getSeq(), fmt(end.getArrivalTime()),
                    end.getArrivalDay() - stop.getDepartureDay() + 1));
        }
        rows.sort(Comparator.comparing(DepartureRow::departure));
        return new StationBoard(toDto(station), day, rows);
    }

    // ------------------------------------------------------------------ mapping

    public static StationDto toDto(Station s) {
        return new StationDto(s.getCode(), s.getName(), s.getCity(), s.getState());
    }

    public static FareDto toDto(FareCalculator.Fare f) {
        return new FareDto(f.base(), f.reservation(), f.superfast(), f.tatkal(), f.catering(), f.gst(), f.total());
    }
}
