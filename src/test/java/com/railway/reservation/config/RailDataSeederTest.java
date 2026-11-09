package com.railway.reservation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainStop;
import com.railway.reservation.repository.StationRepository;
import com.railway.reservation.repository.TrainRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The seeded timetable, loaded into its own in-memory database as the real application loads it. */
@SpringBootTest(properties = {
        "app.seed-demo-data=true",
        "spring.datasource.url=jdbc:h2:mem:seedtest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class RailDataSeederTest {

    @Autowired RailDataSeeder seeder;
    @Autowired StationRepository stations;
    @Autowired TrainRepository trains;
    @Autowired MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @Transactional
    @DisplayName("TC-SEED-01: Every seeded route runs forward in distance and time, and every train has a return working")
    void seededRoutesAreConsistent() {
        List<Train> all = trains.findAllByOrderByNumberAsc();
        assertThat(all).hasSizeGreaterThanOrEqualTo(88);
        assertThat(stations.count()).isGreaterThanOrEqualTo(133);

        Set<String> routes = new HashSet<>();
        for (Train t : all) {
            List<TrainStop> stops = t.getStops();
            assertThat(stops).as(t.getNumber() + " stops").hasSizeGreaterThanOrEqualTo(2);
            assertThat(t.getClasses()).as(t.getNumber() + " classes").isNotEmpty();
            assertThat(stops.getFirst().getDistanceKm()).isZero();
            assertThat(stops.getFirst().getArrivalMinute()).isNull();
            assertThat(stops.getLast().getDepartureMinute()).isNull();
            for (int i = 1; i < stops.size(); i++) {
                TrainStop prev = stops.get(i - 1), cur = stops.get(i);
                assertThat(cur.getDistanceKm()).as(t.getNumber() + " km at " + cur.getStation().getCode())
                        .isGreaterThan(prev.getDistanceKm());
                assertThat(cur.getArrivalMinute()).as(t.getNumber() + " arrival at " + cur.getStation().getCode())
                        .isGreaterThan(prev.getDepartureMinute());
                if (cur.getDepartureMinute() != null) {
                    assertThat(cur.getDepartureMinute()).isGreaterThanOrEqualTo(cur.getArrivalMinute());
                }
            }
            routes.add(stops.getFirst().getStation().getCode() + ">" + stops.getLast().getStation().getCode());
        }
        for (String r : routes) {
            String[] ends = r.split(">");
            assertThat(routes).as("return working for " + r).contains(ends[1] + ">" + ends[0]);
        }
    }

    @Test
    @DisplayName("TC-SEED-02: Seeding again adds nothing, so an existing database keeps its data")
    void seedingTwiceAddsNothing() {
        long s = stations.count(), t = trains.count();
        seeder.run();
        assertThat(stations.count()).isEqualTo(s);
        assertThat(trains.count()).isEqualTo(t);
    }

    @Test
    @DisplayName("TC-SEED-03: The wider network is searchable, for example Mumbai to Chennai, Bengaluru, Kolkata and Kochi")
    void widerNetworkIsSearchable() throws Exception {
        String date = LocalDate.now().plusDays(10).toString();
        assertThat(numbers("CSMT", "MAS", date)).contains("22159");
        assertThat(numbers("CSMT", "SBC", date)).contains("11301");
        assertThat(numbers("CSMT", "HWH", date)).contains("12809");
        assertThat(numbers("LTT", "ERS", date)).contains("16345");
        assertThat(numbers("NDLS", "JP", date)).contains("12958");
    }

    private List<String> numbers(String from, String to, String date) throws Exception {
        JsonNode r = json.readTree(mvc.perform(get("/api/v1/trains/search").param("from", from).param("to", to)
                .param("date", date)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return r.get("results").findValuesAsText("number");
    }
}
