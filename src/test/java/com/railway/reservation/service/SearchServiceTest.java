package com.railway.reservation.service;

import com.railway.reservation.dto.TrainSearchResponseDto;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private SearchServiceImpl searchService;

    private Train testTrain;
    private TrainClass testClass;

    @BeforeEach
    void setUp() {
        testTrain = new Train("12675", "Kovai Express", "Chennai", "Bengaluru",
                LocalTime.of(6, 10), LocalTime.of(11, 30));
        testTrain.setId(10L);

        testClass = new TrainClass("CC", 100, new BigDecimal("450.00"));
        testClass.setId(1L);
        testTrain.addClass(testClass);
    }

    @Test
    @DisplayName("TC-SRC-01: Successfully search trains and calculate available seats")
    void searchTrains_successfulMatchingTrain() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(trainRepository.findBySourceStationIgnoreCaseAndDestinationStationIgnoreCase("Chennai", "Bengaluru"))
                .thenReturn(List.of(testTrain));
        when(bookingRepository.countBookedSeats(eq(10L), eq(1L), eq(tomorrow), any()))
                .thenReturn(15);

        List<TrainSearchResponseDto> results = searchService.searchTrains("Chennai", "Bengaluru", tomorrow);

        assertThat(results).hasSize(1);
        TrainSearchResponseDto result = results.get(0);
        assertThat(result.getTrainNo()).isEqualTo("12675");
        assertThat(result.getName()).isEqualTo("Kovai Express");
        assertThat(result.getSourceStation()).isEqualTo("Chennai");
        assertThat(result.getDestinationStation()).isEqualTo("Bengaluru");
        assertThat(result.getDepartureTime()).isEqualTo(LocalTime.of(6, 10));
        assertThat(result.getArrivalTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(result.getClasses()).hasSize(1);
        assertThat(result.getClasses().get(0).getClassName()).isEqualTo("CC");
        assertThat(result.getClasses().get(0).getTotalSeats()).isEqualTo(100);
        assertThat(result.getClasses().get(0).getAvailableSeats()).isEqualTo(85);
        assertThat(result.getClasses().get(0).getFare()).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("TC-SRC-02: Returns empty list when no trains match the route")
    void searchTrains_noMatchingTrains() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(trainRepository.findBySourceStationIgnoreCaseAndDestinationStationIgnoreCase("Delhi", "Goa"))
                .thenReturn(List.of());

        List<TrainSearchResponseDto> results = searchService.searchTrains("Delhi", "Goa", tomorrow);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Rejects null or blank source station with IllegalArgumentException")
    void searchTrains_invalidSource() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> searchService.searchTrains(null, "Bengaluru", tomorrow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source station");

        assertThatThrownBy(() -> searchService.searchTrains("   ", "Bengaluru", tomorrow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source station");
    }

    @Test
    @DisplayName("Rejects null or blank destination station with IllegalArgumentException")
    void searchTrains_invalidDestination() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> searchService.searchTrains("Chennai", null, tomorrow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Destination station");

        assertThatThrownBy(() -> searchService.searchTrains("Chennai", "   ", tomorrow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Destination station");
    }

    @Test
    @DisplayName("Rejects identical source and destination stations")
    void searchTrains_identicalStations() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> searchService.searchTrains("Chennai", "chennai", tomorrow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source and destination stations cannot be the same");
    }

    @Test
    @DisplayName("Rejects past journey dates with IllegalArgumentException")
    void searchTrains_pastDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThatThrownBy(() -> searchService.searchTrains("Chennai", "Bengaluru", yesterday))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Journey date cannot be in the past");
    }

    @Test
    @DisplayName("Available seats does not go below zero when booked exceeds total")
    void searchTrains_noNegativeAvailableSeats() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(trainRepository.findBySourceStationIgnoreCaseAndDestinationStationIgnoreCase("Chennai", "Bengaluru"))
                .thenReturn(List.of(testTrain));
        when(bookingRepository.countBookedSeats(eq(10L), eq(1L), eq(tomorrow), any()))
                .thenReturn(150); // overbooked

        List<TrainSearchResponseDto> results = searchService.searchTrains("Chennai", "Bengaluru", tomorrow);

        assertThat(results.get(0).getClasses().get(0).getAvailableSeats()).isZero();
    }
}
