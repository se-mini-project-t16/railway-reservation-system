package com.railway.reservation.config;

import com.railway.reservation.model.*;
import com.railway.reservation.repository.CancellationRuleRepository;
import com.railway.reservation.repository.StationRepository;
import com.railway.reservation.repository.TrainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

/**
 * Reference data: the refund rule table always, and when demo seeding is on
 * (the default outside tests) the station list and a timetable of long-
 * distance trains on India's main corridors. Train numbers, names and
 * routes follow real services; timings and distances are close
 * approximations for the simulation. Each train's times are built from leg
 * running times, so every timetable is internally consistent. Seeding is
 * idempotent: it does nothing once stations exist.
 */
@Component
@Order(20)
public class RailDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RailDataSeeder.class);

    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final CancellationRuleRepository ruleRepository;
    private final boolean seedTimetable;

    public RailDataSeeder(StationRepository stationRepository, TrainRepository trainRepository,
                          CancellationRuleRepository ruleRepository,
                          @Value("${app.seed-demo-data:true}") boolean seedTimetable) {
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
        this.ruleRepository = ruleRepository;
        this.seedTimetable = seedTimetable;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRules();
        if (seedTimetable && stationRepository.count() == 0) {
            Map<String, Station> stations = seedStations();
            int trains = seedTrains(stations);
            log.info("Seeded {} stations and {} trains", stations.size(), trains);
        }
    }

    // ------------------------------------------------------------------ refund rules

    void seedRules() {
        if (ruleRepository.count() > 0) return;
        ruleRepository.save(new CancellationRule(48, new BigDecimal("0"), true,
                "48 hours or more before departure: flat charge per traveller"));
        ruleRepository.save(new CancellationRule(12, new BigDecimal("25"), true,
                "12 to 48 hours before departure: 25% of the fare"));
        ruleRepository.save(new CancellationRule(4, new BigDecimal("50"), true,
                "4 to 12 hours before departure: 50% of the fare"));
        ruleRepository.save(new CancellationRule(0, new BigDecimal("100"), false,
                "Under 4 hours before departure: no refund"));
    }

    // ------------------------------------------------------------------ stations

    private static final String[][] STATIONS = {
            {"MAS", "Chennai Central", "Chennai", "Tamil Nadu"},
            {"AJJ", "Arakkonam Junction", "Arakkonam", "Tamil Nadu"},
            {"KPD", "Katpadi Junction", "Vellore", "Tamil Nadu"},
            {"JTJ", "Jolarpettai Junction", "Jolarpettai", "Tamil Nadu"},
            {"SA", "Salem Junction", "Salem", "Tamil Nadu"},
            {"ED", "Erode Junction", "Erode", "Tamil Nadu"},
            {"TUP", "Tiruppur", "Tiruppur", "Tamil Nadu"},
            {"CBE", "Coimbatore Junction", "Coimbatore", "Tamil Nadu"},
            {"CAPE", "Kanniyakumari", "Kanniyakumari", "Tamil Nadu"},
            {"PGT", "Palakkad Junction", "Palakkad", "Kerala"},
            {"TCR", "Thrissur", "Thrissur", "Kerala"},
            {"ERS", "Ernakulam Junction", "Kochi", "Kerala"},
            {"KTYM", "Kottayam", "Kottayam", "Kerala"},
            {"QLN", "Kollam Junction", "Kollam", "Kerala"},
            {"TVC", "Thiruvananthapuram Central", "Thiruvananthapuram", "Kerala"},
            {"SBC", "KSR Bengaluru City", "Bengaluru", "Karnataka"},
            {"BNC", "Bengaluru Cantonment", "Bengaluru", "Karnataka"},
            {"YPR", "Yesvantpur Junction", "Bengaluru", "Karnataka"},
            {"KJM", "Krishnarajapuram", "Bengaluru", "Karnataka"},
            {"BWT", "Bangarapet Junction", "Bangarapet", "Karnataka"},
            {"MYA", "Mandya", "Mandya", "Karnataka"},
            {"MYS", "Mysuru Junction", "Mysuru", "Karnataka"},
            {"RC", "Raichur", "Raichur", "Karnataka"},
            {"WADI", "Wadi Junction", "Wadi", "Karnataka"},
            {"KLBG", "Kalaburagi", "Kalaburagi", "Karnataka"},
            {"DMM", "Dharmavaram Junction", "Dharmavaram", "Andhra Pradesh"},
            {"ATP", "Anantapur", "Anantapur", "Andhra Pradesh"},
            {"GTL", "Guntakal Junction", "Guntakal", "Andhra Pradesh"},
            {"KRNT", "Kurnool City", "Kurnool", "Andhra Pradesh"},
            {"GDR", "Gudur Junction", "Gudur", "Andhra Pradesh"},
            {"NLR", "Nellore", "Nellore", "Andhra Pradesh"},
            {"OGL", "Ongole", "Ongole", "Andhra Pradesh"},
            {"GNT", "Guntur Junction", "Guntur", "Andhra Pradesh"},
            {"BZA", "Vijayawada Junction", "Vijayawada", "Andhra Pradesh"},
            {"RJY", "Rajahmundry", "Rajahmundry", "Andhra Pradesh"},
            {"VSKP", "Visakhapatnam", "Visakhapatnam", "Andhra Pradesh"},
            {"SC", "Secunderabad Junction", "Hyderabad", "Telangana"},
            {"HYB", "Hyderabad Deccan", "Hyderabad", "Telangana"},
            {"NLDA", "Nalgonda", "Nalgonda", "Telangana"},
            {"KZJ", "Kazipet Junction", "Warangal", "Telangana"},
            {"WL", "Warangal", "Warangal", "Telangana"},
            {"BPQ", "Balharshah", "Balharshah", "Maharashtra"},
            {"NGP", "Nagpur Junction", "Nagpur", "Maharashtra"},
            {"SUR", "Solapur", "Solapur", "Maharashtra"},
            {"DD", "Daund Junction", "Daund", "Maharashtra"},
            {"PUNE", "Pune Junction", "Pune", "Maharashtra"},
            {"LNL", "Lonavala", "Lonavala", "Maharashtra"},
            {"KYN", "Kalyan Junction", "Kalyan", "Maharashtra"},
            {"TNA", "Thane", "Thane", "Maharashtra"},
            {"DR", "Dadar", "Mumbai", "Maharashtra"},
            {"CSMT", "Mumbai CSMT", "Mumbai", "Maharashtra"},
            {"MMCT", "Mumbai Central", "Mumbai", "Maharashtra"},
            {"BVI", "Borivali", "Mumbai", "Maharashtra"},
            {"PNVL", "Panvel", "Navi Mumbai", "Maharashtra"},
            {"RN", "Ratnagiri", "Ratnagiri", "Maharashtra"},
            {"KUDL", "Kudal", "Kudal", "Maharashtra"},
            {"MMR", "Manmad Junction", "Manmad", "Maharashtra"},
            {"BSL", "Bhusaval Junction", "Bhusaval", "Maharashtra"},
            {"THVM", "Thivim", "Thivim", "Goa"},
            {"MAO", "Madgaon Junction", "Madgaon", "Goa"},
            {"ST", "Surat", "Surat", "Gujarat"},
            {"BRC", "Vadodara Junction", "Vadodara", "Gujarat"},
            {"ADI", "Ahmedabad Junction", "Ahmedabad", "Gujarat"},
            {"RTM", "Ratlam Junction", "Ratlam", "Madhya Pradesh"},
            {"ET", "Itarsi Junction", "Itarsi", "Madhya Pradesh"},
            {"BPL", "Bhopal Junction", "Bhopal", "Madhya Pradesh"},
            {"RKMP", "Rani Kamlapati", "Bhopal", "Madhya Pradesh"},
            {"GWL", "Gwalior Junction", "Gwalior", "Madhya Pradesh"},
            {"KOTA", "Kota Junction", "Kota", "Rajasthan"},
            {"JHS", "Virangana Lakshmibai Jhansi", "Jhansi", "Uttar Pradesh"},
            {"AGC", "Agra Cantt", "Agra", "Uttar Pradesh"},
            {"MTJ", "Mathura Junction", "Mathura", "Uttar Pradesh"},
            {"GZB", "Ghaziabad", "Ghaziabad", "Uttar Pradesh"},
            {"ALJN", "Aligarh Junction", "Aligarh", "Uttar Pradesh"},
            {"CNB", "Kanpur Central", "Kanpur", "Uttar Pradesh"},
            {"LKO", "Lucknow Charbagh", "Lucknow", "Uttar Pradesh"},
            {"PRYJ", "Prayagraj Junction", "Prayagraj", "Uttar Pradesh"},
            {"BSB", "Varanasi Junction", "Varanasi", "Uttar Pradesh"},
            {"DDU", "Pt. Deen Dayal Upadhyaya Junction", "Chandauli", "Uttar Pradesh"},
            {"NDLS", "New Delhi", "Delhi", "Delhi"},
            {"NZM", "Hazrat Nizamuddin", "Delhi", "Delhi"},
            {"UMB", "Ambala Cantt", "Ambala", "Haryana"},
            {"LDH", "Ludhiana Junction", "Ludhiana", "Punjab"},
            {"JAT", "Jammu Tawi", "Jammu", "Jammu and Kashmir"},
            {"SVDK", "Shri Mata Vaishno Devi Katra", "Katra", "Jammu and Kashmir"},
            {"BXR", "Buxar", "Buxar", "Bihar"},
            {"ARA", "Ara Junction", "Ara", "Bihar"},
            {"PNBE", "Patna Junction", "Patna", "Bihar"},
            {"RJPB", "Rajendra Nagar Terminal", "Patna", "Bihar"},
            {"GAYA", "Gaya Junction", "Gaya", "Bihar"},
            {"DHN", "Dhanbad Junction", "Dhanbad", "Jharkhand"},
            {"HWH", "Howrah Junction", "Kolkata", "West Bengal"},
            {"KGP", "Kharagpur Junction", "Kharagpur", "West Bengal"},
            {"BLS", "Balasore", "Balasore", "Odisha"},
            {"CTC", "Cuttack", "Cuttack", "Odisha"},
            {"BBS", "Bhubaneswar", "Bhubaneswar", "Odisha"},
            {"PURI", "Puri", "Puri", "Odisha"},
            {"BAM", "Brahmapur", "Brahmapur", "Odisha"},
    };

    private Map<String, Station> seedStations() {
        Map<String, Station> out = new LinkedHashMap<>();
        for (String[] s : STATIONS) {
            out.put(s[0], stationRepository.save(new Station(s[0], s[1], s[2], s[3])));
        }
        return out;
    }

    // ------------------------------------------------------------------ trains

    /** A stop: station code, distance from origin, running minutes from the previous departure, halt minutes. */
    private record S(String code, int km, int run, int halt) {}

    private record Spec(String number, String name, TrainType type, String days, boolean pantry, String classes,
                        String start, List<S> stops) {

        /** The return working: same stations reversed, the same leg running times, its own start time and days. */
        Spec reverse(String number, String name, String start, String days) {
            int total = stops.getLast().km();
            List<S> rev = new ArrayList<>();
            for (int i = stops.size() - 1; i >= 0; i--) {
                S s = stops.get(i);
                int run = i == stops.size() - 1 ? 0 : stops.get(i + 1).run();
                rev.add(new S(s.code(), total - s.km(), run, s.halt()));
            }
            return new Spec(number, name, type, days, pantry, classes, start, rev);
        }
    }

    private static S o(String code) { return new S(code, 0, 0, 0); }
    private static S s(String code, int km, int run, int halt) { return new S(code, km, run, halt); }
    private static S t(String code, int km, int run) { return new S(code, km, run, 0); }

    private static final String DAILY = "1111111";

    private List<Spec> specs() {
        List<Spec> out = new ArrayList<>();
        Spec x;

        x = new Spec("12007", "Mysuru Shatabdi Express", TrainType.SHATABDI, "1101111", false, "CC:8,EC:1", "06:00",
                List.of(o("MAS"), s("KPD", 129, 83, 2), s("BNC", 352, 178, 2), s("SBC", 358, 20, 10),
                        s("MYA", 450, 63, 2), t("MYS", 497, 50)));
        out.add(x); out.add(x.reverse("12008", "Mysuru Chennai Shatabdi Express", "14:15", "1101111"));

        x = new Spec("12027", "Bengaluru Shatabdi Express", TrainType.SHATABDI, "1011111", false, "CC:7,EC:1", "17:30",
                List.of(o("MAS"), s("KPD", 129, 83, 2), s("BNC", 352, 205, 2), t("SBC", 358, 18)));
        out.add(x); out.add(x.reverse("12028", "Chennai Shatabdi Express", "06:00", "1011111"));

        x = new Spec("20607", "Chennai Mysuru Vande Bharat Express", TrainType.VANDE_BHARAT, "1101111", false, "CC:7,EC:1", "05:50",
                List.of(o("MAS"), s("KPD", 129, 83, 2), s("SBC", 358, 185, 5), t("MYS", 497, 115)));
        out.add(x); out.add(x.reverse("20608", "Mysuru Chennai Vande Bharat Express", "13:05", "1101111"));

        x = new Spec("12639", "Brindavan Express", TrainType.SUPERFAST, DAILY, false, "2S:10,CC:2", "07:50",
                List.of(o("MAS"), s("AJJ", 68, 58, 2), s("KPD", 129, 48, 2), s("JTJ", 214, 73, 2), s("BWT", 290, 83, 2),
                        s("KJM", 344, 50, 2), s("BNC", 352, 18, 2), t("SBC", 358, 18)));
        out.add(x); out.add(x.reverse("12640", "Brindavan Express", "15:00", DAILY));

        x = new Spec("12657", "Chennai Bengaluru Mail", TrainType.MAIL_EXPRESS, DAILY, false, "SL:10,3A:4,2A:2,1A:1", "22:40",
                List.of(o("MAS"), s("AJJ", 68, 58, 2), s("KPD", 129, 48, 2), s("JTJ", 214, 78, 2), s("BWT", 290, 83, 2),
                        s("KJM", 344, 58, 2), s("BNC", 352, 18, 2), t("SBC", 358, 20)));
        out.add(x); out.add(x.reverse("12658", "Bengaluru Chennai Mail", "22:40", DAILY));

        x = new Spec("12675", "Kovai Express", TrainType.SUPERFAST, DAILY, false, "2S:12,CC:3", "06:15",
                List.of(o("MAS"), s("AJJ", 68, 58, 2), s("KPD", 129, 48, 2), s("JTJ", 214, 73, 2), s("SA", 334, 110, 5),
                        s("ED", 394, 60, 5), s("TUP", 444, 45, 2), t("CBE", 494, 58)));
        out.add(x); out.add(x.reverse("12676", "Kovai Express", "15:15", DAILY));

        x = new Spec("12673", "Cheran Superfast Express", TrainType.SUPERFAST, DAILY, false, "SL:10,3A:4,2A:2", "22:00",
                List.of(o("MAS"), s("AJJ", 68, 58, 2), s("KPD", 129, 48, 2), s("JTJ", 214, 78, 2), s("SA", 334, 115, 5),
                        s("ED", 394, 65, 5), s("TUP", 444, 48, 2), t("CBE", 494, 65)));
        out.add(x); out.add(x.reverse("12674", "Cheran Superfast Express", "22:30", DAILY));

        x = new Spec("12243", "Coimbatore Shatabdi Express", TrainType.SHATABDI, "1011111", false, "CC:7,EC:1", "07:15",
                List.of(o("MAS"), s("KPD", 129, 83, 2), s("JTJ", 214, 68, 2), s("SA", 334, 92, 3), s("ED", 394, 53, 2),
                        s("TUP", 444, 40, 2), t("CBE", 494, 43)));
        out.add(x); out.add(x.reverse("12244", "Chennai Shatabdi Express", "15:05", "1011111"));

        x = new Spec("12695", "Chennai Thiruvananthapuram Superfast Express", TrainType.SUPERFAST, DAILY, true,
                "SL:10,3A:5,2A:2", "15:20",
                List.of(o("MAS"), s("AJJ", 68, 58, 2), s("KPD", 129, 48, 2), s("JTJ", 214, 75, 5), s("SA", 334, 110, 5),
                        s("ED", 394, 60, 5), s("TUP", 444, 45, 2), s("CBE", 494, 58, 5), s("PGT", 548, 75, 5),
                        s("TCR", 624, 85, 3), s("ERS", 698, 87, 5), s("KTYM", 762, 75, 3), s("QLN", 860, 102, 3),
                        t("TVC", 925, 87)));
        out.add(x); out.add(x.reverse("12696", "Thiruvananthapuram Chennai Superfast Express", "17:15", DAILY));

        x = new Spec("16526", "Island Express", TrainType.MAIL_EXPRESS, DAILY, true, "2S:2,SL:10,3A:4,2A:1", "20:00",
                List.of(o("SBC"), s("BNC", 5, 12, 3), s("KJM", 13, 18, 2), s("BWT", 69, 50, 2), s("JTJ", 145, 78, 5),
                        s("SA", 265, 115, 5), s("ED", 325, 70, 5), s("TUP", 375, 45, 2), s("CBE", 425, 63, 5),
                        s("PGT", 479, 80, 5), s("TCR", 555, 100, 3), s("ERS", 629, 97, 5), s("KTYM", 693, 80, 3),
                        s("QLN", 791, 112, 3), s("TVC", 856, 92, 5), t("CAPE", 943, 135)));
        out.add(x); out.add(x.reverse("16525", "Island Express", "10:00", DAILY));

        x = new Spec("12759", "Charminar Express", TrainType.SUPERFAST, DAILY, true, "SL:10,3A:5,2A:2,1A:1", "18:00",
                List.of(o("MAS"), s("GDR", 138, 123, 2), s("NLR", 176, 38, 2), s("OGL", 292, 103, 2), s("GNT", 410, 145, 5),
                        s("NLDA", 561, 180, 2), s("SC", 711, 168, 10), t("HYB", 721, 30)));
        out.add(x); out.add(x.reverse("12760", "Charminar Express", "18:30", DAILY));

        x = new Spec("12621", "Tamil Nadu Express", TrainType.SUPERFAST, DAILY, true, "SL:10,3A:5,2A:2,1A:1", "22:00",
                List.of(o("MAS"), s("BZA", 431, 380, 15), s("WL", 640, 175, 2), s("BPQ", 874, 188, 5),
                        s("NGP", 1084, 205, 10), s("ET", 1381, 305, 5), s("BPL", 1473, 90, 10), s("JHS", 1764, 230, 8),
                        s("GWL", 1861, 72, 2), s("AGC", 1979, 93, 5), s("MTJ", 2033, 45, 2), t("NDLS", 2174, 133)));
        out.add(x); out.add(x.reverse("12622", "Tamil Nadu Express", "22:30", DAILY));

        x = new Spec("12433", "Chennai Rajdhani Express", TrainType.RAJDHANI, "0000101", true, "3A:6,2A:3,1A:1", "06:10",
                List.of(o("MAS"), s("BZA", 431, 370, 10), s("WL", 640, 170, 2), s("BPQ", 874, 168, 5),
                        s("NGP", 1084, 185, 10), s("BPL", 1473, 340, 10), s("JHS", 1764, 200, 8), s("AGC", 1979, 132, 5),
                        t("NZM", 2176, 190)));
        out.add(x); out.add(x.reverse("12434", "Chennai Rajdhani Express", "15:55", "0010100"));

        x = new Spec("12627", "Karnataka Express", TrainType.SUPERFAST, DAILY, true, "SL:10,3A:5,2A:2,1A:1", "19:20",
                List.of(o("SBC"), s("YPR", 6, 15, 2), s("DMM", 195, 153, 2), s("ATP", 232, 33, 2), s("GTL", 304, 98, 10),
                        s("RC", 438, 130, 2), s("WADI", 547, 103, 5), s("SUR", 664, 150, 10), s("DD", 851, 195, 5),
                        s("MMR", 1112, 300, 5), s("BSL", 1296, 160, 10), s("ET", 1593, 300, 5), s("BPL", 1685, 95, 10),
                        s("JHS", 1976, 220, 8), s("GWL", 2073, 77, 2), s("AGC", 2191, 93, 5), t("NDLS", 2385, 190)));
        out.add(x); out.add(x.reverse("12628", "Karnataka Express", "21:15", DAILY));

        x = new Spec("22691", "KSR Bengaluru Rajdhani Express", TrainType.RAJDHANI, DAILY, true, "3A:6,2A:3,1A:1", "20:00",
                List.of(o("SBC"), s("YPR", 6, 15, 2), s("DMM", 195, 161, 2), s("ATP", 232, 33, 2), s("KRNT", 382, 125, 2),
                        s("SC", 610, 243, 10), s("KZJ", 742, 120, 2), s("BPQ", 976, 193, 5), s("NGP", 1185, 185, 10),
                        s("BPL", 1575, 345, 10), s("JHS", 1866, 195, 8), s("AGC", 2081, 132, 5), t("NZM", 2269, 195)));
        out.add(x); out.add(x.reverse("22692", "KSR Bengaluru Rajdhani Express", "20:50", DAILY));

        x = new Spec("12951", "Mumbai Rajdhani Express", TrainType.RAJDHANI, DAILY, true, "3A:8,2A:4,1A:1", "17:00",
                List.of(o("MMCT"), s("BVI", 30, 22, 2), s("ST", 263, 139, 5), s("BRC", 392, 78, 10), s("RTM", 653, 192, 3),
                        s("KOTA", 921, 164, 10), t("NDLS", 1386, 307)));
        out.add(x); out.add(x.reverse("12952", "Mumbai Rajdhani Express", "16:55", DAILY));

        x = new Spec("12953", "August Kranti Rajdhani Express", TrainType.RAJDHANI, DAILY, true, "3A:7,2A:3,1A:1", "17:40",
                List.of(o("MMCT"), s("BVI", 30, 23, 2), s("ST", 263, 127, 5), s("BRC", 392, 85, 10), s("RTM", 653, 193, 3),
                        s("KOTA", 921, 177, 10), s("MTJ", 1265, 225, 2), t("NZM", 1377, 173)));
        out.add(x); out.add(x.reverse("12954", "August Kranti Rajdhani Express", "16:55", DAILY));

        x = new Spec("12123", "Deccan Queen", TrainType.SUPERFAST, DAILY, false, "2S:12,CC:4", "17:10",
                List.of(o("CSMT"), s("DR", 9, 14, 2), s("TNA", 34, 20, 2), s("KYN", 54, 20, 2), s("LNL", 128, 92, 2),
                        t("PUNE", 192, 41)));
        out.add(x); out.add(x.reverse("12124", "Deccan Queen", "07:15", DAILY));

        x = new Spec("12127", "Mumbai Pune Intercity Express", TrainType.SUPERFAST, DAILY, false, "2S:12,CC:3", "06:40",
                List.of(o("CSMT"), s("DR", 9, 12, 2), s("TNA", 34, 21, 2), s("KYN", 54, 20, 3), s("LNL", 128, 85, 2),
                        t("PUNE", 192, 50)));
        out.add(x); out.add(x.reverse("12128", "Pune Mumbai Intercity Express", "17:55", DAILY));

        x = new Spec("12009", "Ahmedabad Shatabdi Express", TrainType.SHATABDI, "1111110", false, "CC:8,EC:1", "06:20",
                List.of(o("MMCT"), s("BVI", 30, 22, 2), s("ST", 263, 136, 5), s("BRC", 392, 100, 5), t("ADI", 491, 115)));
        out.add(x); out.add(x.reverse("12010", "Mumbai Shatabdi Express", "14:40", "1111110"));

        x = new Spec("10103", "Mandovi Express", TrainType.MAIL_EXPRESS, DAILY, true, "2S:4,SL:8,3A:3,2A:1", "07:10",
                List.of(o("CSMT"), s("TNA", 34, 37, 3), s("PNVL", 69, 45, 5), s("RN", 335, 280, 5), s("KUDL", 493, 175, 2),
                        s("THVM", 530, 53, 2), t("MAO", 588, 88)));
        out.add(x); out.add(x.reverse("10104", "Mandovi Express", "09:00", DAILY));

        x = new Spec("12051", "Mumbai Madgaon Jan Shatabdi Express", TrainType.JAN_SHATABDI, DAILY, false, "2S:8,CC:3", "05:10",
                List.of(o("CSMT"), s("DR", 9, 13, 2), s("TNA", 34, 20, 2), s("PNVL", 69, 43, 5), s("RN", 335, 250, 5),
                        s("KUDL", 493, 130, 2), s("THVM", 530, 43, 2), t("MAO", 588, 58)));
        out.add(x); out.add(x.reverse("12052", "Madgaon Mumbai Jan Shatabdi Express", "15:00", DAILY));

        x = new Spec("22439", "New Delhi Katra Vande Bharat Express", TrainType.VANDE_BHARAT, "1011111", false, "CC:14,EC:2", "06:00",
                List.of(o("NDLS"), s("UMB", 199, 116, 2), s("LDH", 313, 60, 2), s("JAT", 577, 218, 2), t("SVDK", 655, 80)));
        out.add(x); out.add(x.reverse("22440", "Katra New Delhi Vande Bharat Express", "15:00", "1011111"));

        x = new Spec("22435", "New Delhi Varanasi Vande Bharat Express", TrainType.VANDE_BHARAT, "0110111", false, "CC:14,EC:2", "06:00",
                List.of(o("NDLS"), s("CNB", 440, 248, 2), s("PRYJ", 634, 118, 2), t("BSB", 759, 110)));
        out.add(x); out.add(x.reverse("22436", "Varanasi New Delhi Vande Bharat Express", "15:00", "0110111"));

        x = new Spec("12301", "Howrah Rajdhani Express", TrainType.RAJDHANI, DAILY, true, "3A:8,2A:4,1A:1", "16:50",
                List.of(o("HWH"), s("DHN", 259, 186, 5), s("GAYA", 458, 147, 3), s("DDU", 661, 154, 10), s("PRYJ", 814, 125, 2),
                        s("CNB", 1008, 123, 5), t("NDLS", 1450, 270)));
        out.add(x); out.add(x.reverse("12302", "Howrah Rajdhani Express", "16:50", DAILY));

        x = new Spec("12309", "Patna Rajdhani Express", TrainType.RAJDHANI, DAILY, true, "3A:7,2A:3,1A:1", "19:10",
                List.of(o("RJPB"), s("PNBE", 5, 12, 3), s("ARA", 55, 48, 2), s("BXR", 122, 48, 2), s("DDU", 216, 110, 10),
                        s("PRYJ", 369, 130, 2), s("CNB", 563, 128, 5), t("NDLS", 1005, 270)));
        out.add(x); out.add(x.reverse("12310", "Patna Rajdhani Express", "17:05", DAILY));

        x = new Spec("12841", "Coromandel Express", TrainType.SUPERFAST, DAILY, true, "2S:2,SL:10,3A:5,2A:2", "14:50",
                List.of(o("HWH"), s("KGP", 116, 85, 5), s("BLS", 235, 98, 2), s("CTC", 409, 133, 2), s("BBS", 437, 40, 5),
                        s("BAM", 603, 140, 5), s("VSKP", 881, 255, 20), s("RJY", 1081, 185, 2), s("BZA", 1231, 123, 10),
                        s("OGL", 1371, 120, 2), s("NLR", 1487, 108, 2), s("GDR", 1525, 38, 2), t("MAS", 1662, 178)));
        out.add(x); out.add(x.reverse("12842", "Coromandel Express", "08:45", DAILY));

        x = new Spec("12821", "Dhauli Express", TrainType.SUPERFAST, DAILY, false, "2S:8,CC:2", "06:00",
                List.of(o("HWH"), s("KGP", 116, 90, 2), s("BLS", 235, 88, 2), s("CTC", 409, 123, 2), s("BBS", 437, 38, 5),
                        t("PURI", 499, 100)));
        out.add(x); out.add(x.reverse("12822", "Dhauli Express", "14:00", DAILY));

        x = new Spec("12049", "Gatimaan Express", TrainType.GATIMAAN, "1111011", false, "CC:8,EC:2", "08:10",
                List.of(o("NZM"), s("AGC", 188, 100, 5), s("GWL", 306, 75, 2), t("JHS", 403, 83)));
        out.add(x); out.add(x.reverse("12050", "Gatimaan Express", "15:05", "1111011"));

        x = new Spec("12002", "Bhopal Shatabdi Express", TrainType.SHATABDI, DAILY, false, "CC:10,EC:2", "06:00",
                List.of(o("NDLS"), s("MTJ", 141, 79, 1), s("AGC", 195, 30, 5), s("GWL", 313, 88, 2), s("JHS", 410, 78, 5),
                        s("BPL", 701, 217, 5), t("RKMP", 707, 10)));
        out.add(x); out.add(x.reverse("12001", "Bhopal Shatabdi Express", "15:15", DAILY));

        x = new Spec("12004", "Lucknow Shatabdi Express", TrainType.SHATABDI, DAILY, false, "CC:10,EC:2", "06:10",
                List.of(o("NDLS"), s("GZB", 25, 36, 2), s("ALJN", 131, 74, 2), s("CNB", 440, 144, 5), t("LKO", 512, 82)));
        out.add(x); out.add(x.reverse("12003", "Lucknow Shatabdi Express", "15:35", DAILY));

        x = new Spec("12025", "Pune Secunderabad Shatabdi Express", TrainType.SHATABDI, "1011111", false, "CC:8,EC:1", "14:15",
                List.of(o("PUNE"), s("DD", 77, 68, 2), s("SUR", 264, 130, 5), s("KLBG", 376, 103, 2), s("WADI", 413, 28, 2),
                        t("SC", 607, 170)));
        out.add(x); out.add(x.reverse("12026", "Secunderabad Pune Shatabdi Express", "05:50", "1011111"));

        x = new Spec("12723", "Telangana Express", TrainType.SUPERFAST, DAILY, true, "SL:10,3A:5,2A:2", "06:00",
                List.of(o("HYB"), s("SC", 10, 25, 5), s("KZJ", 142, 115, 5), s("BPQ", 376, 180, 10), s("NGP", 586, 185, 10),
                        s("ET", 883, 315, 5), s("BPL", 975, 90, 10), s("JHS", 1266, 240, 10), s("GWL", 1363, 70, 2),
                        s("AGC", 1481, 98, 5), t("NDLS", 1676, 195)));
        out.add(x); out.add(x.reverse("12724", "Telangana Express", "16:00", DAILY));

        return out;
    }

    private int seedTrains(Map<String, Station> stations) {
        int n = 0;
        for (Spec spec : specs()) {
            trainRepository.save(build(spec, stations));
            n++;
        }
        return n;
    }

    private Train build(Spec spec, Map<String, Station> stations) {
        Train train = new Train(spec.number(), spec.name(), spec.type(), spec.days(), spec.pantry());
        for (String part : spec.classes().split(",")) {
            String[] kv = part.split(":");
            CoachClass cc = CoachClass.fromCode(kv[0]).orElseThrow();
            train.addClass(new TrainClass(cc, Integer.parseInt(kv[1])));
        }
        LocalTime start = LocalTime.parse(spec.start());
        int clock = start.getHour() * 60 + start.getMinute();
        List<S> stops = spec.stops();
        for (int i = 0; i < stops.size(); i++) {
            S st = stops.get(i);
            Station station = Objects.requireNonNull(stations.get(st.code()), "Unknown station " + st.code());
            Integer arrival = null, departure = null;
            if (i == 0) {
                departure = clock;
            } else {
                clock += st.run();
                arrival = clock;
                if (i < stops.size() - 1) {
                    clock += st.halt();
                    departure = clock;
                }
            }
            train.addStop(new TrainStop(i + 1, station, arrival, departure, st.km()));
        }
        return train;
    }
}
