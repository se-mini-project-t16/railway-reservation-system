package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.repository.CancellationRuleRepository;
import com.railway.reservation.repository.StationRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.support.MutableClock;
import com.railway.reservation.support.RailFixtures;
import com.railway.reservation.support.TestClockConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Search direction, input and date boundary checks for the seeded test network. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class SearchBoundaryTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CancellationRuleRepository rules;
    @Autowired StationRepository stationRepository;
    @Autowired TrainRepository trainRepository;
    @Autowired MutableClock clock;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    @BeforeEach
    void setUp() {
        clock.set(MONDAY.atTime(9, 0));
        RailFixtures.reset(jdbc, rules);
        var stations = RailFixtures.stations(stationRepository);
        RailFixtures.superfast(stations, trainRepository);
        RailFixtures.weekday(stations, trainRepository);
    }

    @AfterEach
    void tearDown() {
        clock.reset();
        RailFixtures.reset(jdbc, rules);
    }

    private ResultActions search(String from, String to, LocalDate date) throws Exception {
        return mvc.perform(get("/api/v1/trains/search")
                .param("from", from)
                .param("to", to)
                .param("date", date.toString()));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("TC-SRC-07: Swapping the stations returns trains only in their scheduled direction")
    void swapReturnsReverseDirectionOnly() throws Exception {
        LocalDate travelDate = MONDAY.plusDays(1);
        JsonNode forward = body(search("AAA", "DDD", travelDate).andExpect(status().isOk()));
        JsonNode reverse = body(search("DDD", "AAA", travelDate).andExpect(status().isOk()));

        assertThat(forward.get("results")).hasSize(2);
        assertThat(reverse.get("results")).isEmpty();
        assertThat(reverse.get("from").get("code").asText()).isEqualTo("DDD");
    }

    @Test
    @DisplayName("TC-SRC-08: Swapping an intermediate journey returns no reverse train")
    void swapMidRouteReturnsNoReverseTrain() throws Exception {
        LocalDate travelDate = MONDAY.plusDays(1);
        JsonNode forward = body(search("BBB", "CCC", travelDate).andExpect(status().isOk()));
        JsonNode reverse = body(search("CCC", "BBB", travelDate).andExpect(status().isOk()));

        assertThat(forward.get("results")).extracting(train -> train.get("number").asText())
                .containsExactly("12901");
        assertThat(reverse.get("results")).isEmpty();
    }

    @Test
    @DisplayName("TC-SRC-09: Station codes and full names resolve before same-station validation")
    void sameStationAliasesAreRejected() throws Exception {
        search("aaa", "AAA", MONDAY.plusDays(1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SAME_STATION"));
        search("Alpha Central", "AAA", MONDAY.plusDays(1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SAME_STATION"));
    }

    @Test
    @DisplayName("TC-SRC-10: Today and day 60 are allowed; yesterday and day 61 are rejected")
    void dateBoundaries() throws Exception {
        search("AAA", "DDD", MONDAY).andExpect(status().isOk());
        search("AAA", "DDD", MONDAY.plusDays(60)).andExpect(status().isOk());
        search("AAA", "DDD", MONDAY.minusDays(1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATE_IN_PAST"));
        search("AAA", "DDD", MONDAY.plusDays(61))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATE_TOO_FAR"));
    }

    @Test
    @DisplayName("TC-SRC-11: Missing and blank station or date inputs return clear errors")
    void missingAndBlankInputs() throws Exception {
        mvc.perform(get("/api/v1/trains/search").param("to", "DDD").param("date", MONDAY.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Missing from."));
        mvc.perform(get("/api/v1/trains/search").param("from", "AAA").param("to", "DDD"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Missing date."));
        mvc.perform(get("/api/v1/trains/search").param("from", "").param("to", "DDD")
                        .param("date", MONDAY.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("STATION_REQUIRED"));
    }
}
