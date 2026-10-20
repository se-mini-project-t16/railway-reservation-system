package com.railway.reservation.controller;

import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.repository.TrainClassRepository;
import com.railway.reservation.repository.TrainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchControllerTest {

    private static final String SEARCH_URL = "/api/v1/trains/search";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainClassRepository trainClassRepository;

    private Train savedTrain;

    @BeforeEach
    void setUp() {
        trainRepository.deleteAll();

        Train train = new Train("12675", "Kovai Express", "Chennai", "Bengaluru",
                LocalTime.of(6, 10), LocalTime.of(11, 30));
        TrainClass cc = new TrainClass("CC", 100, new BigDecimal("450.00"));
        TrainClass ec = new TrainClass("EC", 40, new BigDecimal("950.00"));
        train.addClass(cc);
        train.addClass(ec);

        savedTrain = trainRepository.save(train);
    }

    @Test
    @DisplayName("TC-SRC-01: Guest/user searches trains by route and date, receives 200 OK with timings and classes")
    void searchTrains_successful() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", tomorrow)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trainNo").value("12675"))
                .andExpect(jsonPath("$[0].name").value("Kovai Express"))
                .andExpect(jsonPath("$[0].sourceStation").value("Chennai"))
                .andExpect(jsonPath("$[0].destinationStation").value("Bengaluru"))
                .andExpect(jsonPath("$[0].departureTime").value("06:10:00"))
                .andExpect(jsonPath("$[0].arrivalTime").value("11:30:00"))
                .andExpect(jsonPath("$[0].classes", hasSize(2)))
                .andExpect(jsonPath("$[0].classes[0].className").value("CC"))
                .andExpect(jsonPath("$[0].classes[0].availableSeats").value(100))
                .andExpect(jsonPath("$[0].classes[0].fare").value(450.00));
    }

    @Test
    @DisplayName("TC-SRC-02: Search for non-existent route returns 200 OK with empty list and notice header")
    void searchTrains_noTrainsFound() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Delhi")
                        .param("dst", "Goa")
                        .param("date", tomorrow)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Search-Message", "No trains found for selected route and date"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("TC-PERF-01: Train search response completes well within 3 seconds")
    void searchTrains_performanceCheck() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", tomorrow))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;
        org.assertj.core.api.Assertions.assertThat(duration).isLessThan(3000);
    }

    @Test
    @DisplayName("Rejects missing src parameter with 400 Bad Request")
    void searchTrains_missingSrc() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("dst", "Bengaluru")
                        .param("date", tomorrow))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source station")));
    }

    @Test
    @DisplayName("Rejects missing dst parameter with 400 Bad Request")
    void searchTrains_missingDst() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("date", tomorrow))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Destination station")));
    }

    @Test
    @DisplayName("Rejects missing date parameter with 400 Bad Request")
    void searchTrains_missingDate() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Journey date")));
    }

    @Test
    @DisplayName("Rejects invalid date format with 400 Bad Request")
    void searchTrains_invalidDateFormat() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", "21-10-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid journey date format")));
    }

    @Test
    @DisplayName("Rejects identical source and destination with 400 Bad Request")
    void searchTrains_identicalSourceAndDestination() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "chennai")
                        .param("date", tomorrow))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source and destination stations cannot be the same")));
    }

    @Test
    @DisplayName("Rejects past journey date with 400 Bad Request")
    void searchTrains_pastDate() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "Chennai")
                        .param("dst", "Bengaluru")
                        .param("date", yesterday))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Journey date cannot be in the past")));
    }

    @Test
    @DisplayName("Case-insensitive search returns matching trains")
    void searchTrains_caseInsensitive() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get(SEARCH_URL)
                        .param("src", "chEnNaI")
                        .param("dst", "BeNgaLuRu")
                        .param("date", tomorrow))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trainNo").value("12675"));
    }
}
