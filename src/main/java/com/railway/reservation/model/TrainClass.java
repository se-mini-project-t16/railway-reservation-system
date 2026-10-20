package com.railway.reservation.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "classes")
public class TrainClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    @JsonBackReference
    private Train train;

    @Column(name = "class_name", nullable = false, length = 50)
    private String className;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    public TrainClass() {}

    public TrainClass(String className, int totalSeats, BigDecimal fare) {
        this.className = className;
        this.totalSeats = totalSeats;
        this.fare = fare;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }
}

