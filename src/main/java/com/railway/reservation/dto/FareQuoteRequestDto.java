package com.railway.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class FareQuoteRequestDto {

    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long trainId;
    private LocalDate journeyDate;

    @NotEmpty(message = "At least one passenger is required")
    @Valid
    private List<PassengerDto> passengers;

    public FareQuoteRequestDto() {}

    public FareQuoteRequestDto(Long classId, List<PassengerDto> passengers) {
        this.classId = classId;
        this.passengers = passengers;
    }

    public FareQuoteRequestDto(Long classId, Long trainId, LocalDate journeyDate, List<PassengerDto> passengers) {
        this.classId = classId;
        this.trainId = trainId;
        this.journeyDate = journeyDate;
        this.passengers = passengers;
    }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public Long getTrainId() { return trainId; }
    public void setTrainId(Long trainId) { this.trainId = trainId; }

    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }

    public List<PassengerDto> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerDto> passengers) { this.passengers = passengers; }
}

