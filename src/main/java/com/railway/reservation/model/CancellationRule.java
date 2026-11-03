package com.railway.reservation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Refund rule for confirmed tickets, keyed by how long before departure the
 * ticket is cancelled. The deduction is the larger of the percentage of the
 * fare and the class's flat cancellation charge.
 */
@Entity
@Table(name = "cancellation_rules")
public class CancellationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Applies when cancelled at least this many hours before departure. */
    @Column(name = "min_hours_before", nullable = false)
    private int minHoursBefore;

    @Column(name = "deduction_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal deductionPercent;

    @Column(name = "flat_minimum", nullable = false)
    private boolean flatMinimum;

    @Column(nullable = false, length = 80)
    private String description;

    protected CancellationRule() {}

    public CancellationRule(int minHoursBefore, BigDecimal deductionPercent, boolean flatMinimum, String description) {
        this.minHoursBefore = minHoursBefore;
        this.deductionPercent = deductionPercent;
        this.flatMinimum = flatMinimum;
        this.description = description;
    }

    public Long getId() { return id; }
    public int getMinHoursBefore() { return minHoursBefore; }
    public BigDecimal getDeductionPercent() { return deductionPercent; }
    public boolean isFlatMinimum() { return flatMinimum; }
    public String getDescription() { return description; }

    public void update(BigDecimal deductionPercent, boolean flatMinimum, String description) {
        this.deductionPercent = deductionPercent;
        this.flatMinimum = flatMinimum;
        this.description = description;
    }
}
