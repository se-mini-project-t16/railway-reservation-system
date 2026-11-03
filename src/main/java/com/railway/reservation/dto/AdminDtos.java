package com.railway.reservation.dto;

import com.railway.reservation.validation.SafeText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {}

    // ------------------------------------------------------------------ overview

    public record DayFigure(LocalDate date, int bookings, int travellers, int revenue, int refunds) {}

    public record ClassFigure(String code, String label, int travellers) {}

    public record UpcomingDeparture(String trainNumber, String trainName, LocalDate startDate, String departure,
                                    String originCode, int bookings, int travellers, boolean chartPrepared) {}

    public record Overview(LocalDate today, int bookingsToday, int travellersToday, int revenueToday,
                           int cancellationsToday, int refundsToday, int pendingPayments, int activeTrains,
                           int totalTrains, int passengers, List<DayFigure> lastWeek, List<ClassFigure> classMix,
                           List<UpcomingDeparture> upcoming) {}

    // ------------------------------------------------------------------ fleet

    public record TrainRow(String number, String name, String type, String typeLabel, String runningDays,
                           boolean active, String originCode, String originName, String departure,
                           String terminusCode, String terminusName, String arrival, int days,
                           List<String> classes, long bookings) {}

    public record StopForm(
            @NotBlank(message = "Choose a station.") String code,
            @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "Use HH:MM.") String arrival,
            @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "Use HH:MM.") String departure,
            @NotNull @Min(1) @Max(4) Integer day,
            @NotNull @Min(0) @Max(5000) Integer distanceKm) {}

    public record ClassForm(@NotBlank String code, @NotNull @Min(1) @Max(24) Integer coaches) {}

    public record TrainForm(
            @NotBlank @Pattern(regexp = "^\\d{5}$", message = "A train number is 5 digits.") String number,
            @NotBlank @Size(max = 80) @SafeText String name,
            @NotBlank String type,
            @NotBlank @Pattern(regexp = "^[01]{7}$", message = "Running days are seven 0/1 flags, Monday first.") String runningDays,
            Boolean pantry,
            Boolean active,
            @NotEmpty(message = "Add at least one class.") List<@Valid ClassForm> classes,
            @Size(min = 2, message = "A route needs at least two stations.") List<@Valid StopForm> stops) {}

    public record StationForm(
            @NotBlank @Pattern(regexp = "^[A-Z]{2,5}$", message = "Codes are 2 to 5 capital letters.") String code,
            @NotBlank @Size(max = 80) @SafeText String name,
            @NotBlank @Size(max = 60) @SafeText String city,
            @NotBlank @Size(max = 40) @SafeText String state) {}

    // ------------------------------------------------------------------ bookings and users

    public record BookingRow(String pnr, String status, String statusLabel, String trainNumber, String trainName,
                             String classCode, String quota, String from, String to, LocalDate journeyDate,
                             String departure, int travellers, int activeTravellers, String bookedBy,
                             String email, int totalFare, int refundTotal, LocalDateTime createdAt) {}

    public record PageOf<T>(List<T> items, int page, int size, long total, int pages) {}

    public record UserRow(Long id, String name, String email, String phone, String role, boolean locked,
                          int failedAttempts, LocalDateTime createdAt) {}

    public record RuleForm(@NotNull Long id,
                           @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal deductionPercent,
                           Boolean flatMinimum,
                           @NotBlank @Size(max = 80) @SafeText String description) {}

    // ------------------------------------------------------------------ reservation chart

    public record ChartOccupant(String pnr, int serial, String name, int age, String gender, String from, String to,
                                String bookingStatus) {}

    public record ChartBerth(int berth, String type, boolean otherChannel, List<ChartOccupant> occupants) {}

    public record ChartCoach(String coach, List<ChartBerth> berths) {}

    public record ChartQueueRow(String pnr, int serial, String name, int age, String gender, String from, String to,
                                String status, int number) {}

    public record Chart(String trainNumber, String trainName, String classCode, String classLabel, LocalDate startDate,
                        boolean chartPrepared, LocalDateTime chartAt, int capacity, int tatkalBerths,
                        int otherChannelBerths, int systemTravellers, int freeWholeRun, int otherChannelRac,
                        int otherChannelWl, List<ChartCoach> coaches, List<ChartQueueRow> rac,
                        List<ChartQueueRow> waitlist, List<String> classesOnTrain) {}
}
