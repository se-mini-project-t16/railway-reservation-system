package com.railway.reservation.service.impl;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.FareBreakdownDto;
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
import com.railway.reservation.validation.SafeTextRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    @Transactional(readOnly = true)
    public FareBreakdownDto calculateFareQuote(Long classId, List<PassengerDto> passengers) {
        if (classId == null) {
            throw new IllegalArgumentException("Class ID cannot be null");
        }
        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required for fare calculation");
        }

        TrainClass trainClass = trainClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        BigDecimal seatFare = trainClass.getFare();
        String className = trainClass.getClassName() != null ? trainClass.getClassName().toUpperCase() : "";
        boolean isAc = className.contains("AC") || className.contains("1A") || className.contains("2A")
                || className.contains("3A") || className.contains("CC") || className.contains("EC")
                || className.contains("EXECUTIVE");

        BigDecimal resFeePerSeat = isAc ? BigDecimal.valueOf(40.00) : BigDecimal.valueOf(20.00);
        BigDecimal sfChargePerSeat = isAc ? BigDecimal.valueOf(30.00) : BigDecimal.valueOf(15.00);

        if (seatFare.compareTo(resFeePerSeat.add(sfChargePerSeat)) <= 0) {
            resFeePerSeat = BigDecimal.ZERO;
            sfChargePerSeat = BigDecimal.ZERO;
        }

        BigDecimal taxablePerSeat = seatFare.subtract(resFeePerSeat).subtract(sfChargePerSeat);
        BigDecimal basePerSeat;
        BigDecimal gstPerSeat;

        if (isAc) {
            basePerSeat = taxablePerSeat.divide(BigDecimal.valueOf(1.05), 2, RoundingMode.HALF_UP);
            gstPerSeat = taxablePerSeat.subtract(basePerSeat);
        } else {
            basePerSeat = taxablePerSeat;
            gstPerSeat = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        int passengerCount = passengers.size();
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalSf = BigDecimal.ZERO;
        BigDecimal totalRes = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        BigDecimal totalSeniorDiscount = BigDecimal.ZERO;
        int eligibleSeniorCount = 0;

        for (PassengerDto p : passengers) {
            totalBase = totalBase.add(basePerSeat);
            totalSf = totalSf.add(sfChargePerSeat);
            totalRes = totalRes.add(resFeePerSeat);
            totalGst = totalGst.add(gstPerSeat);

            if (p != null && p.getAge() >= 60 && Boolean.TRUE.equals(p.getSeniorConcession())) {
                eligibleSeniorCount++;
                BigDecimal discount = basePerSeat.multiply(BigDecimal.valueOf(0.40)).setScale(2, RoundingMode.HALF_UP);
                totalSeniorDiscount = totalSeniorDiscount.add(discount);
            }
        }

        BigDecimal totalPayable = totalBase.add(totalSf).add(totalRes).add(totalGst).subtract(totalSeniorDiscount);

        return new FareBreakdownDto(
                passengerCount,
                seatFare.setScale(2, RoundingMode.HALF_UP),
                totalBase.setScale(2, RoundingMode.HALF_UP),
                totalSf.setScale(2, RoundingMode.HALF_UP),
                totalRes.setScale(2, RoundingMode.HALF_UP),
                totalGst.setScale(2, RoundingMode.HALF_UP),
                totalSeniorDiscount.setScale(2, RoundingMode.HALF_UP),
                totalPayable.setScale(2, RoundingMode.HALF_UP),
                eligibleSeniorCount,
                isAc
        );
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

        // Validate each passenger (including senior citizen concession eligibility)
        for (PassengerDto pDto : request.getPassengers()) {
            if (pDto.getAge() < 60 && Boolean.TRUE.equals(pDto.getSeniorConcession())) {
                throw new IllegalArgumentException(
                        "Senior citizen concession is only valid for passengers aged 60 and above (Passenger: " + pDto.getName() + ")");
            }
        }

        // 5. Calculate authoritative itemized fare breakdown (RRS-F-007-EXT)
        FareBreakdownDto fareQuote = calculateFareQuote(trainClass.getId(), request.getPassengers());
        BigDecimal standardFare = trainClass.getFare().multiply(BigDecimal.valueOf(requestedSeats));
        BigDecimal totalFare = fareQuote.getSeniorDiscount().compareTo(BigDecimal.ZERO) > 0
                ? standardFare.subtract(fareQuote.getSeniorDiscount())
                : standardFare;

        // 6. Generate unique 10-character PNR
        String pnr = generateUniquePnr();

        // 7. Create and persist Booking record with PENDING status awaiting payment (RRS-F-008, SAD Section 4.2.1)
        Booking booking = new Booking(pnr, user, train, trainClass, request.getJourneyDate(), totalFare);
        booking.setBaseFare(fareQuote.getBaseFare());
        booking.setSuperfastCharge(fareQuote.getSuperfastCharge());
        booking.setReservationFee(fareQuote.getReservationFee());
        booking.setGst(fareQuote.getGst());
        booking.setSeniorDiscount(fareQuote.getSeniorDiscount());
        booking.setStatus(BookingStatus.PENDING);
        booking = bookingRepository.save(booking);

        // 8. Assign seats and persist passengers (RRS-F-007, RRS-F-007-EXT)
        List<PassengerDto> passengerDtos = new ArrayList<>();
        String classPrefix = trainClass.getClassName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (classPrefix.length() > 3) {
            classPrefix = classPrefix.substring(0, 3);
        }

        // Reuse the lowest seat numbers not held by an active booking, so seats
        // released by cancellation (RRS-F-012) go back to the pool without duplicates
        Set<Integer> takenSeats = new HashSet<>();
        String seatPrefix = classPrefix + "-";
        for (String taken : bookingRepository.findActiveSeatNumbers(
                train.getId(), trainClass.getId(), request.getJourneyDate(), activeStatuses)) {
            if (taken != null && taken.startsWith(seatPrefix)) {
                try {
                    takenSeats.add(Integer.parseInt(taken.substring(seatPrefix.length())));
                } catch (NumberFormatException ignored) {
                    // Not in the classPrefix-N format; cannot collide with generated numbers
                }
            }
        }

        int nextSeat = 0;
        for (int i = 0; i < requestedSeats; i++) {
            PassengerDto pDto = request.getPassengers().get(i);
            do {
                nextSeat++;
            } while (takenSeats.contains(nextSeat));
            String seatNo = seatPrefix + nextSeat;

            Passenger passenger = new Passenger(
                    pDto.getName().trim(),
                    pDto.getAge(),
                    pDto.getGender().trim(),
                    seatNo,
                    pDto.getBerthPreference(),
                    pDto.getMealPreference(),
                    pDto.getSeniorConcession()
            );
            booking.addPassenger(passenger);
            passengerRepository.save(passenger);

            passengerDtos.add(mapToPassengerDto(passenger));
        }

        // 9. Initiate Payment Session with Sandbox Payment Gateway (RRS-F-008, TC-PAY-01)
        PaymentSessionDto paymentSession = paymentService.initiatePaymentSession(booking);

        return mapToResponseDto(booking, passengerDtos, paymentSession);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingByPnr(String pnr) {
        Booking booking = findBookingByPnr(pnr);
        List<PassengerDto> passengerDtos = booking.getPassengers().stream()
                .map(this::mapToPassengerDto)
                .toList();

        return mapToResponseDto(booking, passengerDtos);
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

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingHistory(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null.");
        }
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<Booking> bookings = bookingRepository.findByUserIdOrderByJourneyDateDescCreatedAtDesc(userId);
        return bookings.stream()
                .map(b -> {
                    List<PassengerDto> passengerDtos = b.getPassengers().stream()
                            .map(this::mapToPassengerDto)
                            .toList();
                    return mapToResponseDto(b, passengerDtos);
                })
                .toList();
    }

    private Booking findBookingByPnr(String pnr) {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new ResourceNotFoundException("Booking not found with PNR: " + pnr);
        }
        SafeTextRules.requireSafe("PNR", pnr);
        String cleanPnr = pnr.trim();
        return bookingRepository.findByPnr(cleanPnr)
                .or(() -> bookingRepository.findByPnr(cleanPnr.toUpperCase()))
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
                .map(this::mapToPassengerDto)
                .toList();

        return mapToResponseDto(booking, passengerDtos);
    }

    private PassengerDto mapToPassengerDto(Passenger p) {
        PassengerDto dto = new PassengerDto(
                p.getName(),
                p.getAge(),
                p.getGender(),
                p.getSeatNo(),
                p.getBerthPreference(),
                p.getMealPreference(),
                p.getSeniorConcession()
        );
        dto.setId(p.getId());
        dto.setStatus(p.getStatus() != null ? p.getStatus() : "CONFIRMED");
        return dto;
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

        if (booking.getBaseFare() != null) {
            dto.setBaseFare(booking.getBaseFare());
            dto.setSuperfastCharge(booking.getSuperfastCharge());
            dto.setReservationFee(booking.getReservationFee());
            dto.setGst(booking.getGst());
            dto.setSeniorDiscount(booking.getSeniorDiscount());
        } else if (trainClass != null && passengerDtos != null && !passengerDtos.isEmpty()) {
            try {
                FareBreakdownDto quote = calculateFareQuote(trainClass.getId(), passengerDtos);
                dto.setBaseFare(quote.getBaseFare());
                dto.setSuperfastCharge(quote.getSuperfastCharge());
                dto.setReservationFee(quote.getReservationFee());
                dto.setGst(quote.getGst());
                dto.setSeniorDiscount(quote.getSeniorDiscount());
            } catch (Exception ignored) {
                // Keep default nulls if historical booking cannot re-resolve trainClass
            }
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        boolean upcoming = !booking.getJourneyDate().isBefore(today);

        LocalTime deptTime = train.getDepartureTime() != null ? train.getDepartureTime() : LocalTime.MIN;
        LocalDateTime departureDateTime = LocalDateTime.of(booking.getJourneyDate(), deptTime);
        boolean cancellable = booking.getStatus() == BookingStatus.CONFIRMED && now.isBefore(departureDateTime);

        dto.setUpcoming(upcoming);
        dto.setCancellable(cancellable);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            dto.setTicketUrl("/ticket.html?pnr=" + booking.getPnr());
        }
        if (cancellable) {
            dto.setCancelUrl("/api/v1/bookings/" + booking.getPnr() + "/cancel");
        }

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
