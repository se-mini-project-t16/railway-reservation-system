package com.railway.reservation.dto;

import com.railway.reservation.dto.TrainDtos.ClassAvailability;
import com.railway.reservation.dto.TrainDtos.EndPoint;
import com.railway.reservation.dto.TrainDtos.FareDto;
import com.railway.reservation.model.BerthPreference;
import com.railway.reservation.model.FoodChoice;
import com.railway.reservation.model.Gender;
import com.railway.reservation.model.PaymentMethod;
import com.railway.reservation.validation.SafeText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Requests and read models for booking, payment, tickets, PNR status and cancellation. */
public final class BookingDtos {

    private BookingDtos() {}

    public static final String NAME_PATTERN = "^[A-Za-z][A-Za-z .']{1,39}$";
    public static final String MOBILE_PATTERN = "^[6-9]\\d{9}$";

    // ------------------------------------------------------------------ requests

    public record PassengerInput(
            @NotBlank(message = "Enter the traveller's name.")
            @Pattern(regexp = NAME_PATTERN, message = "Use 2 to 40 letters; spaces, dots and apostrophes are allowed.")
            @SafeText String name,
            @NotNull(message = "Enter the traveller's age.")
            @Min(value = 5, message = "Children under 5 travel free without a berth and are not added to the ticket.")
            @Max(value = 120, message = "Enter a valid age.") Integer age,
            @NotNull(message = "Choose a gender.") Gender gender,
            BerthPreference berthPreference,
            FoodChoice food) {}

    public record BookingRequest(
            @NotBlank(message = "Choose a train.") String trainNumber,
            @NotBlank(message = "Choose a boarding station.") String from,
            @NotBlank(message = "Choose a destination station.") String to,
            @NotNull(message = "Choose a journey date.") LocalDate date,
            @NotBlank(message = "Choose a class.") String classCode,
            String quota,
            @NotEmpty(message = "Add at least one traveller.")
            @Size(max = 6, message = "A ticket can carry at most 6 travellers.")
            List<@Valid PassengerInput> passengers,
            @NotBlank(message = "Enter a mobile number for journey alerts.")
            @Pattern(regexp = MOBILE_PATTERN, message = "Enter a 10-digit Indian mobile number.") String mobile,
            Boolean onlyIfConfirmed,
            Boolean saveTravellers) {

        public boolean confirmedOnly() { return Boolean.TRUE.equals(onlyIfConfirmed); }
        public boolean rememberTravellers() { return Boolean.TRUE.equals(saveTravellers); }
    }

    public record QuoteRequest(
            @NotBlank String trainNumber, @NotBlank String from, @NotBlank String to, @NotNull LocalDate date,
            @NotBlank String classCode, String quota,
            @NotNull @Min(1) @Max(6) Integer passengers) {}

    public record CancelRequest(@NotEmpty(message = "Choose at least one traveller to cancel.") List<Long> passengerIds) {}

    public record PaymentRequest(
            @NotNull(message = "Choose how to pay.") PaymentMethod method,
            String upiId, String cardNumber, String cardExpiry, String cardCvv, String cardName, String bankCode) {}

    // ------------------------------------------------------------------ views

    public record TrainRef(String number, String name, String type, String typeLabel) {}

    /** A traveller's status: CNF with coach and berth, RAC or WL with position, or CAN. */
    public record StatusView(String status, String coach, Integer berth, String berthType, Integer number, String text) {}

    public record PassengerView(Long id, int serial, String name, int age, String gender, String berthPreference,
                                String food, StatusView bookingStatus, StatusView currentStatus, int fare,
                                Integer refund, boolean cancellable) {}

    public record PaymentView(String txnRef, String method, String instrument, int amount, String status,
                              String failureReason, LocalDateTime at, int refunded) {}

    public record CancellationView(LocalDateTime at, String reason, int passengers, int refund) {}

    public record BookingView(String pnr, String status, String statusLabel, TrainRef train, String classCode,
                              String classLabel, String quota, String quotaLabel, EndPoint from, EndPoint to,
                              LocalDate journeyDate, int durationMinutes, int distanceKm, FareDto fare, int perPassenger,
                              int refundTotal, String contactMobile, String contactEmail, String bookedBy,
                              LocalDateTime createdAt, LocalDateTime paymentDueAt, LocalDateTime bookedAt,
                              boolean chartPrepared, LocalDateTime chartAt, boolean cancellable,
                              List<PassengerView> passengers, List<PaymentView> payments,
                              List<CancellationView> cancellations) {}

    public record BookingSummary(String pnr, String status, String statusLabel, TrainRef train, String classCode,
                                 String quota, EndPoint from, EndPoint to, LocalDateTime departureAt,
                                 int travellers, int activeTravellers, String seatSummary, int totalFare,
                                 int refundTotal, LocalDateTime paymentDueAt) {}

    public record PnrPassenger(int serial, StatusView bookingStatus, StatusView currentStatus) {}

    public record PnrStatus(String pnr, String status, String statusLabel, TrainRef train, String classCode,
                            String classLabel, String quota, EndPoint from, EndPoint to, LocalDate journeyDate,
                            boolean chartPrepared, LocalDateTime chartAt, List<PnrPassenger> passengers) {}

    public record Quote(TrainRef train, EndPoint from, EndPoint to, String classCode, String classLabel,
                        String quota, int distanceKm, int durationMinutes, FareDto perPassenger, FareDto total,
                        int passengers, ClassAvailability availability, boolean pantry, int maxPassengers,
                        List<String> berthPreferences, LocalDateTime tatkalOpensAt) {}

    public record RefundLine(Long passengerId, String name, String status, int fare, int deduction, int refund,
                             String rule) {}

    public record CancelPreview(String pnr, List<RefundLine> lines, int totalFare, int totalDeduction,
                                int totalRefund, long minutesBeforeDeparture) {}

    public record PaymentResult(String outcome, String message, String txnRef, BookingView booking) {}
}
