package com.railway.reservation.service;

import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.TrainResponseDto;
import com.railway.reservation.dto.UpdateTrainDto;

public interface AdminService {

    TrainResponseDto createTrain(CreateTrainDto createTrainDto);

    TrainResponseDto updateTrain(Long id, UpdateTrainDto updateTrainDto);

    void deleteTrain(Long id);
}
