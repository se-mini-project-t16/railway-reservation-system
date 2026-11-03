package com.railway.reservation.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String pnr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private TrainClass trainClass;

    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "total_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFare;

    @Column(name = "base_fare", precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "superfast_charge", precision = 10, scale = 2)
    private BigDecimal superfastCharge;

    @Column(name = "reservation_fee", precision = 10, scale = 2)
    private BigDecimal reservationFee;

    @Column(name = "gst", precision = 10, scale = 2)
    private BigDecimal gst;

    @Column(name = "senior_discount", precision = 10, scale = 2)
    private BigDecimal seniorDiscount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Passenger> passengers = new ArrayList<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    public Booking() {}

    public Booking(String pnr, User user, Train train, TrainClass trainClass, LocalDate journeyDate, BigDecimal totalFare) {
        this.pnr = pnr;
        this.user = user;
        this.train = train;
        this.trainClass = trainClass;
        this.journeyDate = journeyDate;
        this.totalFare = totalFare;
        this.status = BookingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }

    public TrainClass getTrainClass() { return trainClass; }
    public void setTrainClass(TrainClass trainClass) { this.trainClass = trainClass; }

    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }

    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

    public BigDecimal getSuperfastCharge() { return superfastCharge; }
    public void setSuperfastCharge(BigDecimal superfastCharge) { this.superfastCharge = superfastCharge; }

    public BigDecimal getReservationFee() { return reservationFee; }
    public void setReservationFee(BigDecimal reservationFee) { this.reservationFee = reservationFee; }

    public BigDecimal getGst() { return gst; }
    public void setGst(BigDecimal gst) { this.gst = gst; }

    public BigDecimal getSeniorDiscount() { return seniorDiscount; }
    public void setSeniorDiscount(BigDecimal seniorDiscount) { this.seniorDiscount = seniorDiscount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Passenger> getPassengers() { return passengers; }
    public void setPassengers(List<Passenger> passengers) { this.passengers = passengers; }

    public void addPassenger(Passenger passenger) {
        passengers.add(passenger);
        passenger.setBooking(this);
    }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) {
        this.payment = payment;
        if (payment != null) payment.setBooking(this);
    }
}

