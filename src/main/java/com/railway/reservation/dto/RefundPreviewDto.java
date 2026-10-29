package com.railway.reservation.dto;

import java.math.BigDecimal;

public class RefundPreviewDto {

    private String pnr;
    private BigDecimal totalFare;
    private long hoursBeforeDeparture;
    private BigDecimal refundPercent;
    private BigDecimal refundAmount;

    public RefundPreviewDto() {}

    public RefundPreviewDto(String pnr, BigDecimal totalFare, long hoursBeforeDeparture,
                            BigDecimal refundPercent, BigDecimal refundAmount) {
        this.pnr = pnr;
        this.totalFare = totalFare;
        this.hoursBeforeDeparture = hoursBeforeDeparture;
        this.refundPercent = refundPercent;
        this.refundAmount = refundAmount;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }

    public long getHoursBeforeDeparture() { return hoursBeforeDeparture; }
    public void setHoursBeforeDeparture(long hoursBeforeDeparture) { this.hoursBeforeDeparture = hoursBeforeDeparture; }

    public BigDecimal getRefundPercent() { return refundPercent; }
    public void setRefundPercent(BigDecimal refundPercent) { this.refundPercent = refundPercent; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}
