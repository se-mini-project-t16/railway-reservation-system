package com.railway.reservation.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trains")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "train_id")
    private Long id;

    @Column(name = "train_no", nullable = false, unique = true, length = 20)
    private String trainNo;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "src_station", nullable = false, length = 80)
    private String sourceStation;

    @Column(name = "dst_station", nullable = false, length = 80)
    private String destinationStation;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<TrainClass> classes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "train_stops", joinColumns = @JoinColumn(name = "train_id"))
    @OrderBy("stopOrder ASC")
    private List<TrainStop> stops = new ArrayList<>();

    public Train() {}

    public Train(String trainNo, String name, String sourceStation, String destinationStation, LocalTime departureTime, LocalTime arrivalTime) {
        this.trainNo = trainNo;
        this.name = name;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSourceStation() { return sourceStation; }
    public void setSourceStation(String sourceStation) { this.sourceStation = sourceStation; }

    public String getDestinationStation() { return destinationStation; }
    public void setDestinationStation(String destinationStation) { this.destinationStation = destinationStation; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public List<TrainClass> getClasses() { return classes; }
    public void setClasses(List<TrainClass> classes) { this.classes = classes; }

    public void addClass(TrainClass trainClass) {
        classes.add(trainClass);
        trainClass.setTrain(this);
    }

    public List<TrainStop> getStops() {
        return stops;
    }

    public void setStops(List<TrainStop> stops) {
        this.stops = stops;
    }

    public void addStop(TrainStop stop) {
        this.stops.add(stop);
    }
}

