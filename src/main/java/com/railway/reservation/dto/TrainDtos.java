package com.railway.reservation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read models for stations, timetables, search results and availability. */
public final class TrainDtos {

    private TrainDtos() {}

    public record StationDto(String code, String name, String city, String state) {}

    public record StopDto(int seq, String code, String name, String city,
                          String arrival, String departure, Integer day,
                          int haltMinutes, int distanceKm) {}

    public record ClassInfo(String code, String label, int coaches, int berthsPerCoach) {}

    /**
     * Availability of one class for one journey.
     * status: AVAILABLE, RAC, WAITLIST, REGRET, NOT_OPEN (Tatkal not open yet), NOT_OFFERED, DEPARTED.
     */
    public record ClassAvailability(String code, String label, Integer fare, String status, int count,
                                    String text, LocalDateTime opensAt) {}

    public record FareDto(int base, int reservation, int superfast, int tatkal, int catering, int gst, int total) {}

    public record EndPoint(String code, String name, String city, String time, LocalDate date, int day) {}

    public record TrainResult(String number, String name, String type, String typeLabel, String runningDays,
                              boolean pantry, EndPoint from, EndPoint to, int durationMinutes, int distanceKm,
                              int stopsBetween, String originCode, String terminusCode,
                              List<ClassAvailability> classes) {}

    public record SearchResponse(StationDto from, StationDto to, LocalDate date, String quota,
                                 List<StationDto> fromStations, List<StationDto> toStations,
                                 List<TrainResult> results) {}

    public record TrainDetail(String number, String name, String type, String typeLabel, String runningDays,
                              boolean pantry, boolean active, int distanceKm, int durationMinutes,
                              List<ClassInfo> classes, List<StopDto> stops) {}

    public record DayAvailability(LocalDate date, boolean runs, ClassAvailability availability) {}

    public record AvailabilityCalendar(String trainNumber, String classCode, String quota, String from, String to,
                                       FareDto fare, List<DayAvailability> days) {}

    public record DepartureRow(String number, String name, String typeLabel, String departure, LocalDate date,
                               String terminusCode, String terminusName, String terminusCity, boolean starts,
                               String arrivalAtTerminus, int terminusDay) {}

    public record StationBoard(StationDto station, LocalDate date, List<DepartureRow> departures) {}

    public record TrainSummary(String number, String name, String typeLabel, String runningDays,
                               String originCode, String originName, String departure,
                               String terminusCode, String terminusName, String arrival, int arrivalDay,
                               int distanceKm, List<String> classes) {}

    public record CancellationRuleDto(int minHoursBefore, String deductionPercent, boolean flatMinimum,
                                      String description) {}

    public record ClassChargeDto(String code, String label, int flatCancellationCharge) {}

    public record RefundPolicy(List<CancellationRuleDto> confirmedRules, List<ClassChargeDto> flatCharges,
                               int waitlistClerkage, String tatkalRule) {}
}
