package com.railway.reservation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One PNR: a journey on one train, in one class and quota, for up to six travellers. */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_inventory", columnList = "train_id, coach_class, train_start_date"),
        @Index(name = "idx_booking_user", columnList = "user_id")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String pnr;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id")
    private Train train;

    @Enumerated(EnumType.STRING)
    @Column(name = "coach_class", nullable = false, length = 20)
    private CoachClass coachClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Quota quota = Quota.GENERAL;

    @Column(name = "from_seq", nullable = false)
    private int fromSeq;

    @Column(name = "to_seq", nullable = false)
    private int toSeq;

    @Column(name = "from_code", nullable = false, length = 8)
    private String fromCode;

    @Column(name = "to_code", nullable = false, length = 8)
    private String toCode;

    /** Date the traveller boards, at the boarding station. */
    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    /** Date the train leaves its origin; inventory is counted against this date. */
    @Column(name = "train_start_date", nullable = false)
    private LocalDate trainStartDate;

    @Column(name = "departure_at", nullable = false)
    private LocalDateTime departureAt;

    @Column(name = "arrival_at", nullable = false)
    private LocalDateTime arrivalAt;

    @Column(name = "distance_km", nullable = false)
    private int distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BookingStatus status = BookingStatus.PAYMENT_PENDING;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare = BigDecimal.ZERO;
    @Column(name = "reservation_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal reservationCharge = BigDecimal.ZERO;
    @Column(name = "superfast_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal superfastCharge = BigDecimal.ZERO;
    @Column(name = "tatkal_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal tatkalCharge = BigDecimal.ZERO;
    @Column(name = "catering_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal cateringCharge = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal gst = BigDecimal.ZERO;
    @Column(name = "total_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFare = BigDecimal.ZERO;
    @Column(name = "refund_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundTotal = BigDecimal.ZERO;

    @Column(name = "contact_mobile", nullable = false, length = 15)
    private String contactMobile;

    @Column(name = "only_if_confirmed", nullable = false)
    private boolean onlyIfConfirmed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "payment_due_at")
    private LocalDateTime paymentDueAt;

    @Column(name = "booked_at")
    private LocalDateTime bookedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("serial ASC")
    private List<Passenger> passengers = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Payment> payments = new ArrayList<>();

    protected Booking() {}

    public Booking(String pnr, User user, Train train, CoachClass coachClass, Quota quota) {
        this.pnr = pnr;
        this.user = user;
        this.train = train;
        this.coachClass = coachClass;
        this.quota = quota;
    }

    public void addPassenger(Passenger p) {
        p.setBooking(this);
        passengers.add(p);
    }

    public void addPayment(Payment p) {
        p.setBooking(this);
        payments.add(p);
    }

    public List<Passenger> orderedPassengers() {
        return passengers.stream().sorted(Comparator.comparingInt(Passenger::getSerial)).toList();
    }

    public List<Passenger> activePassengers() {
        return orderedPassengers().stream().filter(p -> p.getCurrentStatus() != SeatStatus.CAN).toList();
    }

    public Payment successfulPayment() {
        return payments.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).findFirst().orElse(null);
    }

    public Long getId() { return id; }
    public String getPnr() { return pnr; }
    public User getUser() { return user; }
    public Train getTrain() { return train; }
    public CoachClass getCoachClass() { return coachClass; }
    public Quota getQuota() { return quota; }
    public int getFromSeq() { return fromSeq; }
    public int getToSeq() { return toSeq; }
    public String getFromCode() { return fromCode; }
    public String getToCode() { return toCode; }
    public void setSegment(int fromSeq, String fromCode, int toSeq, String toCode) {
        this.fromSeq = fromSeq; this.fromCode = fromCode; this.toSeq = toSeq; this.toCode = toCode;
    }
    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }
    public LocalDate getTrainStartDate() { return trainStartDate; }
    public void setTrainStartDate(LocalDate trainStartDate) { this.trainStartDate = trainStartDate; }
    public LocalDateTime getDepartureAt() { return departureAt; }
    public void setDepartureAt(LocalDateTime departureAt) { this.departureAt = departureAt; }
    public LocalDateTime getArrivalAt() { return arrivalAt; }
    public void setArrivalAt(LocalDateTime arrivalAt) { this.arrivalAt = arrivalAt; }
    public int getDistanceKm() { return distanceKm; }
    public void setDistanceKm(int distanceKm) { this.distanceKm = distanceKm; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal v) { this.baseFare = v; }
    public BigDecimal getReservationCharge() { return reservationCharge; }
    public void setReservationCharge(BigDecimal v) { this.reservationCharge = v; }
    public BigDecimal getSuperfastCharge() { return superfastCharge; }
    public void setSuperfastCharge(BigDecimal v) { this.superfastCharge = v; }
    public BigDecimal getTatkalCharge() { return tatkalCharge; }
    public void setTatkalCharge(BigDecimal v) { this.tatkalCharge = v; }
    public BigDecimal getCateringCharge() { return cateringCharge; }
    public void setCateringCharge(BigDecimal v) { this.cateringCharge = v; }
    public BigDecimal getGst() { return gst; }
    public void setGst(BigDecimal v) { this.gst = v; }
    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal v) { this.totalFare = v; }
    public BigDecimal getRefundTotal() { return refundTotal; }
    public void setRefundTotal(BigDecimal v) { this.refundTotal = v; }
    public String getContactMobile() { return contactMobile; }
    public void setContactMobile(String contactMobile) { this.contactMobile = contactMobile; }
    public boolean isOnlyIfConfirmed() { return onlyIfConfirmed; }
    public void setOnlyIfConfirmed(boolean onlyIfConfirmed) { this.onlyIfConfirmed = onlyIfConfirmed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaymentDueAt() { return paymentDueAt; }
    public void setPaymentDueAt(LocalDateTime paymentDueAt) { this.paymentDueAt = paymentDueAt; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public List<Passenger> getPassengers() { return passengers; }
    public List<Payment> getPayments() { return payments; }
}
