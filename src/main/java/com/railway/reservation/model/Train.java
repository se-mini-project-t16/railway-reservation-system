package com.railway.reservation.model;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "trains")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String number;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainType type;

    /** Seven characters, Monday first: '1' when the train leaves its origin that day. */
    @Column(name = "running_days", nullable = false, length = 7)
    private String runningDays = "1111111";

    @Column(nullable = false)
    private boolean pantry;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<TrainStop> stops = new ArrayList<>();

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainClass> classes = new ArrayList<>();

    protected Train() {}

    public Train(String number, String name, TrainType type, String runningDays, boolean pantry) {
        this.number = number;
        this.name = name;
        this.type = type;
        this.runningDays = runningDays;
        this.pantry = pantry;
    }

    public boolean runsOn(LocalDate originDate) {
        return runsOn(originDate.getDayOfWeek());
    }

    public boolean runsOn(DayOfWeek day) {
        return runningDays.charAt(day.getValue() - 1) == '1';
    }

    /** Superfast surcharge applies to every train numbered in the 12xxx, 20xxx and 22xxx series. */
    public boolean isSuperfast() {
        return type != TrainType.MAIL_EXPRESS || number.startsWith("12") || number.startsWith("2");
    }

    public void addStop(TrainStop stop) {
        stop.setTrain(this);
        stops.add(stop);
    }

    public void addClass(TrainClass trainClass) {
        trainClass.setTrain(this);
        classes.add(trainClass);
    }

    public List<TrainStop> orderedStops() {
        return stops.stream().sorted(Comparator.comparingInt(TrainStop::getSeq)).toList();
    }

    public TrainStop origin() { return orderedStops().getFirst(); }
    public TrainStop terminus() { return orderedStops().getLast(); }

    public Optional<TrainStop> stopFor(String stationCode) {
        return stops.stream().filter(s -> s.getStation().getCode().equalsIgnoreCase(stationCode)).findFirst();
    }

    public Optional<TrainClass> classOf(CoachClass coachClass) {
        return classes.stream().filter(c -> c.getCoachClass() == coachClass).findFirst();
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TrainType getType() { return type; }
    public void setType(TrainType type) { this.type = type; }
    public String getRunningDays() { return runningDays; }
    public void setRunningDays(String runningDays) { this.runningDays = runningDays; }
    public boolean isPantry() { return pantry; }
    public void setPantry(boolean pantry) { this.pantry = pantry; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<TrainStop> getStops() { return stops; }
    public List<TrainClass> getClasses() { return classes; }
}
