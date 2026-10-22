package com.railway.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class UpdateTrainDto {

    @NotBlank(message = "Train number must not be blank")
    @Size(max = 20, message = "Train number cannot exceed 20 characters")
    private String trainNo;

    @NotBlank(message = "Train name must not be blank")
    @Size(max = 120, message = "Train name cannot exceed 120 characters")
    private String name;

    @NotBlank(message = "Source station must not be blank")
    @Size(max = 80, message = "Source station cannot exceed 80 characters")
    private String sourceStation;

    @NotBlank(message = "Destination station must not be blank")
    @Size(max = 80, message = "Destination station cannot exceed 80 characters")
    private String destinationStation;

    @NotNull(message = "Departure time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime arrivalTime;

    @NotEmpty(message = "Train must have at least one class")
    @Valid
    private List<TrainClassDto> classes = new ArrayList<>();

    public UpdateTrainDto() {}

    public UpdateTrainDto(String trainNo, String name, String sourceStation, String destinationStation,
                          LocalTime departureTime, LocalTime arrivalTime, List<TrainClassDto> classes) {
        this.trainNo = trainNo;
        this.name = name;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.classes = classes != null ? classes : new ArrayList<>();
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

    public List<TrainClassDto> getClasses() {
        return classes;
    }

    public void setClasses(List<TrainClassDto> classes) {
        this.classes = classes;
    }
}
