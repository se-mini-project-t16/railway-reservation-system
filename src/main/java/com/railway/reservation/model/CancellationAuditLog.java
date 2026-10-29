package com.railway.reservation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit trail for ticket cancellations (RRS-F-012). Ids are stored as plain
 * columns so the record outlives any later change to the booking or user.
 */
@Entity
@Table(name = "cancellation_audit_log")
public class CancellationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(nullable = false, length = 20)
    private String pnr;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;

    public CancellationAuditLog() {}

    public CancellationAuditLog(Long bookingId, String pnr, Long userId, BigDecimal refundAmount, LocalDateTime cancelledAt) {
        this.bookingId = bookingId;
        this.pnr = pnr;
        this.userId = userId;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
