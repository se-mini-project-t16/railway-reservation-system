package com.railway.reservation.support;

import com.railway.reservation.model.*;
import com.railway.reservation.repository.CancellationRuleRepository;
import com.railway.reservation.repository.StationRepository;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, fully known network for tests:
 *
 * <pre>
 *  12901 Test Superfast (daily): AAA 08:00 -> BBB 10:00/10:05 -> CCC 14:00/14:10 -> DDD 02:00 (day 2)
 *        0 km               150 km              400 km               800 km
 *        classes: SL x1 coach (72 berths), 3A x1 coach (72 berths), CC x1 coach (78 seats)
 *  12902 Weekday Express (Mon to Fri): AAA 18:00 -> DDD 06:00 (day 2), SL x1
 *  AAB is a second station in Alpha city (for nearby-station search).
 * </pre>
 */
public final class RailFixtures {

    private RailFixtures() {}

    /** Empties every table, then restores the refund rules. */
    public static void reset(JdbcTemplate jdbc, CancellationRuleRepository rules) {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'", String.class);
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String t : tables) jdbc.execute("TRUNCATE TABLE \"" + t + "\" RESTART IDENTITY");
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        rules.save(new CancellationRule(48, BigDecimal.ZERO, true, "48 hours or more: flat charge"));
        rules.save(new CancellationRule(12, new BigDecimal("25"), true, "12 to 48 hours: 25%"));
        rules.save(new CancellationRule(4, new BigDecimal("50"), true, "4 to 12 hours: 50%"));
        rules.save(new CancellationRule(0, new BigDecimal("100"), false, "Under 4 hours: no refund"));
    }

    public static Map<String, Station> stations(StationRepository repo) {
        Map<String, Station> out = new HashMap<>();
        for (String[] s : new String[][]{
                {"AAA", "Alpha Central", "Alpha", "State One"}, {"AAB", "Alpha Cantonment", "Alpha", "State One"},
                {"BBB", "Bravo Junction", "Bravo", "State One"}, {"CCC", "Charlie Road", "Charlie", "State Two"},
                {"DDD", "Delta Terminus", "Delta", "State Two"}}) {
            out.put(s[0], repo.save(new Station(s[0], s[1], s[2], s[3])));
        }
        return out;
    }

    public static Train superfast(Map<String, Station> st, TrainRepository repo) {
        Train t = new Train("12901", "Test Superfast", TrainType.SUPERFAST, "1111111", true);
        t.addClass(new TrainClass(CoachClass.SLEEPER, 1));
        t.addClass(new TrainClass(CoachClass.THIRD_AC, 1));
        t.addClass(new TrainClass(CoachClass.CHAIR_CAR, 1));
        t.addStop(new TrainStop(1, st.get("AAA"), null, 8 * 60, 0));
        t.addStop(new TrainStop(2, st.get("BBB"), 10 * 60, 10 * 60 + 5, 150));
        t.addStop(new TrainStop(3, st.get("CCC"), 14 * 60, 14 * 60 + 10, 400));
        t.addStop(new TrainStop(4, st.get("DDD"), 26 * 60, null, 800));
        return repo.save(t);
    }

    public static Train weekday(Map<String, Station> st, TrainRepository repo) {
        Train t = new Train("12902", "Weekday Express", TrainType.SUPERFAST, "1111100", false);
        t.addClass(new TrainClass(CoachClass.SLEEPER, 1));
        t.addStop(new TrainStop(1, st.get("AAA"), null, 18 * 60, 0));
        t.addStop(new TrainStop(2, st.get("DDD"), 30 * 60, null, 800));
        return repo.save(t);
    }

    /** A valid admin train form as JSON-ready map. */
    public static Map<String, Object> trainForm(String number, String name) {
        return Map.of(
                "number", number, "name", name, "type", "SUPERFAST", "runningDays", "1111111",
                "pantry", false, "active", true,
                "classes", List.of(Map.of("code", "SL", "coaches", 2), Map.of("code", "3A", "coaches", 1)),
                "stops", List.of(
                        Map.of("code", "AAA", "arrival", "", "departure", "09:00", "day", 1, "distanceKm", 0),
                        Map.of("code", "BBB", "arrival", "11:00", "departure", "11:05", "day", 1, "distanceKm", 160),
                        Map.of("code", "DDD", "arrival", "01:30", "departure", "", "day", 2, "distanceKm", 700)));
    }
}
