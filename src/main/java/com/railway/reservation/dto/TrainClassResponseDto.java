package com.railway.reservation.dto;

import java.math.BigDecimal;

public class TrainClassResponseDto {

    private Long id;
    private String className;
    private int totalSeats;
    private BigDecimal fare;

    public TrainClassResponseDto() {}

    public TrainClassResponseDto(Long id, String className, int totalSeats, BigDecimal fare) {
        this.id = id;
        this.className = className;
        this.totalSeats = totalSeats;
        this.fare = fare;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }
}
