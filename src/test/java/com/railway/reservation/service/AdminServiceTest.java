package com.railway.reservation.service;

import com.railway.reservation.dto.AdminDashboardStatsDto;
import com.railway.reservation.dto.BookingReportDto;
import com.railway.reservation.dto.CoachSeatMatrixDto;
import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.ReservationChartDto;
import com.railway.reservation.dto.TrainClassDto;
import com.railway.reservation.dto.TrainResponseDto;
import com.railway.reservation.dto.TrainStopDto;
import com.railway.reservation.dto.UpdateTrainDto;
import com.railway.reservation.exception.DuplicateTrainException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.exception.TrainDeletionException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.Payment;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.service.impl.AdminServiceImpl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private Train existingTrain;

    @BeforeEach
    void setUp() {
        existingTrain = new Train("12675", "Kovai Express", "Chennai", "Bengaluru",
                LocalTime.of(6, 10), LocalTime.of(11, 30));
        existingTrain.setId(1L);

        TrainClass cc = new TrainClass("CC", 100, new BigDecimal("450.00"));
        cc.setId(11L);
        existingTrain.addClass(cc);
    }

    @Test
    @DisplayName("createTrain creates a new train with classes successfully")
    void createTrain_Success() {
        CreateTrainDto createDto = new CreateTrainDto(
                "12001",
                "Shatabdi Express",
                "New Delhi",
                "Bhopal",
                LocalTime.of(6, 0),
                LocalTime.of(14, 0),
                List.of(new TrainClassDto("CC", 120, new BigDecimal("650.00")))
        );

        when(trainRepository.existsByTrainNo("12001")).thenReturn(false);
        when(trainRepository.saveAndFlush(any(Train.class))).thenAnswer(invocation -> {
            Train t = invocation.getArgument(0);
            t.setId(10L);
            return t;
        });

        TrainResponseDto result = adminService.createTrain(createDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTrainNo()).isEqualTo("12001");
        assertThat(result.getName()).isEqualTo("Shatabdi Express");
        assertThat(result.getClasses()).hasSize(1);
        assertThat(result.getClasses().get(0).getClassName()).isEqualTo("CC");
        verify(trainRepository).saveAndFlush(any(Train.class));
    }

    @Test
    @DisplayName("createTrain throws DuplicateTrainException when trainNo already exists")
    void createTrain_DuplicateTrainNo_ThrowsDuplicateTrainException() {
        CreateTrainDto createDto = new CreateTrainDto(
                "12675",
                "Duplicate Train",
                "Chennai",
                "Bengaluru",
                LocalTime.of(6, 0),
                LocalTime.of(11, 0),
                List.of(new TrainClassDto("CC", 50, new BigDecimal("300.00")))
        );

        when(trainRepository.existsByTrainNo("12675")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createTrain(createDto))
                .isInstanceOf(DuplicateTrainException.class)
                .hasMessageContaining("12675");

        verify(trainRepository, never()).save(any(Train.class));
    }

    @Test
    @DisplayName("updateTrain updates existing train details and replaces classes")
    void updateTrain_Success() {
        UpdateTrainDto updateDto = new UpdateTrainDto(
                "12675",
                "Kovai Superfast Express",
                "Chennai Central",
                "Bengaluru City",
                LocalTime.of(6, 15),
                LocalTime.of(11, 45),
                List.of(
                        new TrainClassDto("1A", 24, new BigDecimal("1200.00")),
                        new TrainClassDto("2A", 48, new BigDecimal("800.00"))
                )
        );

        when(trainRepository.findById(1L)).thenReturn(Optional.of(existingTrain));
        when(trainRepository.saveAndFlush(any(Train.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainResponseDto result = adminService.updateTrain(1L, updateDto);

        assertThat(result.getName()).isEqualTo("Kovai Superfast Express");
        assertThat(result.getSourceStation()).isEqualTo("Chennai Central");
        assertThat(result.getDestinationStation()).isEqualTo("Bengaluru City");
        assertThat(result.getClasses()).hasSize(2);
        verify(trainRepository).saveAndFlush(existingTrain);
    }

    @Test
    @DisplayName("updateTrain throws ResourceNotFoundException when train does not exist")
    void updateTrain_NotFound_ThrowsResourceNotFoundException() {
        UpdateTrainDto updateDto = new UpdateTrainDto(
                "12675", "Name", "Src", "Dst",
                LocalTime.of(6, 0), LocalTime.of(11, 0),
                List.of(new TrainClassDto("CC", 50, new BigDecimal("300.00")))
        );

        when(trainRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateTrain(999L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateTrain throws DuplicateTrainException when changing trainNo to an existing trainNo")
    void updateTrain_DuplicateTrainNo_ThrowsDuplicateTrainException() {
        UpdateTrainDto updateDto = new UpdateTrainDto(
                "99999", "New Name", "Src", "Dst",
                LocalTime.of(6, 0), LocalTime.of(11, 0),
                List.of(new TrainClassDto("CC", 50, new BigDecimal("300.00")))
        );

        when(trainRepository.findById(1L)).thenReturn(Optional.of(existingTrain));
        when(trainRepository.existsByTrainNo("99999")).thenReturn(true);

        assertThatThrownBy(() -> adminService.updateTrain(1L, updateDto))
                .isInstanceOf(DuplicateTrainException.class)
                .hasMessageContaining("99999");
    }

    @Test
    @DisplayName("deleteTrain deletes train when no active bookings exist")
    void deleteTrain_Success() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(existingTrain));
        when(bookingRepository.existsByTrainId(1L)).thenReturn(false);

        adminService.deleteTrain(1L);

        verify(trainRepository).delete(existingTrain);
    }

    @Test
    @DisplayName("deleteTrain throws ResourceNotFoundException when train is not found")
    void deleteTrain_NotFound_ThrowsResourceNotFoundException() {
        when(trainRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteTrain(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("deleteTrain throws TrainDeletionException when bookings exist for the train")
    void deleteTrain_BookingsExist_ThrowsTrainDeletionException() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(existingTrain));
        when(bookingRepository.existsByTrainId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminService.deleteTrain(1L))
                .isInstanceOf(TrainDeletionException.class)
                .hasMessageContaining("1");

        verify(trainRepository, never()).delete(any(Train.class));
    }

    @Test
    @DisplayName("TC-ADM-02: getBookingsReport maps rows and computes total, failed, cancelled and refund metrics")
    void getBookingsReport_ComputesMetrics() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        TrainClass cc = existingTrain.getClasses().get(0);

        Booking confirmed = new Booking("PNR1", null, existingTrain, cc, date, new BigDecimal("900.00"));
        confirmed.setStatus(BookingStatus.CONFIRMED);
        confirmed.addPassenger(new Passenger("Alice Smith", 28, "Female", "CC-1"));
        confirmed.setPayment(new Payment(confirmed, new BigDecimal("900.00"), "TXN-1", "SUCCESS"));

        Booking failed = new Booking("PNR2", null, existingTrain, cc, date, new BigDecimal("450.00"));
        failed.setStatus(BookingStatus.FAILED);

        Booking cancelled = new Booking("PNR3", null, existingTrain, cc, date, new BigDecimal("450.00"));
        cancelled.setStatus(BookingStatus.CANCELLED);
        Payment refunded = new Payment(cancelled, new BigDecimal("450.00"), "TXN-3", "REFUND_INITIATED");
        refunded.setRefundAmount(new BigDecimal("337.50"));
        cancelled.setPayment(refunded);

        when(bookingRepository.findForAdminReport("12675", date)).thenReturn(List.of(confirmed, failed, cancelled));

        BookingReportDto report = adminService.getBookingsReport("12675", date);

        assertThat(report.getTotalBookings()).isEqualTo(3);
        assertThat(report.getFailedPayments()).isEqualTo(1);
        assertThat(report.getCancellations()).isEqualTo(1);
        assertThat(report.getTotalRefunds()).isEqualByComparingTo("337.50");
        assertThat(report.getBookings()).hasSize(3);
        assertThat(report.getBookings().get(0).getPnr()).isEqualTo("PNR1");
        assertThat(report.getBookings().get(0).getTrainNo()).isEqualTo("12675");
        assertThat(report.getBookings().get(0).getFare()).isEqualByComparingTo("900.00");
        assertThat(report.getBookings().get(0).getPassengers().get(0).name()).isEqualTo("Alice Smith");
        assertThat(report.getBookings().get(0).getPassengers().get(0).seatNo()).isEqualTo("CC-1");
    }

    @Test
    @DisplayName("getDashboardStats calculates today and all-time revenue and occupancy metrics")
    void getDashboardStats_CalculatesMetrics() {
        when(trainRepository.count()).thenReturn(5L);
        when(bookingRepository.countByStatus(BookingStatus.CONFIRMED)).thenReturn(12L);
        when(trainRepository.findAll()).thenReturn(List.of(existingTrain));

        Booking confirmed = new Booking("PNR_TODAY", null, existingTrain, existingTrain.getClasses().get(0),
                LocalDate.now(), new BigDecimal("900.00"));
        confirmed.setStatus(BookingStatus.CONFIRMED);
        confirmed.addPassenger(new Passenger("Today Pax", 30, "Male", "CC-1"));

        when(bookingRepository.findBookingsCreatedBetween(any(), any())).thenReturn(List.of(confirmed));
        when(bookingRepository.findAll()).thenReturn(List.of(confirmed));

        AdminDashboardStatsDto stats = adminService.getDashboardStats();

        assertThat(stats).isNotNull();
        assertThat(stats.getActiveFleetTrains()).isEqualTo(5L);
        assertThat(stats.getTodayBookings()).isEqualTo(1L);
        assertThat(stats.getTodayRevenue()).isEqualByComparingTo("900.00");
        assertThat(stats.getTotalBookingsAllTime()).isEqualTo(12L);
        assertThat(stats.getTotalRevenueAllTime()).isEqualByComparingTo("900.00");
        assertThat(stats.getAverageOccupancyRate()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("getCoachSeatMatrix maps 2D seat cells and passenger details for confirmed bookings")
    void getCoachSeatMatrix_GeneratesGrid() {
        LocalDate date = LocalDate.of(2026, 11, 10);
        TrainClass cc = existingTrain.getClasses().get(0);

        Booking booking = new Booking("PNR_GRID", null, existingTrain, cc, date, new BigDecimal("450.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.addPassenger(new Passenger("John Doe", 35, "Male", "CC-1"));

        when(trainRepository.findById(1L)).thenReturn(Optional.of(existingTrain));
        when(bookingRepository.findActiveTrainBookingsWithPassengers(1L, date, BookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));

        CoachSeatMatrixDto matrix = adminService.getCoachSeatMatrix(1L, date, "CC");

        assertThat(matrix).isNotNull();
        assertThat(matrix.getTrainNo()).isEqualTo("12675");
        assertThat(matrix.getClassName()).isEqualTo("CC");
        assertThat(matrix.getTotalSeats()).isEqualTo(100);
        assertThat(matrix.getBookedSeatsCount()).isEqualTo(1);
        assertThat(matrix.getAvailableSeatsCount()).isEqualTo(99);
        assertThat(matrix.getSeats()).hasSize(100);

        // First seat is booked by John Doe
        assertThat(matrix.getSeats().get(0).getStatus()).isEqualTo("BOOKED");
        assertThat(matrix.getSeats().get(0).getPassengerName()).isEqualTo("John Doe");
        assertThat(matrix.getSeats().get(0).getPnr()).isEqualTo("PNR_GRID");

        // Second seat is available
        assertThat(matrix.getSeats().get(1).getStatus()).isEqualTo("AVAILABLE");
        assertThat(matrix.getSeats().get(1).getPassengerName()).isNull();
    }

    @Test
    @DisplayName("getReservationChart and generateReservationChartCsv return passenger chart data")
    void getReservationChart_AndCsv_Success() {
        LocalDate date = LocalDate.of(2026, 11, 10);
        TrainClass cc = existingTrain.getClasses().get(0);

        Booking booking = new Booking("PNR_CHART", null, existingTrain, cc, date, new BigDecimal("450.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.addPassenger(new Passenger("Sarah Connor", 29, "Female", "CC-5"));

        when(trainRepository.findById(1L)).thenReturn(Optional.of(existingTrain));
        when(bookingRepository.findActiveTrainBookingsWithPassengers(1L, date, BookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));

        ReservationChartDto chart = adminService.getReservationChart(1L, date);
        assertThat(chart).isNotNull();
        assertThat(chart.getTotalPassengers()).isEqualTo(1);
        assertThat(chart.getEntries().get(0).getPassengerName()).isEqualTo("Sarah Connor");
        assertThat(chart.getEntries().get(0).getSeatNumber()).isEqualTo("CC-5");

        String csv = adminService.generateReservationChartCsv(1L, date);
        assertThat(csv).contains("PNR,Coach/Class,Seat Number,Passenger Name");
        assertThat(csv).contains("PNR_CHART,CC,CC-5,Sarah Connor,29,Female,CONFIRMED");
    }
}
