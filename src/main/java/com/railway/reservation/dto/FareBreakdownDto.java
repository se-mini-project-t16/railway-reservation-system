package com.railway.reservation.dto;

import java.math.BigDecimal;

public class FareBreakdownDto {

    private int passengerCount;
    private BigDecimal farePerSeat;
    private BigDecimal baseFare;
    private BigDecimal superfastCharge;
    private BigDecimal reservationFee;
    private BigDecimal gst;
    private BigDecimal seniorDiscount;
    private BigDecimal totalFare;
    private int eligibleSeniorCount;
    private boolean isAc;

    public FareBreakdownDto() {}

    public FareBreakdownDto(
            int passengerCount,
            BigDecimal farePerSeat,
            BigDecimal baseFare,
            BigDecimal superfastCharge,
            BigDecimal reservationFee,
            BigDecimal gst,
            BigDecimal seniorDiscount,
            BigDecimal totalFare,
            int eligibleSeniorCount,
            boolean isAc) {
        this.passengerCount = passengerCount;
        this.farePerSeat = farePerSeat;
        this.baseFare = baseFare;
        this.superfastCharge = superfastCharge;
        this.reservationFee = reservationFee;
        this.gst = gst;
        this.seniorDiscount = seniorDiscount;
        this.totalFare = totalFare;
        this.eligibleSeniorCount = eligibleSeniorCount;
        this.isAc = isAc;
    }

    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }

    public BigDecimal getFarePerSeat() { return farePerSeat; }
    public void setFarePerSeat(BigDecimal farePerSeat) { this.farePerSeat = farePerSeat; }

    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

    public BigDecimal getSuperfastCharge() { return superfastCharge; }
    public void setSuperfastCharge(BigDecimal superfastCharge) { this.superfastCharge = superfastCharge; }

    public BigDecimal getReservationFee() { return reservationFee; }
    public void setReservationFee(BigDecimal reservationFee) { this.reservationFee = reservationFee; }

    public BigDecimal getGst() { return gst; }
    public void setGst(BigDecimal gst) { this.gst = gst; }

    public BigDecimal getSeniorDiscount() { return seniorDiscount; }
    public void setSeniorDiscount(BigDecimal seniorDiscount) { this.seniorDiscount = seniorDiscount; }

    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }

    public int getEligibleSeniorCount() { return eligibleSeniorCount; }
    public void setEligibleSeniorCount(int eligibleSeniorCount) { this.eligibleSeniorCount = eligibleSeniorCount; }

    public boolean isAc() { return isAc; }
    public void setAc(boolean ac) { isAc = ac; }
}

