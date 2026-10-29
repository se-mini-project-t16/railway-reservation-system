package com.railway.reservation.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    @JsonBackReference
    private Booking booking;

    @Column(name = "gateway_ref", length = 100)
    private String gatewayRef;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_ref", length = 100)
    private String refundRef;

    public Payment() {}

    public Payment(Booking booking, BigDecimal amount, String gatewayRef, String status) {
        this.booking = booking;
        this.amount = amount;
        this.gatewayRef = gatewayRef;
        this.status = status;
        this.paidAt = ("CONFIRMED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status))
                ? LocalDateTime.now()
                : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public String getGatewayRef() { return gatewayRef; }
    public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getRefundRef() { return refundRef; }
    public void setRefundRef(String refundRef) { this.refundRef = refundRef; }
}

