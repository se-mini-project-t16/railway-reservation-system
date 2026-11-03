package com.railway.reservation.service.rail;

import com.railway.reservation.dto.AdminDtos.*;
import com.railway.reservation.dto.TrainDtos.StationDto;
import com.railway.reservation.dto.TrainDtos.TrainDetail;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Operations console (RRS-F-015, RRS-F-016): fleet, routes, bookings, charts, users and refund rules. */
@Service
@Transactional
public class AdminService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final TrainRepository trainRepository;
    private final StationRepository stationRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CancellationRuleRepository ruleRepository;
    private final CancellationAuditLogRepository auditRepository;
    private final InventoryService inventoryService;
    private final TimetableService timetableService;

    public AdminService(TrainRepository trainRepository, StationRepository stationRepository,
                        BookingRepository bookingRepository, UserRepository userRepository,
                        CancellationRuleRepository ruleRepository, CancellationAuditLogRepository auditRepository,
                        InventoryService inventoryService, TimetableService timetableService) {
        this.trainRepository = trainRepository;
        this.stationRepository = stationRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.ruleRepository = ruleRepository;
        this.auditRepository = auditRepository;
        this.inventoryService = inventoryService;
        this.timetableService = timetableService;
    }

    // ------------------------------------------------------------------ overview

    @Transactional(readOnly = true)
    public Overview overview() {
        LocalDate today = inventoryService.today();
        LocalDateTime now = inventoryService.now();
        LocalDate weekStart = today.minusDays(6);
        List<Booking> week = bookingRepository.findBookedBetween(weekStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<CancellationAuditLog> weekCancels = auditRepository.findByCancelledAtBetween(
                weekStart.atStartOfDay(), today.plusDays(1).atStartOfDay());

        List<DayFigure> days = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(today); d = d.plusDays(1)) {
            LocalDate day = d;
            List<Booking> booked = week.stream().filter(b -> b.getBookedAt().toLocalDate().equals(day)).toList();
            int refunds = weekCancels.stream().filter(a -> a.getCancelledAt().toLocalDate().equals(day))
                    .mapToInt(a -> a.getRefundAmount().intValue()).sum();
            days.add(new DayFigure(day, booked.size(), booked.stream().mapToInt(b -> b.getPassengers().size()).sum(),
                    booked.stream().mapToInt(b -> b.getTotalFare().intValue()).sum(), refunds));
        }
        DayFigure t = days.getLast();
        int cancellationsToday = (int) weekCancels.stream()
                .filter(a -> a.getCancelledAt().toLocalDate().equals(today)).count();

        Map<CoachClass, Integer> mix = new EnumMap<>(CoachClass.class);
        week.forEach(b -> mix.merge(b.getCoachClass(), b.getPassengers().size(), Integer::sum));
        List<ClassFigure> classMix = mix.entrySet().stream()
                .sorted(Map.Entry.<CoachClass, Integer>comparingByValue().reversed())
                .map(e -> new ClassFigure(e.getKey().code(), e.getKey().label(), e.getValue())).toList();

        List<Booking> soon = bookingRepository.findDepartingBetween(
                EnumSet.of(BookingStatus.BOOKED, BookingStatus.PARTIALLY_CANCELLED), now, now.plusHours(48));
        Map<String, List<Booking>> byRun = soon.stream().collect(Collectors.groupingBy(
                b -> b.getTrain().getNumber() + "|" + b.getTrainStartDate(), LinkedHashMap::new, Collectors.toList()));
        List<UpcomingDeparture> upcoming = byRun.values().stream().map(list -> {
            Booking b = list.getFirst();
            Train train = b.getTrain();
            return new UpcomingDeparture(train.getNumber(), train.getName(), b.getTrainStartDate(),
                    train.origin().getDepartureTime().format(HM), train.origin().getStation().getCode(), list.size(),
                    list.stream().mapToInt(x -> x.activePassengers().size()).sum(),
                    inventoryService.chartPrepared(train, b.getTrainStartDate()));
        }).limit(12).toList();

        List<Train> trains = trainRepository.findAll();
        return new Overview(today, t.bookings(), t.travellers(), t.revenue(), cancellationsToday, t.refunds(),
                (int) bookingRepository.countByStatus(BookingStatus.PAYMENT_PENDING),
                (int) trains.stream().filter(Train::isActive).count(), trains.size(),
                (int) userRepository.countByRole(Role.ROLE_PASSENGER), days, classMix, upcoming);
    }

    // ------------------------------------------------------------------ fleet

    @Transactional(readOnly = true)
    public List<TrainRow> trains() {
        return trainRepository.findAllByOrderByNumberAsc().stream().map(t -> {
            TrainStop o = t.origin(), e = t.terminus();
            return new TrainRow(t.getNumber(), t.getName(), t.getType().name(), t.getType().label(), t.getRunningDays(),
                    t.isActive(), o.getStation().getCode(), o.getStation().getName(), o.getDepartureTime().format(HM),
                    e.getStation().getCode(), e.getStation().getName(), e.getArrivalTime().format(HM), e.getArrivalDay(),
                    t.getClasses().stream().sorted(Comparator.comparingInt(c -> c.getCoachClass().ordinal()))
                            .map(c -> c.getCoachClass().code()).toList(),
                    bookingRepository.countByTrainId(t.getId()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public TrainDetail train(String number) {
        return TimetableService.toDetail(timetableService.findTrain(number));
    }

    public TrainDetail createTrain(TrainForm form) {
        if (trainRepository.existsByNumber(form.number())) {
            throw ApiException.conflict("DUPLICATE_TRAIN", "Train " + form.number() + " already exists.");
        }
        Train train = new Train(form.number(), form.name().trim(), trainType(form.type()), form.runningDays(), Boolean.TRUE.equals(form.pantry()));
        train.setActive(!Boolean.FALSE.equals(form.active()));
        applyClasses(train, form.classes(), false);
        applyStops(train, form.stops());
        return TimetableService.toDetail(trainRepository.save(train));
    }

    /**
     * Updates a train. Once tickets exist, the route is fixed (tickets point at
     * its stops) and coaches can be added but not removed.
     */
    public TrainDetail updateTrain(String number, TrainForm form) {
        Train train = timetableService.findTrain(number);
        if (!train.getNumber().equals(form.number())) {
            throw ApiException.badRequest("NUMBER_CHANGE", "A train's number cannot be changed. Create a new train instead.");
        }
        boolean hasBookings = bookingRepository.countByTrainId(train.getId()) > 0;
        train.setName(form.name().trim());
        train.setType(trainType(form.type()));
        train.setRunningDays(form.runningDays());
        train.setPantry(Boolean.TRUE.equals(form.pantry()));
        train.setActive(!Boolean.FALSE.equals(form.active()));
        applyClasses(train, form.classes(), hasBookings);
        if (form.stops() != null && !sameRoute(train, form.stops())) {
            if (hasBookings) {
                throw ApiException.conflict("ROUTE_LOCKED",
                        "This train has tickets, so its route and timings cannot change. Deactivate it and create a new service instead.");
            }
            train.getStops().clear();
            trainRepository.flush();
            applyStops(train, form.stops());
        }
        return TimetableService.toDetail(train);
    }

    public void setActive(String number, boolean active) {
        timetableService.findTrain(number).setActive(active);
    }

    public void deleteTrain(String number) {
        Train train = timetableService.findTrain(number);
        if (bookingRepository.countByTrainId(train.getId()) > 0) {
            throw ApiException.conflict("TRAIN_HAS_BOOKINGS",
                    "Train " + number + " has tickets on record, so it cannot be deleted. Deactivate it instead.");
        }
        trainRepository.delete(train);
    }

    private TrainType trainType(String raw) {
        try {
            return TrainType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("UNKNOWN_TYPE", "Choose a train type.");
        }
    }

    private void applyClasses(Train train, List<ClassForm> forms, boolean hasBookings) {
        Map<CoachClass, Integer> wanted = new EnumMap<>(CoachClass.class);
        for (ClassForm f : forms) {
            CoachClass cc = CoachClass.fromCode(f.code())
                    .orElseThrow(() -> ApiException.badRequest("UNKNOWN_CLASS", "Unknown class " + f.code() + "."));
            if (wanted.put(cc, f.coaches()) != null) {
                throw ApiException.badRequest("DUPLICATE_CLASS", cc.label() + " is listed twice.");
            }
        }
        for (TrainClass existing : new ArrayList<>(train.getClasses())) {
            Integer coaches = wanted.remove(existing.getCoachClass());
            if (coaches == null) {
                if (hasBookings) {
                    throw ApiException.conflict("CLASS_LOCKED", "This train has tickets, so "
                            + existing.getCoachClass().label() + " cannot be removed.");
                }
                train.getClasses().remove(existing);
            } else {
                if (hasBookings && coaches < existing.getCoaches()) {
                    throw ApiException.conflict("CLASS_LOCKED", "This train has tickets, so coaches can be added but not removed.");
                }
                existing.setCoaches(coaches);
            }
        }
        wanted.forEach((cc, coaches) -> train.addClass(new TrainClass(cc, coaches)));
    }

    private boolean sameRoute(Train train, List<StopForm> forms) {
        List<TrainStop> stops = train.orderedStops();
        if (stops.size() != forms.size()) return false;
        List<int[]> computed = minutes(forms);
        for (int i = 0; i < stops.size(); i++) {
            TrainStop s = stops.get(i);
            StopForm f = forms.get(i);
            if (!s.getStation().getCode().equalsIgnoreCase(f.code()) || s.getDistanceKm() != f.distanceKm()
                    || !Objects.equals(s.getArrivalMinute(), toInteger(computed.get(i)[0]))
                    || !Objects.equals(s.getDepartureMinute(), toInteger(computed.get(i)[1]))) return false;
        }
        return true;
    }

    private static Integer toInteger(int v) { return v < 0 ? null : v; }

    /** Converts clock times and day numbers to minutes after origin-day midnight, validating order. */
    private List<int[]> minutes(List<StopForm> forms) {
        List<int[]> out = new ArrayList<>();
        int previous = -1;
        for (int i = 0; i < forms.size(); i++) {
            StopForm f = forms.get(i);
            boolean first = i == 0, last = i == forms.size() - 1;
            int dayBase = (f.day() - 1) * 1440;
            int arr = -1, dep = -1;
            if (!first) {
                if (blank(f.arrival())) throw ApiException.badRequest("ARRIVAL_REQUIRED", "Stop " + (i + 1) + " needs an arrival time.");
                arr = dayBase + clock(f.arrival());
                if (arr <= previous) throw ApiException.badRequest("TIME_ORDER",
                        "Stop " + (i + 1) + " (" + f.code() + ") arrives before the train leaves the stop before it. Check its day.");
                previous = arr;
            }
            if (!last) {
                if (blank(f.departure())) throw ApiException.badRequest("DEPARTURE_REQUIRED", "Stop " + (i + 1) + " needs a departure time.");
                dep = dayBase + clock(f.departure());
                if (arr >= 0 && dep < arr) dep += 1440;
                if (dep < previous) throw ApiException.badRequest("TIME_ORDER", "Stop " + (i + 1) + " departs before it arrives.");
                previous = dep;
            }
            out.add(new int[]{arr, dep});
        }
        return out;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static int clock(String hm) {
        LocalTime t = LocalTime.parse(hm);
        return t.getHour() * 60 + t.getMinute();
    }

    private void applyStops(Train train, List<StopForm> forms) {
        if (forms == null || forms.size() < 2) {
            throw ApiException.badRequest("ROUTE_TOO_SHORT", "A route needs at least two stations.");
        }
        List<int[]> minutes = minutes(forms);
        Set<String> seen = new HashSet<>();
        int previousKm = -1;
        for (int i = 0; i < forms.size(); i++) {
            StopForm f = forms.get(i);
            Station station = stationRepository.findByCodeIgnoreCase(f.code())
                    .orElseThrow(() -> ApiException.badRequest("UNKNOWN_STATION", "Unknown station code " + f.code() + "."));
            if (!seen.add(station.getCode())) {
                throw ApiException.badRequest("DUPLICATE_STOP", station.getName() + " appears twice on the route.");
            }
            if (i == 0 && f.distanceKm() != 0) {
                throw ApiException.badRequest("ORIGIN_DISTANCE", "The first station is at 0 km.");
            }
            if (f.distanceKm() <= previousKm) {
                throw ApiException.badRequest("DISTANCE_ORDER", "Distances must increase along the route (stop " + (i + 1) + ").");
            }
            previousKm = f.distanceKm();
            train.addStop(new TrainStop(i + 1, station, toInteger(minutes.get(i)[0]), toInteger(minutes.get(i)[1]), f.distanceKm()));
        }
    }

    // ------------------------------------------------------------------ stations

    @Transactional(readOnly = true)
    public List<StationDto> stations() {
        return timetableService.stations();
    }

    public StationDto createStation(StationForm form) {
        if (stationRepository.existsByCodeIgnoreCase(form.code())) {
            throw ApiException.conflict("DUPLICATE_STATION", "Station code " + form.code() + " is taken.");
        }
        return TimetableService.toDto(stationRepository.save(
                new Station(form.code(), form.name().trim(), form.city().trim(), form.state().trim())));
    }

    // ------------------------------------------------------------------ bookings

    @Transactional(readOnly = true)
    public PageOf<BookingRow> bookings(String pnr, String email, String trainNo, LocalDate date, String status,
                                       int page, int size) {
        BookingStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = BookingStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("UNKNOWN_STATUS", "Unknown booking status " + status + ".");
            }
        }
        Page<Booking> result = bookingRepository.search(blankToNull(pnr), lowerOrNull(email), blankToNull(trainNo), date, st,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt")));
        List<BookingRow> rows = result.getContent().stream().map(b -> new BookingRow(b.getPnr(), b.getStatus().name(),
                BookingMapper.statusLabel(b.getStatus()), b.getTrain().getNumber(), b.getTrain().getName(),
                b.getCoachClass().code(), b.getQuota().code(), b.getFromCode(), b.getToCode(), b.getJourneyDate(),
                b.getDepartureAt().toLocalTime().format(HM), b.getPassengers().size(), b.activePassengers().size(),
                b.getUser().getName(), b.getUser().getEmail(), b.getTotalFare().intValue(),
                b.getRefundTotal().intValue(), b.getCreatedAt())).toList();
        return new PageOf<>(rows, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static String lowerOrNull(String s) { return s == null || s.isBlank() ? null : s.trim().toLowerCase(Locale.ROOT); }

    // ------------------------------------------------------------------ chart

    @Transactional(readOnly = true)
    public Chart chart(String trainNo, LocalDate startDate, String classCode) {
        Train train = timetableService.findTrain(trainNo);
        List<TrainClass> classes = train.getClasses().stream()
                .sorted(Comparator.comparingInt(c -> c.getCoachClass().ordinal())).toList();
        if (classes.isEmpty()) throw ApiException.badRequest("NO_CLASSES", "This train has no coaches configured.");
        TrainClass tc = classCode == null || classCode.isBlank() ? classes.getFirst()
                : CoachClass.fromCode(classCode).flatMap(train::classOf)
                .orElseThrow(() -> ApiException.badRequest("CLASS_NOT_ON_TRAIN", trainNo + " has no " + classCode + " class."));
        LocalDate date = startDate == null ? inventoryService.today() : startDate;
        SeatLedger ledger = inventoryService.readLedger(tc, date);
        CoachClass cc = tc.getCoachClass();

        List<ChartCoach> coaches = new ArrayList<>();
        int per = cc.berthsPerCoach();
        for (int c = 0; c < tc.getCoaches(); c++) {
            List<ChartBerth> berths = new ArrayList<>();
            for (int b = 1; b <= per; b++) {
                int index = c * per + b - 1;
                List<ChartOccupant> occupants = ledger.occupantsOf(index).stream()
                        .sorted(Comparator.comparingInt(p -> p.getBooking().getFromSeq()))
                        .map(p -> new ChartOccupant(p.getBooking().getPnr(), p.getSerial(), p.getName(), p.getAge(),
                                p.getGender().name(), p.getBooking().getFromCode(), p.getBooking().getToCode(),
                                p.getBooking().getStatus().name())).toList();
                berths.add(new ChartBerth(b, cc.berthTypeOf(b).code(), ledger.isExternal(index), occupants));
            }
            coaches.add(new ChartCoach(DemandSimulator.coachLabel(cc, c + 1), berths));
        }
        List<ChartQueueRow> rac = queueRows(ledger, SeatStatus.RAC);
        List<ChartQueueRow> wl = queueRows(ledger, SeatStatus.WL);
        int system = (int) ledger.activePassengers().stream().filter(p -> p.getCurrentStatus() == SeatStatus.CNF).count();
        return new Chart(train.getNumber(), train.getName(), cc.code(), cc.label(), date,
                inventoryService.chartPrepared(train, date), inventoryService.chartPreparedAt(train, date),
                tc.capacity(), tc.tatkalBerths(), ledger.externalCount(), system, ledger.freeWholeRun(),
                ledger.externalRac(), ledger.externalWl(), coaches, rac, wl,
                classes.stream().map(c -> c.getCoachClass().code()).toList());
    }

    private List<ChartQueueRow> queueRows(SeatLedger ledger, SeatStatus status) {
        return ledger.activePassengers().stream().filter(p -> p.getCurrentStatus() == status)
                .sorted(Comparator.comparingLong(Passenger::getQueueSeq))
                .map(p -> new ChartQueueRow(p.getBooking().getPnr(), p.getSerial(), p.getName(), p.getAge(),
                        p.getGender().name(), p.getBooking().getFromCode(), p.getBooking().getToCode(),
                        status.name() + " (" + p.getBooking().getQuota().code() + ")", ledger.currentNumber(p)))
                .toList();
    }

    @Transactional(readOnly = true)
    public String chartCsv(String trainNo, LocalDate startDate, String classCode) {
        Chart chart = chart(trainNo, startDate, classCode);
        StringBuilder sb = new StringBuilder("coach,berth,berth_type,status,pnr,passenger,age,gender,from,to\n");
        for (ChartCoach coach : chart.coaches()) {
            for (ChartBerth berth : coach.berths()) {
                if (berth.otherChannel()) {
                    sb.append(coach.coach()).append(',').append(berth.berth()).append(',').append(berth.type())
                            .append(",OTHER_CHANNEL,,,,,,\n");
                }
                for (ChartOccupant o : berth.occupants()) {
                    sb.append(coach.coach()).append(',').append(berth.berth()).append(',').append(berth.type())
                            .append(",CNF,").append(o.pnr()).append(',').append(csv(o.name())).append(',').append(o.age())
                            .append(',').append(o.gender()).append(',').append(o.from()).append(',').append(o.to()).append('\n');
                }
            }
        }
        for (ChartQueueRow r : chart.rac()) appendQueue(sb, r);
        for (ChartQueueRow r : chart.waitlist()) appendQueue(sb, r);
        return sb.toString();
    }

    private static void appendQueue(StringBuilder sb, ChartQueueRow r) {
        sb.append(",,,").append(r.status().split(" ")[0]).append(' ').append(r.number()).append(',').append(r.pnr())
                .append(',').append(csv(r.name())).append(',').append(r.age()).append(',').append(r.gender())
                .append(',').append(r.from()).append(',').append(r.to()).append('\n');
    }

    /** Quotes a CSV field and neutralises spreadsheet formula prefixes. */
    static String csv(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // ------------------------------------------------------------------ users and rules

    @Transactional(readOnly = true)
    public PageOf<UserRow> users(int page, int size) {
        Page<User> result = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        List<UserRow> rows = result.getContent().stream().map(u -> new UserRow(u.getId(), u.getName(), u.getEmail(),
                u.getPhone(), u.getRole().name().replace("ROLE_", ""), u.isAccountLocked(), u.getFailedLoginAttempts(),
                u.getCreatedAt())).toList();
        return new PageOf<>(rows, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public void unlockUser(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "No such user."));
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
    }

    public void updateRules(List<RuleForm> forms) {
        Map<Long, CancellationRule> rules = ruleRepository.findAll().stream()
                .collect(Collectors.toMap(CancellationRule::getId, r -> r));
        for (RuleForm f : forms) {
            CancellationRule r = rules.get(f.id());
            if (r == null) throw ApiException.badRequest("UNKNOWN_RULE", "Unknown rule " + f.id() + ".");
            r.update(f.deductionPercent(), Boolean.TRUE.equals(f.flatMinimum()), f.description().trim());
        }
    }

    @Transactional(readOnly = true)
    public List<CancellationRule> rules() {
        return ruleRepository.findAllByOrderByMinHoursBeforeDesc();
    }
}
