package com.railway.reservation.service;

import com.railway.reservation.dto.BookingReportDto;
import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.TrainResponseDto;
import com.railway.reservation.dto.UpdateTrainDto;

import java.time.LocalDate;

public interface AdminService {

    TrainResponseDto createTrain(CreateTrainDto createTrainDto);

    TrainResponseDto updateTrain(Long id, UpdateTrainDto updateTrainDto);

    void deleteTrain(Long id);

    BookingReportDto getBookingsReport(String trainNo, LocalDate journeyDate);
}
