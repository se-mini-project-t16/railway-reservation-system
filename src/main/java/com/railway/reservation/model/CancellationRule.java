package com.railway.reservation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cancellation_rules")
public class CancellationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @Column(name = "hours_before", nullable = false)
    private int hoursBefore;

    @Column(name = "refund_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercent;

    public CancellationRule() {}

    public CancellationRule(int hoursBefore, BigDecimal refundPercent) {
        this.hoursBefore = hoursBefore;
        this.refundPercent = refundPercent;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getHoursBefore() { return hoursBefore; }
    public void setHoursBefore(int hoursBefore) { this.hoursBefore = hoursBefore; }

    public BigDecimal getRefundPercent() { return refundPercent; }
    public void setRefundPercent(BigDecimal refundPercent) { this.refundPercent = refundPercent; }
}

