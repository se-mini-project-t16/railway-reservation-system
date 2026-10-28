package com.railway.reservation.service.impl;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.dto.PnrStatusResponseDto;
import com.railway.reservation.exception.PaymentNotVerifiedException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.exception.SeatUnavailableException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.*;
import com.railway.reservation.service.BookingService;
import com.railway.reservation.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BookingServiceImpl implements BookingService {

    private static final String PNR_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PNR_LENGTH = 10;
    private final SecureRandom random = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TrainRepository trainRepository;
    private final TrainClassRepository trainClassRepository;
    private final PassengerRepository passengerRepository;
    private final PaymentService paymentService;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            TrainRepository trainRepository,
            TrainClassRepository trainClassRepository,
            PassengerRepository passengerRepository,
            PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.trainRepository = trainRepository;
        this.trainClassRepository = trainClassRepository;
        this.passengerRepository = passengerRepository;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request) {
        // 1. Verify User exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // 2. Verify Train exists
        Train train = trainRepository.findById(request.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + request.getTrainId()));

        // 3. Verify TrainClass exists and belongs to the Train
        TrainClass trainClass = trainClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + request.getClassId()));

        if (!trainClass.getTrain().getId().equals(train.getId())) {
            throw new IllegalArgumentException("Selected class does not belong to train " + train.getTrainNo());
        }

        // 4. Check real-time seat availability (RRS-F-006, SAD Section 3.7)
        List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);
        int alreadyBooked = bookingRepository.countBookedSeats(
                train.getId(), trainClass.getId(), request.getJourneyDate(), activeStatuses);

        int requestedSeats = request.getPassengers().size();
        int availableSeats = trainClass.getTotalSeats() - alreadyBooked;

        if (requestedSeats > availableSeats) {
            throw new SeatUnavailableException(String.format(
                    "Insufficient seats available. Requested: %d, Available: %d for class %s on %s",
                    requestedSeats, availableSeats, trainClass.getClassName(), request.getJourneyDate()));
        }

        // 5. Calculate total fare
        BigDecimal totalFare = trainClass.getFare().multiply(BigDecimal.valueOf(requestedSeats));

        // 6. Generate unique 10-character PNR
        String pnr = generateUniquePnr();

        // 7. Create and persist Booking record with PENDING status awaiting payment (RRS-F-008, SAD Section 4.2.1)
        Booking booking = new Booking(pnr, user, train, trainClass, request.getJourneyDate(), totalFare);
        booking.setStatus(BookingStatus.PENDING);
        booking = bookingRepository.save(booking);

        // 8. Assign seats and persist passengers (RRS-F-007)
        List<PassengerDto> passengerDtos = new ArrayList<>();
        String classPrefix = trainClass.getClassName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (classPrefix.length() > 3) {
            classPrefix = classPrefix.substring(0, 3);
        }

        for (int i = 0; i < requestedSeats; i++) {
            PassengerDto pDto = request.getPassengers().get(i);
            String seatNo = classPrefix + "-" + (alreadyBooked + i + 1);

            Passenger passenger = new Passenger(pDto.getName().trim(), pDto.getAge(), pDto.getGender().trim(), seatNo);
            booking.addPassenger(passenger);
            passengerRepository.save(passenger);

            passengerDtos.add(new PassengerDto(passenger.getName(), passenger.getAge(), passenger.getGender(), seatNo));
        }

        // 9. Initiate Payment Session with Sandbox Payment Gateway (RRS-F-008, TC-PAY-01)
        PaymentSessionDto paymentSession = paymentService.initiatePaymentSession(booking);

        return mapToResponseDto(booking, passengerDtos, paymentSession);
    }

    @Override
    @Transactional(readOnly = true)
    public PnrStatusResponseDto getPublicPnrStatus(String pnr) {
        return mapToPublicDto(findBookingByPnr(pnr));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getTicketByPnr(String pnr, Long userId) {
        Booking booking = bookingRepository.findByPnrAndUserId(pnr, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        requireVerifiedPayment(booking);
        return mapToFullTicketDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getTicketByPnrForAdmin(String pnr) {
        Booking booking = findBookingByPnr(pnr);
        requireVerifiedPayment(booking);
        return mapToFullTicketDto(booking);
    }

    private Booking findBookingByPnr(String pnr) {
        return bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));
    }

    private void requireVerifiedPayment(Booking booking) {
        Payment payment = booking.getPayment();
        boolean verified = payment != null
                && payment.getPaidAt() != null
                && ("SUCCESS".equalsIgnoreCase(payment.getStatus())
                    || "CONFIRMED".equalsIgnoreCase(payment.getStatus()))
                && booking.getStatus() == BookingStatus.CONFIRMED;

        if (!verified) {
            throw new PaymentNotVerifiedException("Payment has not been verified for this booking.");
        }
    }

    private BookingResponseDto mapToFullTicketDto(Booking booking) {
        List<PassengerDto> passengerDtos = booking.getPassengers().stream()
                .map(p -> new PassengerDto(p.getName(), p.getAge(), p.getGender(), p.getSeatNo()))
                .toList();

        return mapToResponseDto(booking, passengerDtos);
    }

    private PnrStatusResponseDto mapToPublicDto(Booking booking) {
        Train train = booking.getTrain();
        TrainClass trainClass = booking.getTrainClass();
        List<String> seatNumbers = booking.getPassengers().stream()
                .map(Passenger::getSeatNo)
                .filter(Objects::nonNull)
                .toList();

        return new PnrStatusResponseDto(
                booking.getPnr(),
                booking.getStatus(),
                train.getTrainNo(),
                train.getName(),
                train.getSourceStation(),
                train.getDestinationStation(),
                booking.getJourneyDate(),
                trainClass.getClassName(),
                booking.getTotalFare(),
                seatNumbers);
    }

    private String generateUniquePnr() {
        String pnr;
        do {
            StringBuilder sb = new StringBuilder(PNR_LENGTH);
            for (int i = 0; i < PNR_LENGTH; i++) {
                sb.append(PNR_CHARACTERS.charAt(random.nextInt(PNR_CHARACTERS.length())));
            }
            pnr = sb.toString();
        } while (bookingRepository.findByPnr(pnr).isPresent());
        return pnr;
    }

    private BookingResponseDto mapToResponseDto(Booking booking, List<PassengerDto> passengerDtos, PaymentSessionDto paymentSession) {
        BookingResponseDto dto = mapToResponseDto(booking, passengerDtos);
        if (paymentSession != null) {
            dto.setPaymentUrl(paymentSession.getPaymentUrl());
            dto.setPaymentStatus(paymentSession.getStatus());
            dto.setGatewayRef(paymentSession.getGatewayRef());
        }
        return dto;
    }

    private BookingResponseDto mapToResponseDto(Booking booking, List<PassengerDto> passengerDtos) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setPnr(booking.getPnr());
        dto.setStatus(booking.getStatus());
        dto.setBookingId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setPassengerAccountName(booking.getUser().getName());

        Train train = booking.getTrain();
        dto.setTrainId(train.getId());
        dto.setTrainNo(train.getTrainNo());
        dto.setTrainName(train.getName());
        dto.setSourceStation(train.getSourceStation());
        dto.setDestinationStation(train.getDestinationStation());
        dto.setDepartureTime(train.getDepartureTime());
        dto.setArrivalTime(train.getArrivalTime());

        TrainClass trainClass = booking.getTrainClass();
        dto.setClassName(trainClass.getClassName());
        dto.setFarePerSeat(trainClass.getFare());
        dto.setTotalFare(booking.getTotalFare());

        dto.setJourneyDate(booking.getJourneyDate());
        dto.setBookingTime(booking.getCreatedAt());
        dto.setPassengers(passengerDtos);

        if (booking.getPayment() != null) {
            dto.setPaymentStatus(booking.getPayment().getStatus());
            dto.setGatewayRef(booking.getPayment().getGatewayRef());
            dto.setPaymentUrl("/payment.html?pnr=" + booking.getPnr() +
                    "&gatewayRef=" + booking.getPayment().getGatewayRef() +
                    "&amount=" + booking.getTotalFare() +
                    "&trainNo=" + booking.getTrain().getTrainNo());
        }

        return dto;
    }
}

