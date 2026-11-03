package com.railway.reservation.model;

import jakarta.persistence.*;

import java.time.LocalTime;

/**
 * One halt on a train's route. Times are stored as minutes after midnight of
 * the day the train leaves its origin, so a stop at 02:10 on the second day is
 * 1570. That keeps overnight and multi-day journeys exact.
 */
@Entity
@Table(name = "train_stops", uniqueConstraints = @UniqueConstraint(columnNames = {"train_id", "seq"}))
public class TrainStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id")
    private Train train;

    @Column(nullable = false)
    private int seq;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "station_id")
    private Station station;

    /** Minutes after origin-day midnight; null at the origin. */
    @Column(name = "arrival_min")
    private Integer arrivalMinute;

    /** Minutes after origin-day midnight; null at the terminus. */
    @Column(name = "departure_min")
    private Integer departureMinute;

    @Column(name = "distance_km", nullable = false)
    private int distanceKm;

    protected TrainStop() {}

    public TrainStop(int seq, Station station, Integer arrivalMinute, Integer departureMinute, int distanceKm) {
        this.seq = seq;
        this.station = station;
        this.arrivalMinute = arrivalMinute;
        this.departureMinute = departureMinute;
        this.distanceKm = distanceKm;
    }

    public static LocalTime clock(Integer minute) {
        return minute == null ? null : LocalTime.of((minute / 60) % 24, minute % 60);
    }

    /** 1 for the day the train leaves its origin, 2 for the next day, and so on. */
    public static Integer dayOf(Integer minute) {
        return minute == null ? null : minute / 1440 + 1;
    }

    public LocalTime getArrivalTime() { return clock(arrivalMinute); }
    public LocalTime getDepartureTime() { return clock(departureMinute); }
    public Integer getArrivalDay() { return dayOf(arrivalMinute); }
    public Integer getDepartureDay() { return dayOf(departureMinute); }

    public int getHaltMinutes() {
        return arrivalMinute == null || departureMinute == null ? 0 : departureMinute - arrivalMinute;
    }

    public Long getId() { return id; }
    public Train getTrain() { return train; }
    void setTrain(Train train) { this.train = train; }
    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }
    public Station getStation() { return station; }
    public Integer getArrivalMinute() { return arrivalMinute; }
    public void setArrivalMinute(Integer arrivalMinute) { this.arrivalMinute = arrivalMinute; }
    public Integer getDepartureMinute() { return departureMinute; }
    public void setDepartureMinute(Integer departureMinute) { this.departureMinute = departureMinute; }
    public int getDistanceKm() { return distanceKm; }
    public void setDistanceKm(int distanceKm) { this.distanceKm = distanceKm; }
}
