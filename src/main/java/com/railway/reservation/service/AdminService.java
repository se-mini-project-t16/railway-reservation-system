package com.railway.reservation.service;

import com.railway.reservation.dto.AdminDashboardStatsDto;
import com.railway.reservation.dto.BookingReportDto;
import com.railway.reservation.dto.CoachSeatMatrixDto;
import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.ReservationChartDto;
import com.railway.reservation.dto.TrainResponseDto;
import com.railway.reservation.dto.UpdateTrainDto;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {

    List<TrainResponseDto> getAllTrains();

    TrainResponseDto createTrain(CreateTrainDto createTrainDto);

    TrainResponseDto updateTrain(Long id, UpdateTrainDto updateTrainDto);

    void deleteTrain(Long id);

    BookingReportDto getBookingsReport(String trainNo, LocalDate journeyDate);

    AdminDashboardStatsDto getDashboardStats();

    CoachSeatMatrixDto getCoachSeatMatrix(Long trainId, LocalDate journeyDate, String className);

    ReservationChartDto getReservationChart(Long trainId, LocalDate journeyDate);

    String generateReservationChartCsv(Long trainId, LocalDate journeyDate);
}
