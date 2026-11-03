package com.railway.reservation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable record of every cancellation; ids are plain columns so it outlives later edits. */
@Entity
@Table(name = "cancellation_audit_log")
public class CancellationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String pnr;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = 20)
    private String reason;

    @Column(name = "passengers_cancelled", nullable = false)
    private int passengersCancelled;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;

    protected CancellationAuditLog() {}

    public CancellationAuditLog(String pnr, Long actorUserId, String reason, int passengersCancelled,
                                BigDecimal refundAmount, LocalDateTime cancelledAt) {
        this.pnr = pnr;
        this.actorUserId = actorUserId;
        this.reason = reason;
        this.passengersCancelled = passengersCancelled;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
    }

    public Long getId() { return id; }
    public String getPnr() { return pnr; }
    public Long getActorUserId() { return actorUserId; }
    public String getReason() { return reason; }
    public int getPassengersCancelled() { return passengersCancelled; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
