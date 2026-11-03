package com.railway.reservation.service.impl;

import com.railway.reservation.dto.AdminDashboardStatsDto;
import com.railway.reservation.dto.BookingReportDto;
import com.railway.reservation.dto.BookingReportRowDto;
import com.railway.reservation.dto.CoachSeatMatrixDto;
import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.ReservationChartDto;
import com.railway.reservation.dto.ReservationChartEntryDto;
import com.railway.reservation.dto.SeatCellDto;
import com.railway.reservation.dto.TrainClassDto;
import com.railway.reservation.dto.TrainClassResponseDto;
import com.railway.reservation.dto.TrainResponseDto;
import com.railway.reservation.dto.TrainStopDto;
import com.railway.reservation.dto.UpdateTrainDto;
import com.railway.reservation.exception.DuplicateTrainException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.exception.TrainDeletionException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.model.TrainStop;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.service.AdminService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @Transactional(readOnly = true)
    public List<TrainResponseDto> getAllTrains() {
        return trainRepository.findAll(Sort.by("trainNo")).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
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

        if (createTrainDto.getStops() != null) {
            for (TrainStopDto stopDto : createTrainDto.getStops()) {
                TrainStop stop = new TrainStop(
                        stopDto.getStopOrder(),
                        stopDto.getStationCode().trim().toUpperCase(),
                        stopDto.getStationName().trim(),
                        stopDto.getArrivalTime(),
                        stopDto.getDepartureTime(),
                        stopDto.getHaltMinutes(),
                        stopDto.getPlatformNo(),
                        stopDto.getDistanceKm()
                );
                train.addStop(stop);
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
        train.getStops().clear();
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

        if (updateTrainDto.getStops() != null) {
            for (TrainStopDto stopDto : updateTrainDto.getStops()) {
                TrainStop stop = new TrainStop(
                        stopDto.getStopOrder(),
                        stopDto.getStationCode().trim().toUpperCase(),
                        stopDto.getStationName().trim(),
                        stopDto.getArrivalTime(),
                        stopDto.getDepartureTime(),
                        stopDto.getHaltMinutes(),
                        stopDto.getPlatformNo(),
                        stopDto.getDistanceKm()
                );
                train.addStop(stop);
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

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Booking> todayBookingsList = bookingRepository.findBookingsCreatedBetween(startOfDay, endOfDay);

        long todayConfirmed = todayBookingsList.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal todayRevenue = todayBookingsList.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalFare)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long todayCancellations = todayBookingsList.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .count();

        BigDecimal todayRefunds = todayBookingsList.stream()
                .map(Booking::getPayment)
                .filter(Objects::nonNull)
                .map(p -> p.getRefundAmount())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeFleet = trainRepository.count();
        long totalConfirmedAllTime = bookingRepository.countByStatus(BookingStatus.CONFIRMED);

        List<Booking> allConfirmed = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.toList());

        BigDecimal totalRevenueAllTime = allConfirmed.stream()
                .map(Booking::getTotalFare)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fleet average occupancy across scheduled trains
        List<Train> trains = trainRepository.findAll();
        int totalCapacity = trains.stream()
                .flatMap(t -> t.getClasses().stream())
                .mapToInt(TrainClass::getTotalSeats)
                .sum();

        int totalBookedActive = allConfirmed.stream()
                .mapToInt(b -> (int) b.getPassengers().stream()
                        .filter(p -> p.getStatus() == null || !"CANCELLED".equalsIgnoreCase(p.getStatus()))
                        .count())
                .sum();

        double avgOccupancy = totalCapacity > 0 ? (totalBookedActive * 100.0 / totalCapacity) : 0.0;
        double roundedOccupancy = Math.round(avgOccupancy * 10.0) / 10.0;

        return new AdminDashboardStatsDto(
                todayConfirmed,
                todayRevenue,
                activeFleet,
                roundedOccupancy,
                totalConfirmedAllTime,
                totalRevenueAllTime,
                todayCancellations,
                todayRefunds
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CoachSeatMatrixDto getCoachSeatMatrix(Long trainId, LocalDate journeyDate, String className) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + trainId));

        if (journeyDate == null) {
            journeyDate = LocalDate.now();
        }

        TrainClass targetClass = null;
        if (className != null && !className.isBlank()) {
            String trimmed = className.trim();
            targetClass = train.getClasses().stream()
                    .filter(tc -> tc.getClassName().equalsIgnoreCase(trimmed))
                    .findFirst()
                    .orElse(null);
        }
        if (targetClass == null && !train.getClasses().isEmpty()) {
            targetClass = train.getClasses().get(0);
        }
        if (targetClass == null) {
            throw new IllegalArgumentException("Train " + train.getTrainNo() + " has no configured classes.");
        }

        int totalSeats = targetClass.getTotalSeats();
        String classCode = targetClass.getClassName();

        // 1. Fetch active bookings for this train, class, and journey date
        List<Booking> bookings = bookingRepository.findActiveTrainBookingsWithPassengers(trainId, journeyDate, BookingStatus.CONFIRMED);

        // Map assigned seat number -> passenger & booking
        Map<String, Map.Entry<Passenger, Booking>> bookedSeatMap = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getTrainClass() != null && b.getTrainClass().getId().equals(targetClass.getId())) {
                for (Passenger p : b.getPassengers()) {
                    if (p.getStatus() == null || !"CANCELLED".equalsIgnoreCase(p.getStatus())) {
                        String seatNo = p.getSeatNo();
                        if (seatNo != null && !seatNo.isBlank()) {
                            bookedSeatMap.put(seatNo.trim(), Map.entry(p, b));
                            // Also map clean numeric index if prefixed (e.g. "CC-12" -> "12")
                            if (seatNo.contains("-")) {
                                String[] parts = seatNo.split("-");
                                bookedSeatMap.put(parts[parts.length - 1].trim(), Map.entry(p, b));
                            }
                        }
                    }
                }
            }
        }

        // 2. Determine layout dimensions
        int columns;
        if ("CC".equalsIgnoreCase(classCode)) {
            columns = 5; // 3 + 2 layout
        } else if ("1A".equalsIgnoreCase(classCode)) {
            columns = 4; // 2 + 2 layout
        } else if ("2A".equalsIgnoreCase(classCode)) {
            columns = 4; // 2 + 2 layout
        } else {
            columns = 4; // 3A, SL, 2S layout
        }
        int rows = (totalSeats + columns - 1) / columns;

        List<SeatCellDto> cells = new ArrayList<>(totalSeats);
        int bookedCount = 0;

        for (int i = 1; i <= totalSeats; i++) {
            int rowIdx = (i - 1) / columns + 1;
            int colIdx = (i - 1) % columns + 1;

            String berthType = resolveBerthType(classCode, colIdx, rowIdx);
            String seatKeyWithPrefix = classCode + "-" + i;
            String seatKeyNumeric = String.valueOf(i);

            Map.Entry<Passenger, Booking> bookedEntry = bookedSeatMap.get(seatKeyWithPrefix);
            if (bookedEntry == null) {
                bookedEntry = bookedSeatMap.get(seatKeyNumeric);
            }

            SeatCellDto cell = new SeatCellDto();
            cell.setSeatNumber(seatKeyWithPrefix);
            cell.setSeatIndex(i);
            cell.setRow(rowIdx);
            cell.setCol(colIdx);
            cell.setBerthType(berthType);

            if (bookedEntry != null) {
                cell.setStatus("BOOKED");
                Passenger p = bookedEntry.getKey();
                Booking b = bookedEntry.getValue();
                cell.setPassengerName(p.getName());
                cell.setPassengerAge(p.getAge());
                cell.setPassengerGender(p.getGender());
                cell.setPnr(b.getPnr());
                bookedCount++;
            } else {
                cell.setStatus("AVAILABLE");
            }
            cells.add(cell);
        }

        int availableCount = totalSeats - bookedCount;
        double occupancyRate = totalSeats > 0 ? (bookedCount * 100.0 / totalSeats) : 0.0;

        CoachSeatMatrixDto dto = new CoachSeatMatrixDto();
        dto.setTrainId(train.getId());
        dto.setTrainNo(train.getTrainNo());
        dto.setTrainName(train.getName());
        dto.setClassName(classCode);
        dto.setCoachCode(classCode + "-1");
        dto.setJourneyDate(journeyDate);
        dto.setTotalSeats(totalSeats);
        dto.setBookedSeatsCount(bookedCount);
        dto.setAvailableSeatsCount(availableCount);
        dto.setOccupancyRate(Math.round(occupancyRate * 10.0) / 10.0);
        dto.setRows(rows);
        dto.setColumns(columns);
        dto.setSeats(cells);

        return dto;
    }

    private String resolveBerthType(String classCode, int col, int row) {
        if ("CC".equalsIgnoreCase(classCode)) {
            if (col == 1 || col == 5) return "Window";
            if (col == 2) return "Middle";
            return "Aisle";
        } else if ("2A".equalsIgnoreCase(classCode)) {
            if (col == 1 || col == 3) return "Lower";
            if (col == 2) return "Upper";
            return "Side Lower";
        } else if ("3A".equalsIgnoreCase(classCode) || "SL".equalsIgnoreCase(classCode)) {
            if (col == 1) return "Lower";
            if (col == 2) return "Middle";
            if (col == 3) return "Upper";
            return "Side Lower";
        } else if ("1A".equalsIgnoreCase(classCode)) {
            return (col % 2 == 1) ? "Lower" : "Upper";
        }
        return "Standard";
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationChartDto getReservationChart(Long trainId, LocalDate journeyDate) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + trainId));

        if (journeyDate == null) {
            journeyDate = LocalDate.now();
        }

        List<Booking> bookings = bookingRepository.findActiveTrainBookingsWithPassengers(trainId, journeyDate, BookingStatus.CONFIRMED);

        List<ReservationChartEntryDto> entries = new ArrayList<>();
        for (Booking b : bookings) {
            String className = (b.getTrainClass() != null) ? b.getTrainClass().getClassName() : "2S";
            for (Passenger p : b.getPassengers()) {
                if (p.getStatus() == null || !"CANCELLED".equalsIgnoreCase(p.getStatus())) {
                    entries.add(new ReservationChartEntryDto(
                            b.getPnr(),
                            p.getSeatNo() != null ? p.getSeatNo() : "Confirmed",
                            className,
                            p.getName(),
                            p.getAge(),
                            p.getGender(),
                            "CONFIRMED",
                            train.getSourceStation(),
                            train.getDestinationStation()
                    ));
                }
            }
        }

        entries.sort(Comparator.comparing(ReservationChartEntryDto::getClassName)
                .thenComparing(ReservationChartEntryDto::getSeatNumber, Comparator.nullsLast(Comparator.naturalOrder())));

        ReservationChartDto chart = new ReservationChartDto();
        chart.setTrainId(train.getId());
        chart.setTrainNo(train.getTrainNo());
        chart.setTrainName(train.getName());
        chart.setSourceStation(train.getSourceStation());
        chart.setDestinationStation(train.getDestinationStation());
        chart.setDepartureTime(train.getDepartureTime());
        chart.setJourneyDate(journeyDate);
        chart.setTotalPassengers(entries.size());
        chart.setEntries(entries);

        return chart;
    }

    @Override
    @Transactional(readOnly = true)
    public String generateReservationChartCsv(Long trainId, LocalDate journeyDate) {
        ReservationChartDto chart = getReservationChart(trainId, journeyDate);

        StringBuilder sb = new StringBuilder();
        sb.append("PNR,Coach/Class,Seat Number,Passenger Name,Age,Gender,Booking Status,Boarding,Destination\n");

        for (ReservationChartEntryDto e : chart.getEntries()) {
            sb.append(escapeCsv(e.getPnr())).append(",")
              .append(escapeCsv(e.getClassName())).append(",")
              .append(escapeCsv(e.getSeatNumber())).append(",")
              .append(escapeCsv(e.getPassengerName())).append(",")
              .append(e.getAge() != null ? e.getAge() : "").append(",")
              .append(escapeCsv(e.getGender())).append(",")
              .append(escapeCsv(e.getStatus())).append(",")
              .append(escapeCsv(e.getBoardingStation())).append(",")
              .append(escapeCsv(e.getDestinationStation())).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
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

        TrainResponseDto dto = new TrainResponseDto(
                train.getId(),
                train.getTrainNo(),
                train.getName(),
                train.getSourceStation(),
                train.getDestinationStation(),
                train.getDepartureTime(),
                train.getArrivalTime(),
                classDtos
        );

        if (train.getStops() != null && !train.getStops().isEmpty()) {
            List<TrainStopDto> stopDtos = train.getStops().stream()
                    .map(s -> new TrainStopDto(
                            s.getStopOrder(),
                            s.getStationCode(),
                            s.getStationName(),
                            s.getArrivalTime(),
                            s.getDepartureTime(),
                            s.getHaltMinutes(),
                            s.getPlatformNo(),
                            s.getDistanceKm()
                    ))
                    .collect(Collectors.toList());
            dto.setStops(stopDtos);
        }

        return dto;
    }
}
