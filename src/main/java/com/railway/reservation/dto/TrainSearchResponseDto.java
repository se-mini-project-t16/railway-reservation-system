package com.railway.reservation.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TrainSearchResponseDto {

    private Long trainId;
    private String trainNo;
    private String name;
    private String sourceStation;
    private String destinationStation;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private LocalDate journeyDate;
    private List<SeatAvailabilityDto> classes = new ArrayList<>();
    private List<TrainStopDto> stops = new ArrayList<>();

    public TrainSearchResponseDto() {}

    public TrainSearchResponseDto(Long trainId, String trainNo, String name, String sourceStation,
                                  String destinationStation, LocalTime departureTime,
                                  LocalTime arrivalTime, LocalDate journeyDate,
                                  List<SeatAvailabilityDto> classes) {
        this(trainId, trainNo, name, sourceStation, destinationStation, departureTime, arrivalTime, journeyDate, classes, new ArrayList<>());
    }

    public TrainSearchResponseDto(Long trainId, String trainNo, String name, String sourceStation,
                                  String destinationStation, LocalTime departureTime,
                                  LocalTime arrivalTime, LocalDate journeyDate,
                                  List<SeatAvailabilityDto> classes,
                                  List<TrainStopDto> stops) {
        this.trainId = trainId;
        this.trainNo = trainNo;
        this.name = name;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.journeyDate = journeyDate;
        this.classes = classes != null ? classes : new ArrayList<>();
        this.stops = stops != null ? stops : new ArrayList<>();
    }

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public void setTrainNo(String trainNo) {
        this.trainNo = trainNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceStation() {
        return sourceStation;
    }

    public void setSourceStation(String sourceStation) {
        this.sourceStation = sourceStation;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public LocalDate getJourneyDate() {
        return journeyDate;
    }

    public void setJourneyDate(LocalDate journeyDate) {
        this.journeyDate = journeyDate;
    }

    public List<SeatAvailabilityDto> getClasses() {
        return classes;
    }

    public void setClasses(List<SeatAvailabilityDto> classes) {
        this.classes = classes;
    }

    public List<TrainStopDto> getStops() {
        return stops;
    }

    public void setStops(List<TrainStopDto> stops) {
        this.stops = stops;
    }
}
