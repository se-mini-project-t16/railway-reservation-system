package com.railway.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TrainResponseDto {

    private Long id;
    private String trainNo;
    private String name;
    private String sourceStation;
    private String destinationStation;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime departureTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime arrivalTime;

    private List<TrainClassResponseDto> classes = new ArrayList<>();
    private List<TrainStopDto> stops = new ArrayList<>();

    public TrainResponseDto() {}

    public TrainResponseDto(Long id, String trainNo, String name, String sourceStation,
                            String destinationStation, LocalTime departureTime,
                            LocalTime arrivalTime, List<TrainClassResponseDto> classes) {
        this.id = id;
        this.trainNo = trainNo;
        this.name = name;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.classes = classes != null ? classes : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<TrainClassResponseDto> getClasses() {
        return classes;
    }

    public void setClasses(List<TrainClassResponseDto> classes) {
        this.classes = classes;
    }

    public List<TrainStopDto> getStops() {
        return stops;
    }

    public void setStops(List<TrainStopDto> stops) {
        this.stops = stops;
    }
}
