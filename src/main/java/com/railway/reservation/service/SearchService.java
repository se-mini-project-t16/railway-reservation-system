package com.railway.reservation.service;

import com.railway.reservation.dto.TrainSearchResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface SearchService {

    /**
     * Search trains matching the specified route and travel date.
     *
     * @param sourceStation      source station name
     * @param destinationStation destination station name
     * @param journeyDate        travel journey date (must not be past)
     * @return list of matching trains with timings, classes, and available seats
     */
    List<TrainSearchResponseDto> searchTrains(String sourceStation, String destinationStation, LocalDate journeyDate);
}
