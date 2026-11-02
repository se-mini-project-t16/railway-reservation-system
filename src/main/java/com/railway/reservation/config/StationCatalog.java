package com.railway.reservation.config;

import com.railway.reservation.dto.StationDto;

import java.util.*;

/**
 * Authoritative master railway station catalog and alias resolver (RRS-F-004-EXT).
 * Contains official 3/4-letter Indian Railway station codes, full station names,
 * cities, states, and railway operating zones.
 */
public final class StationCatalog {

    public static final String CODE_MAS = "MAS";
    public static final String CODE_SBC = "SBC";
    public static final String CODE_NDLS = "NDLS";
    public static final String CODE_MMCT = "MMCT";
    public static final String CODE_HWH = "HWH";
    public static final String CODE_PUNE = "PUNE";

    private static final Map<String, StationDto> STATIONS = new LinkedHashMap<>();
    private static final Map<String, Set<String>> ALIAS_GROUPS = new HashMap<>();

    static {
        registerStation("MAS", "Chennai Central", "Chennai", "Tamil Nadu", "SR",
                List.of("chennai", "chennai central", "mas", "mgr chennai central"));
        registerStation("SBC", "KSR Bengaluru", "Bengaluru", "Karnataka", "SWR",
                List.of("bengaluru", "bangalore", "ksr bengaluru", "sbc", "bangalore city"));
        registerStation("NDLS", "New Delhi", "New Delhi", "Delhi", "NR",
                List.of("delhi", "new delhi", "ndls", "dli"));
        registerStation("MMCT", "Mumbai Central", "Mumbai", "Maharashtra", "WR",
                List.of("mumbai", "mumbai central", "mmct", "bct", "bombay"));
        registerStation("HWH", "Howrah Junction", "Kolkata", "West Bengal", "ER",
                List.of("howrah", "howrah junction", "hwh", "kolkata"));
        registerStation("PUNE", "Pune Junction", "Pune", "Maharashtra", "CR",
                List.of("pune", "pune junction"));

        // Intermediate halt stations
        registerStation("KPD", "Katpadi Junction", "Vellore", "Tamil Nadu", "SR",
                List.of("katpadi", "katpadi junction", "kpd"));
        registerStation("JTJ", "Jolarpettai Junction", "Jolarpettai", "Tamil Nadu", "SR",
                List.of("jolarpettai", "jolarpettai junction", "jtj"));
        registerStation("AJJ", "Arakkonam Junction", "Arakkonam", "Tamil Nadu", "SR",
                List.of("arakkonam", "arakkonam junction", "ajj"));
        registerStation("ST", "Surat", "Surat", "Gujarat", "WR",
                List.of("surat", "st"));
        registerStation("BRC", "Vadodara Junction", "Vadodara", "Gujarat", "WR",
                List.of("vadodara", "vadodara junction", "brc", "baroda"));
        registerStation("RTM", "Ratlam Junction", "Ratlam", "Madhya Pradesh", "WR",
                List.of("ratlam", "ratlam junction", "rtm"));
        registerStation("KOTA", "Kota Junction", "Kota", "Rajasthan", "WCR",
                List.of("kota", "kota junction"));
        registerStation("LNL", "Lonavala", "Lonavala", "Maharashtra", "CR",
                List.of("lonavala", "lnl"));
        registerStation("KJT", "Karjat Junction", "Karjat", "Maharashtra", "CR",
                List.of("karjat", "karjat junction", "kjt"));
        registerStation("CNB", "Kanpur Central", "Kanpur", "Uttar Pradesh", "NCR",
                List.of("kanpur", "kanpur central", "cnb"));
        registerStation("PRYJ", "Prayagraj Junction", "Prayagraj", "Uttar Pradesh", "NCR",
                List.of("prayagraj", "prayagraj junction", "pryj", "allahabad"));
        registerStation("DDU", "Pt. Deen Dayal Upadhyaya Junction", "Mughalsarai", "Uttar Pradesh", "ECR",
                List.of("ddu", "deen dayal upadhyaya", "mughalsarai"));
        registerStation("GAYA", "Gaya Junction", "Gaya", "Bihar", "ECR",
                List.of("gaya", "gaya junction"));
        registerStation("DHN", "Dhanbad Junction", "Dhanbad", "Jharkhand", "ECR",
                List.of("dhanbad", "dhanbad junction", "dhn"));
        registerStation("ASN", "Asansol Junction", "Asansol", "West Bengal", "ER",
                List.of("asansol", "asansol junction", "asn"));
    }

    private StationCatalog() {}

    private static void registerStation(String code, String name, String city, String state, String zone, List<String> aliases) {
        StationDto dto = new StationDto(code, name, city, state, zone);
        STATIONS.put(code, dto);

        Set<String> allVariants = new LinkedHashSet<>();
        allVariants.add(code.toLowerCase());
        allVariants.add(name.toLowerCase());
        allVariants.add(city.toLowerCase());
        for (String a : aliases) {
            allVariants.add(a.toLowerCase());
        }

        for (String variant : allVariants) {
            ALIAS_GROUPS.put(variant, allVariants);
        }
    }

    public static List<StationDto> getAllStations() {
        return new ArrayList<>(STATIONS.values());
    }

    public static Optional<StationDto> findByCode(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(STATIONS.get(code.trim().toUpperCase()));
    }

    /**
     * Expands a user query into all equivalent station names and codes (e.g. 'MAS' -> ['mas', 'chennai', 'chennai central']).
     * If station is not recognized in catalog, returns the trimmed lowercase query itself.
     */
    public static List<String> expandAliases(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String clean = input.trim().toLowerCase();
        Set<String> matched = ALIAS_GROUPS.get(clean);
        if (matched != null) {
            return new ArrayList<>(matched);
        }
        return List.of(clean);
    }

    public static boolean isKnownStation(String query) {
        if (query == null || query.trim().isEmpty()) return false;
        return ALIAS_GROUPS.containsKey(query.trim().toLowerCase());
    }
}
