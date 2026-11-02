package com.railway.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class BookingRequestDto {

    // User ID is derived from the authenticated security principal by the server.
    // Client-provided value is optional and will be overwritten for security [RRS-SR-006].
    private Long userId;

    @NotNull(message = "Train ID is required")
    private Long trainId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Journey date is required")
    @FutureOrPresent(message = "Journey date cannot be in the past")
    private LocalDate journeyDate;

    @NotEmpty(message = "At least one passenger is required")
    @Valid
    private List<PassengerDto> passengers;

    public BookingRequestDto() {}

    public BookingRequestDto(Long userId, Long trainId, Long classId, LocalDate journeyDate, List<PassengerDto> passengers) {
        this.userId = userId;
        this.trainId = trainId;
        this.classId = classId;
        this.journeyDate = journeyDate;
        this.passengers = passengers;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTrainId() { return trainId; }
    public void setTrainId(Long trainId) { this.trainId = trainId; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }

    public List<PassengerDto> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerDto> passengers) { this.passengers = passengers; }
}

