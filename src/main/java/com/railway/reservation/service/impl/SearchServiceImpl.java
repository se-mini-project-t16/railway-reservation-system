package com.railway.reservation.service.impl;

import com.railway.reservation.dto.SeatAvailabilityDto;
import com.railway.reservation.dto.TrainSearchResponseDto;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.service.SearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.CONFIRMED,
            BookingStatus.PENDING
    );

    private final TrainRepository trainRepository;
    private final BookingRepository bookingRepository;

    public SearchServiceImpl(TrainRepository trainRepository, BookingRepository bookingRepository) {
        this.trainRepository = trainRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainSearchResponseDto> searchTrains(String sourceStation, String destinationStation, LocalDate journeyDate) {
        if (sourceStation == null || sourceStation.trim().isEmpty()) {
            throw new IllegalArgumentException("Source station ('src') parameter is required.");
        }
        if (destinationStation == null || destinationStation.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination station ('dst') parameter is required.");
        }
        if (sourceStation.trim().equalsIgnoreCase(destinationStation.trim())) {
            throw new IllegalArgumentException("Source and destination stations cannot be the same.");
        }
        if (journeyDate == null) {
            throw new IllegalArgumentException("Journey date ('date') parameter is required.");
        }
        if (journeyDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Journey date cannot be in the past.");
        }

        List<Train> trains = trainRepository.findBySourceStationIgnoreCaseAndDestinationStationIgnoreCase(
                sourceStation.trim(), destinationStation.trim()
        );

        List<TrainSearchResponseDto> dtos = new ArrayList<>();
        for (Train train : trains) {
            List<SeatAvailabilityDto> classDtos = new ArrayList<>();
            for (TrainClass tc : train.getClasses()) {
                int bookedSeats = bookingRepository.countBookedSeats(
                        train.getId(),
                        tc.getId(),
                        journeyDate,
                        ACTIVE_STATUSES
                );
                int availableSeats = Math.max(0, tc.getTotalSeats() - bookedSeats);
                classDtos.add(new SeatAvailabilityDto(
                        tc.getId(),
                        tc.getClassName(),
                        tc.getTotalSeats(),
                        availableSeats,
                        tc.getFare()
                ));
            }

            dtos.add(new TrainSearchResponseDto(
                    train.getId(),
                    train.getTrainNo(),
                    train.getName(),
                    train.getSourceStation(),
                    train.getDestinationStation(),
                    train.getDepartureTime(),
                    train.getArrivalTime(),
                    journeyDate,
                    classDtos
            ));
        }

        return dtos;
    }
}
