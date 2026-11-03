package com.railway.reservation.service.impl;

import com.railway.reservation.config.StationCatalog;
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
        if (trains.isEmpty()) {
            List<String> srcAliases = StationCatalog.expandAliases(sourceStation);
            List<String> dstAliases = StationCatalog.expandAliases(destinationStation);
            if (srcAliases.size() > 1 || dstAliases.size() > 1) {
                trains = trainRepository.findBySourceAndDestinationAliases(
                        sourceStation.trim(), srcAliases, destinationStation.trim(), dstAliases
                );
            }
        }

        List<TrainSearchResponseDto> dtos = new ArrayList<>();
        for (Train train : trains) {
            List<SeatAvailabilityDto> classDtos = new ArrayList<>();
            for (TrainClass tc : train.getClasses()) {
                classDtos.add(calculateAvailableSeats(train, tc, journeyDate));
            }

            List<TrainStopDto> stopDtos = new ArrayList<>();
            if (train.getStops() != null) {
                for (TrainStop s : train.getStops()) {
                    stopDtos.add(new TrainStopDto(
                            s.getStopOrder(),
                            s.getStationCode(),
                            s.getStationName(),
                            s.getArrivalTime(),
                            s.getDepartureTime(),
                            s.getHaltMinutes(),
                            s.getPlatformNo(),
                            s.getDistanceKm()
                    ));
                }
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
                    classDtos,
                    stopDtos
            ));
        }

        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public SeatAvailabilityDto getSeatAvailability(Long trainId, String className, LocalDate journeyDate) {
        if (trainId == null) {
            throw new IllegalArgumentException("Train ID parameter is required.");
        }
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name parameter is required.");
        }
        if (journeyDate == null) {
            throw new IllegalArgumentException("Journey date parameter is required.");
        }
        if (journeyDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Journey date cannot be in the past.");
        }

        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with ID: " + trainId));

        TrainClass trainClass = train.getClasses().stream()
                .filter(c -> c.getClassName().equalsIgnoreCase(className.trim()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class '" + className + "' not found for train: " + train.getTrainNo()));

        return calculateAvailableSeats(train, trainClass, journeyDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainStopDto> getTrainRoute(Long trainId) {
        if (trainId == null) {
            throw new IllegalArgumentException("Train ID parameter is required.");
        }
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with ID: " + trainId));

        List<TrainStopDto> stopDtos = new ArrayList<>();
        if (train.getStops() != null) {
            for (TrainStop s : train.getStops()) {
                stopDtos.add(new TrainStopDto(
                        s.getStopOrder(),
                        s.getStationCode(),
                        s.getStationName(),
                        s.getArrivalTime(),
                        s.getDepartureTime(),
                        s.getHaltMinutes(),
                        s.getPlatformNo(),
                        s.getDistanceKm()
                ));
            }
        }
        return stopDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public SeatAvailabilityDto calculateAvailableSeats(Train train, TrainClass trainClass, LocalDate journeyDate) {
        int bookedSeats = bookingRepository.countBookedSeats(
                train.getId(),
                trainClass.getId(),
                journeyDate,
                ACTIVE_STATUSES
        );
        int availableSeats = Math.max(0, trainClass.getTotalSeats() - bookedSeats);
        return new SeatAvailabilityDto(
                trainClass.getId(),
                trainClass.getClassName(),
                trainClass.getTotalSeats(),
                availableSeats,
                trainClass.getFare()
        );
    }
}
