package com.railway.reservation.dto;

import java.math.BigDecimal;
import java.util.List;

public class PartialRefundPreviewDto {

    private String pnr;
    private List<Long> selectedPassengerIds;
    private List<String> selectedPassengerNames;
    private BigDecimal grossFare;
    private long hoursBeforeDeparture;
    private BigDecimal refundPercent;
    private BigDecimal deductionPercent;
    private BigDecimal refundAmount;
    private BigDecimal deductionAmount;

    public PartialRefundPreviewDto() {}

    public PartialRefundPreviewDto(
            String pnr,
            List<Long> selectedPassengerIds,
            List<String> selectedPassengerNames,
            BigDecimal grossFare,
            long hoursBeforeDeparture,
            BigDecimal refundPercent,
            BigDecimal deductionPercent,
            BigDecimal refundAmount,
            BigDecimal deductionAmount) {
        this.pnr = pnr;
        this.selectedPassengerIds = selectedPassengerIds;
        this.selectedPassengerNames = selectedPassengerNames;
        this.grossFare = grossFare;
        this.hoursBeforeDeparture = hoursBeforeDeparture;
        this.refundPercent = refundPercent;
        this.deductionPercent = deductionPercent;
        this.refundAmount = refundAmount;
        this.deductionAmount = deductionAmount;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public List<Long> getSelectedPassengerIds() { return selectedPassengerIds; }
    public void setSelectedPassengerIds(List<Long> selectedPassengerIds) { this.selectedPassengerIds = selectedPassengerIds; }

    public List<String> getSelectedPassengerNames() { return selectedPassengerNames; }
    public void setSelectedPassengerNames(List<String> selectedPassengerNames) { this.selectedPassengerNames = selectedPassengerNames; }

    public BigDecimal getGrossFare() { return grossFare; }
    public void setGrossFare(BigDecimal grossFare) { this.grossFare = grossFare; }

    public long getHoursBeforeDeparture() { return hoursBeforeDeparture; }
    public void setHoursBeforeDeparture(long hoursBeforeDeparture) { this.hoursBeforeDeparture = hoursBeforeDeparture; }

    public BigDecimal getRefundPercent() { return refundPercent; }
    public void setRefundPercent(BigDecimal refundPercent) { this.refundPercent = refundPercent; }

    public BigDecimal getDeductionPercent() { return deductionPercent; }
    public void setDeductionPercent(BigDecimal deductionPercent) { this.deductionPercent = deductionPercent; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public void setDeductionAmount(BigDecimal deductionAmount) { this.deductionAmount = deductionAmount; }
}

