package com.railway.reservation.service;

import com.railway.reservation.dto.SeatAvailabilityDto;
import com.railway.reservation.dto.TrainSearchResponseDto;
import com.railway.reservation.dto.TrainStopDto;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;

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

    /**
     * Retrieve intermediate route stops and timetable details for a specific train (RRS-F-004-UI).
     *
     * @param trainId the train ID
     * @return ordered list of timetable stops
     */
    List<TrainStopDto> getTrainRoute(Long trainId);

    /**
     * Calculate dynamic seat availability for a train, class, and journey date.
     *
     * @param trainId     the train ID
     * @param className   the class name (e.g. "CC", "EC")
     * @param journeyDate the journey date
     * @return seat availability and fare details
     */
    SeatAvailabilityDto getSeatAvailability(Long trainId, String className, LocalDate journeyDate);

    /**
     * Core calculation helper: computes availableSeats = max(0, totalSeats - bookedSeats)
     * excluding cancelled and failed bookings.
     *
     * @param train       train entity
     * @param trainClass  train class entity
     * @param journeyDate journey date
     * @return SeatAvailabilityDto
     */
    SeatAvailabilityDto calculateAvailableSeats(Train train, TrainClass trainClass, LocalDate journeyDate);
}
