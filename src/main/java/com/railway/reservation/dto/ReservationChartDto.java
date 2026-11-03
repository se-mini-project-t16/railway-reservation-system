package com.railway.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationChartDto {

    private Long trainId;
    private String trainNo;
    private String trainName;
    private String sourceStation;
    private String destinationStation;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime departureTime;

    private LocalDate journeyDate;
    private int totalPassengers;
    private List<ReservationChartEntryDto> entries = new ArrayList<>();

    public ReservationChartDto() {}

    public Long getTrainId() { return trainId; }
    public void setTrainId(Long trainId) { this.trainId = trainId; }

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }

    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }

    public String getSourceStation() { return sourceStation; }
    public void setSourceStation(String sourceStation) { this.sourceStation = sourceStation; }

    public String getDestinationStation() { return destinationStation; }
    public void setDestinationStation(String destinationStation) { this.destinationStation = destinationStation; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }

    public int getTotalPassengers() { return totalPassengers; }
    public void setTotalPassengers(int totalPassengers) { this.totalPassengers = totalPassengers; }

    public List<ReservationChartEntryDto> getEntries() { return entries; }
    public void setEntries(List<ReservationChartEntryDto> entries) { this.entries = entries; }
}

