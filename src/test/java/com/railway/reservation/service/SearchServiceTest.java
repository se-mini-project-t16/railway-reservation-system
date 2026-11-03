package com.railway.reservation.service;

import com.railway.reservation.dto.SeatAvailabilityDto;
import com.railway.reservation.dto.TrainSearchResponseDto;
import com.railway.reservation.dto.TrainStopDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.model.TrainStop;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    // ==========================================
    // RRS-F-005 Seat Availability Calculation Tests
    // ==========================================

    @Test
    @DisplayName("RRS-F-005: Calculate available seats for specific train and class (partial booking)")
    void getSeatAvailability_partialBooking() {
        LocalDate futureDate = LocalDate.now().plusDays(5);
        when(trainRepository.findById(10L)).thenReturn(Optional.of(testTrain));
        when(bookingRepository.countBookedSeats(eq(10L), eq(1L), eq(futureDate), any()))
                .thenReturn(42);

        SeatAvailabilityDto availability = searchService.getSeatAvailability(10L, "CC", futureDate);

        assertThat(availability.getClassId()).isEqualTo(1L);
        assertThat(availability.getClassName()).isEqualTo("CC");
        assertThat(availability.getTotalSeats()).isEqualTo(100);
        assertThat(availability.getAvailableSeats()).isEqualTo(58);
        assertThat(availability.getFare()).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("RRS-F-005: Fully booked train calculates 0 available seats")
    void getSeatAvailability_fullyBooked() {
        LocalDate futureDate = LocalDate.now().plusDays(3);
        when(trainRepository.findById(10L)).thenReturn(Optional.of(testTrain));
        when(bookingRepository.countBookedSeats(eq(10L), eq(1L), eq(futureDate), any()))
                .thenReturn(100);

        SeatAvailabilityDto availability = searchService.getSeatAvailability(10L, "CC", futureDate);

        assertThat(availability.getAvailableSeats()).isZero();
    }

    @Test
    @DisplayName("RRS-F-005: Zero bookings returns total seats as available")
    void getSeatAvailability_zeroBookings() {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        when(trainRepository.findById(10L)).thenReturn(Optional.of(testTrain));
        when(bookingRepository.countBookedSeats(eq(10L), eq(1L), eq(futureDate), any()))
                .thenReturn(0);

        SeatAvailabilityDto availability = searchService.getSeatAvailability(10L, "cc", futureDate);

        assertThat(availability.getAvailableSeats()).isEqualTo(100);
    }

    @Test
    @DisplayName("RRS-F-005: Excludes CANCELLED and FAILED bookings from active seat count")
    @SuppressWarnings("unchecked")
    void calculateAvailableSeats_activeStatusesExcludeCancelledAndFailed() {
        LocalDate futureDate = LocalDate.now().plusDays(4);
        ArgumentCaptor<List<BookingStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);

        searchService.calculateAvailableSeats(testTrain, testClass, futureDate);

        verify(bookingRepository).countBookedSeats(eq(10L), eq(1L), eq(futureDate), statusCaptor.capture());
        List<BookingStatus> capturedStatuses = statusCaptor.getValue();
        assertThat(capturedStatuses)
                .containsExactlyInAnyOrder(BookingStatus.CONFIRMED, BookingStatus.PENDING)
                .doesNotContain(BookingStatus.CANCELLED, BookingStatus.FAILED);
    }

    @Test
    @DisplayName("RRS-F-005: Throws ResourceNotFoundException for unknown train ID")
    void getSeatAvailability_trainNotFound() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        when(trainRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchService.getSeatAvailability(999L, "CC", futureDate))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Train not found with ID: 999");
    }

    @Test
    @DisplayName("RRS-F-005: Throws ResourceNotFoundException for unknown class on train")
    void getSeatAvailability_classNotFound() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        when(trainRepository.findById(10L)).thenReturn(Optional.of(testTrain));

        assertThatThrownBy(() -> searchService.getSeatAvailability(10L, "FIRST_AC", futureDate))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Class 'FIRST_AC' not found for train: 12675");
    }

    @Test
    @DisplayName("RRS-F-005: Validates required parameters for getSeatAvailability")
    void getSeatAvailability_parameterValidations() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        LocalDate pastDate = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> searchService.getSeatAvailability(null, "CC", futureDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Train ID parameter is required");

        assertThatThrownBy(() -> searchService.getSeatAvailability(10L, "   ", futureDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Class name parameter is required");

        assertThatThrownBy(() -> searchService.getSeatAvailability(10L, "CC", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Journey date parameter is required");

        assertThatThrownBy(() -> searchService.getSeatAvailability(10L, "CC", pastDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Journey date cannot be in the past");
    }

    @Test
    @DisplayName("TC-SCH-01: Search response maps intermediate timetable halt stops with platform and halt duration")
    void searchTrains_mapsIntermediateStops() {
        testTrain.addStop(new TrainStop(1, "MAS", "Chennai Central", null, LocalTime.of(6, 10), 0, 4, 0));
        testTrain.addStop(new TrainStop(2, "KPD", "Katpadi Junction", LocalTime.of(7, 58), LocalTime.of(8, 0), 2, 1, 130));
        testTrain.addStop(new TrainStop(3, "SBC", "KSR Bengaluru", LocalTime.of(11, 30), null, 0, 3, 359));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(trainRepository.findBySourceStationIgnoreCaseAndDestinationStationIgnoreCase("Chennai", "Bengaluru"))
                .thenReturn(List.of(testTrain));
        when(bookingRepository.countBookedSeats(eq(10L), eq(1L), eq(tomorrow), any()))
                .thenReturn(0);

        List<TrainSearchResponseDto> results = searchService.searchTrains("Chennai", "Bengaluru", tomorrow);

        assertThat(results).hasSize(1);
        TrainSearchResponseDto res = results.get(0);
        assertThat(res.getStops()).hasSize(3);
        assertThat(res.getStops().get(0).getStationCode()).isEqualTo("MAS");
        assertThat(res.getStops().get(1).getStationCode()).isEqualTo("KPD");
        assertThat(res.getStops().get(1).getHaltMinutes()).isEqualTo(2);
        assertThat(res.getStops().get(1).getPlatformNo()).isEqualTo(1);
        assertThat(res.getStops().get(1).getDistanceKm()).isEqualTo(130);
        assertThat(res.getStops().get(2).getStationCode()).isEqualTo("SBC");
    }

    @Test
    @DisplayName("RRS-F-004-UI: getTrainRoute returns ordered timetable stops for train")
    void getTrainRoute_successful() {
        testTrain.addStop(new TrainStop(1, "MAS", "Chennai Central", null, LocalTime.of(6, 10), 0, 4, 0));
        testTrain.addStop(new TrainStop(2, "KPD", "Katpadi Junction", LocalTime.of(7, 58), LocalTime.of(8, 0), 2, 1, 130));
        testTrain.addStop(new TrainStop(3, "SBC", "KSR Bengaluru", LocalTime.of(11, 30), null, 0, 3, 359));

        when(trainRepository.findById(10L)).thenReturn(Optional.of(testTrain));

        List<TrainStopDto> route = searchService.getTrainRoute(10L);

        assertThat(route).hasSize(3);
        assertThat(route.get(0).getStationCode()).isEqualTo("MAS");
        assertThat(route.get(0).getStopOrder()).isEqualTo(1);
        assertThat(route.get(1).getStationCode()).isEqualTo("KPD");
        assertThat(route.get(1).getHaltMinutes()).isEqualTo(2);
        assertThat(route.get(1).getPlatformNo()).isEqualTo(1);
        assertThat(route.get(2).getStationCode()).isEqualTo("SBC");
        assertThat(route.get(2).getDistanceKm()).isEqualTo(359);
    }

    @Test
    @DisplayName("RRS-F-004-UI: getTrainRoute throws ResourceNotFoundException for unknown train ID")
    void getTrainRoute_trainNotFound() {
        when(trainRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchService.getTrainRoute(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Train not found with ID: 999");
    }

    @Test
    @DisplayName("RRS-F-004-UI: getTrainRoute validates non-null train ID")
    void getTrainRoute_nullTrainId() {
        assertThatThrownBy(() -> searchService.getTrainRoute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Train ID parameter is required");
    }
}
