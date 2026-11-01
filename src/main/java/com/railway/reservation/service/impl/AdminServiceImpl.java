package com.railway.reservation.service.impl;

import com.railway.reservation.dto.BookingReportDto;
import com.railway.reservation.dto.BookingReportRowDto;
import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.TrainClassDto;
import com.railway.reservation.dto.TrainClassResponseDto;
import com.railway.reservation.dto.TrainResponseDto;
import com.railway.reservation.dto.UpdateTrainDto;
import com.railway.reservation.exception.DuplicateTrainException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.exception.TrainDeletionException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final TrainRepository trainRepository;
    private final BookingRepository bookingRepository;

    public AdminServiceImpl(TrainRepository trainRepository, BookingRepository bookingRepository) {
        this.trainRepository = trainRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public TrainResponseDto createTrain(CreateTrainDto createTrainDto) {
        if (trainRepository.existsByTrainNo(createTrainDto.getTrainNo().trim())) {
            throw new DuplicateTrainException("A train with train number " + createTrainDto.getTrainNo().trim() + " already exists.");
        }

        Train train = new Train(
                createTrainDto.getTrainNo().trim(),
                createTrainDto.getName().trim(),
                createTrainDto.getSourceStation().trim(),
                createTrainDto.getDestinationStation().trim(),
                createTrainDto.getDepartureTime(),
                createTrainDto.getArrivalTime()
        );

        if (createTrainDto.getClasses() != null) {
            for (TrainClassDto classDto : createTrainDto.getClasses()) {
                TrainClass trainClass = new TrainClass(
                        classDto.getClassName().trim(),
                        classDto.getTotalSeats(),
                        classDto.getFare()
                );
                train.addClass(trainClass);
            }
        }

        Train savedTrain = trainRepository.saveAndFlush(train);
        return mapToResponseDto(savedTrain);
    }

    @Override
    public TrainResponseDto updateTrain(Long id, UpdateTrainDto updateTrainDto) {
        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + id));

        String newTrainNo = updateTrainDto.getTrainNo().trim();
        if (!train.getTrainNo().equalsIgnoreCase(newTrainNo) && trainRepository.existsByTrainNo(newTrainNo)) {
            throw new DuplicateTrainException("A train with train number " + newTrainNo + " already exists.");
        }

        train.setTrainNo(newTrainNo);
        train.setName(updateTrainDto.getName().trim());
        train.setSourceStation(updateTrainDto.getSourceStation().trim());
        train.setDestinationStation(updateTrainDto.getDestinationStation().trim());
        train.setDepartureTime(updateTrainDto.getDepartureTime());
        train.setArrivalTime(updateTrainDto.getArrivalTime());

        train.getClasses().clear();
        trainRepository.flush();

        if (updateTrainDto.getClasses() != null) {
            for (TrainClassDto classDto : updateTrainDto.getClasses()) {
                TrainClass trainClass = new TrainClass(
                        classDto.getClassName().trim(),
                        classDto.getTotalSeats(),
                        classDto.getFare()
                );
                train.addClass(trainClass);
            }
        }

        Train updatedTrain = trainRepository.saveAndFlush(train);
        return mapToResponseDto(updatedTrain);
    }

    @Override
    public void deleteTrain(Long id) {
        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + id));

        if (bookingRepository.existsByTrainId(id)) {
            throw new TrainDeletionException("Cannot delete train with id " + id + " because active bookings exist.");
        }

        trainRepository.delete(train);
        trainRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingReportDto getBookingsReport(String trainNo, LocalDate journeyDate) {
        List<Booking> bookings = bookingRepository.findForAdminReport(trainNo, journeyDate);

        List<BookingReportRowDto> rows = bookings.stream()
                .map(b -> new BookingReportRowDto(
                        b.getPnr(),
                        b.getTrain().getTrainNo(),
                        b.getJourneyDate(),
                        b.getStatus().name(),
                        b.getTotalFare(),
                        b.getPassengers().stream()
                                .map(p -> new BookingReportRowDto.PassengerSeat(p.getName(), p.getSeatNo()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        long failedPayments = bookings.stream().filter(b -> b.getStatus() == BookingStatus.FAILED).count();
        long cancellations = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        BigDecimal totalRefunds = bookings.stream()
                .map(Booking::getPayment)
                .filter(Objects::nonNull)
                .map(p -> p.getRefundAmount())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BookingReportDto(trainNo, journeyDate, rows.size(), failedPayments, cancellations, totalRefunds, rows);
    }

    private TrainResponseDto mapToResponseDto(Train train) {
        List<TrainClassResponseDto> classDtos = train.getClasses().stream()
                .map(tc -> new TrainClassResponseDto(
                        tc.getId(),
                        tc.getClassName(),
                        tc.getTotalSeats(),
                        tc.getFare()
                ))
                .collect(Collectors.toList());

        return new TrainResponseDto(
                train.getId(),
                train.getTrainNo(),
                train.getName(),
                train.getSourceStation(),
                train.getDestinationStation(),
                train.getDepartureTime(),
                train.getArrivalTime(),
                classDtos
        );
    }
}
