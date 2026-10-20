package com.railway.reservation.dto;

import java.math.BigDecimal;

public class SeatAvailabilityDto {

    private Long classId;
    private String className;
    private int totalSeats;
    private int availableSeats;
    private BigDecimal fare;

    public SeatAvailabilityDto() {}

    public SeatAvailabilityDto(Long classId, String className, int totalSeats, int availableSeats, BigDecimal fare) {
        this.classId = classId;
        this.className = className;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.fare = fare;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }
}
