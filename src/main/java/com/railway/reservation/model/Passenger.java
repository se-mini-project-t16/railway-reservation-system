package com.railway.reservation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A traveller on a PNR. The booking_* columns freeze the status issued at
 * booking time (as printed on the ticket); the current_* columns move as
 * waiting lists clear or the traveller cancels.
 */
@Entity
@Table(name = "passengers")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(nullable = false)
    private int serial;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "berth_preference", nullable = false, length = 12)
    private BerthPreference berthPreference = BerthPreference.NONE;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private FoodChoice food;

    // Status issued at booking
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 4)
    private SeatStatus bookingStatus;
    @Column(name = "booking_coach", length = 6)
    private String bookingCoach;
    @Column(name = "booking_berth")
    private Integer bookingBerth;
    @Column(name = "booking_number")
    private Integer bookingNumber;

    // Current status
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 4)
    private SeatStatus currentStatus;
    /** Zero-based berth position within the class (coach * perCoach + berth - 1); null when not confirmed. */
    @Column(name = "berth_index")
    private Integer berthIndex;
    @Column(length = 6)
    private String coach;
    @Column(name = "berth_no")
    private Integer berthNo;
    @Enumerated(EnumType.STRING)
    @Column(name = "berth_type", length = 12)
    private BerthType berthType;

    /** Global order in which RAC/WL travellers clear: lower clears first. */
    @Column(name = "queue_seq", nullable = false)
    private long queueSeq;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare = BigDecimal.ZERO;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected Passenger() {}

    public Passenger(int serial, String name, int age, Gender gender, BerthPreference preference, FoodChoice food) {
        this.serial = serial;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.berthPreference = preference == null ? BerthPreference.NONE : preference;
        this.food = food;
    }

    public void confirm(int index, String coach, int berthNo, BerthType type) {
        this.currentStatus = SeatStatus.CNF;
        this.berthIndex = index;
        this.coach = coach;
        this.berthNo = berthNo;
        this.berthType = type;
    }

    public void queue(SeatStatus status) {
        this.currentStatus = status;
        this.berthIndex = null;
        this.coach = null;
        this.berthNo = null;
        this.berthType = null;
    }

    public void freezeBookingStatus(Integer number) {
        this.bookingStatus = currentStatus;
        this.bookingCoach = coach;
        this.bookingBerth = berthNo;
        this.bookingNumber = number;
    }

    /** Senior travellers and women aged 45 or more are offered lower berths first. */
    public boolean prefersLowerByRule() {
        return age >= 60 || (gender == Gender.FEMALE && age >= 45);
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    void setBooking(Booking booking) { this.booking = booking; }
    public int getSerial() { return serial; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public Gender getGender() { return gender; }
    public BerthPreference getBerthPreference() { return berthPreference; }
    public FoodChoice getFood() { return food; }
    public SeatStatus getBookingStatus() { return bookingStatus; }
    public String getBookingCoach() { return bookingCoach; }
    public Integer getBookingBerth() { return bookingBerth; }
    public Integer getBookingNumber() { return bookingNumber; }
    public SeatStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(SeatStatus currentStatus) { this.currentStatus = currentStatus; }
    public Integer getBerthIndex() { return berthIndex; }
    public String getCoach() { return coach; }
    public Integer getBerthNo() { return berthNo; }
    public BerthType getBerthType() { return berthType; }
    public long getQueueSeq() { return queueSeq; }
    public void setQueueSeq(long queueSeq) { this.queueSeq = queueSeq; }
    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
