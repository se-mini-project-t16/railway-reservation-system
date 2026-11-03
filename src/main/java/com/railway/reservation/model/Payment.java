package com.railway.reservation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One payment attempt against a booking. Only masked instrument details are stored. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 24)
    private String txnRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PaymentMethod method;

    @Column(nullable = false, length = 60)
    private String instrument;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 120)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "refunded_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    protected Payment() {}

    public Payment(String txnRef, PaymentMethod method, String instrument, BigDecimal amount,
                   PaymentStatus status, String failureReason, LocalDateTime createdAt) {
        this.txnRef = txnRef;
        this.method = method;
        this.instrument = instrument;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    void setBooking(Booking booking) { this.booking = booking; }
    public String getTxnRef() { return txnRef; }
    public PaymentMethod getMethod() { return method; }
    public String getInstrument() { return instrument; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
}
