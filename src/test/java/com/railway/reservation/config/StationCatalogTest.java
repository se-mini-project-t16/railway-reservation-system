package com.railway.reservation.config;

import com.railway.reservation.dto.StationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying master railway station catalog and timetable constants (RRS-F-004-EXT, TC-SCH-01).
 */
class StationCatalogTest {

    @Test
    @DisplayName("TC-SCH-01: Master catalog contains official 3/4-letter Indian Railway station codes")
    void testOfficialStationCodesConfigured() {
        assertThat(StationCatalog.CODE_MAS).isEqualTo("MAS");
        assertThat(StationCatalog.CODE_SBC).isEqualTo("SBC");
        assertThat(StationCatalog.CODE_NDLS).isEqualTo("NDLS");
        assertThat(StationCatalog.CODE_MMCT).isEqualTo("MMCT");
        assertThat(StationCatalog.CODE_HWH).isEqualTo("HWH");
        assertThat(StationCatalog.CODE_PUNE).isEqualTo("PUNE");

        assertThat(StationCatalog.findByCode("MAS")).isPresent();
        assertThat(StationCatalog.findByCode("SBC")).isPresent();
        assertThat(StationCatalog.findByCode("NDLS")).isPresent();
        assertThat(StationCatalog.findByCode("MMCT")).isPresent();
        assertThat(StationCatalog.findByCode("HWH")).isPresent();
        assertThat(StationCatalog.findByCode("PUNE")).isPresent();
    }

    @Test
    @DisplayName("TC-SCH-01: Intermediate halt stations are registered with valid metadata")
    void testIntermediateHaltStationsConfigured() {
        List<String> intermediateCodes = List.of("KPD", "JTJ", "AJJ", "ST", "BRC", "RTM", "KOTA", "LNL", "KJT", "CNB", "PRYJ", "DDU", "GAYA", "DHN", "ASN");

        for (String code : intermediateCodes) {
            Optional<StationDto> station = StationCatalog.findByCode(code);
            assertThat(station).as("Station with code " + code + " should be registered").isPresent();
            assertThat(station.get().getName()).isNotBlank();
            assertThat(station.get().getCity()).isNotBlank();
            assertThat(station.get().getState()).isNotBlank();
            assertThat(station.get().getZone()).isNotBlank();
        }
    }

    @Test
    @DisplayName("TC-SCH-01: Station alias resolver expands codes and city names bidirectionally")
    void testStationAliasExpansion() {
        List<String> masAliases = StationCatalog.expandAliases("MAS");
        assertThat(masAliases).contains("mas", "chennai", "chennai central");

        List<String> chennaiAliases = StationCatalog.expandAliases("chennai");
        assertThat(chennaiAliases).contains("mas", "chennai");

        List<String> sbcAliases = StationCatalog.expandAliases("SBC");
        assertThat(sbcAliases).contains("sbc", "bengaluru", "bangalore");

        List<String> ndlsAliases = StationCatalog.expandAliases("NDLS");
        assertThat(ndlsAliases).contains("ndls", "delhi", "new delhi");

        List<String> mmctAliases = StationCatalog.expandAliases("MMCT");
        assertThat(mmctAliases).contains("mmct", "mumbai", "mumbai central");

        // Unknown station returns fallback query
        List<String> unknown = StationCatalog.expandAliases("UNKNOWN_STN");
        assertThat(unknown).containsExactly("unknown_stn");
    }

    @Test
    @DisplayName("TC-SCH-01: Catalog getAllStations returns all stations without empty fields")
    void testGetAllStationsCompleteness() {
        List<StationDto> allStations = StationCatalog.getAllStations();
        assertThat(allStations).hasSizeGreaterThanOrEqualTo(20);

        for (StationDto s : allStations) {
            assertThat(s.getCode()).matches("^[A-Z]{2,5}$");
            assertThat(s.getName()).isNotBlank();
            assertThat(s.getCity()).isNotBlank();
            assertThat(s.getState()).isNotBlank();
            assertThat(s.getZone()).isNotBlank();
        }
    }
}
