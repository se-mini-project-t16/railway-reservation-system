package com.railway.reservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class TrainClassDto {

    @NotBlank(message = "Class name must not be blank")
    @Size(max = 50, message = "Class name cannot exceed 50 characters")
    private String className;

    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeats;

    @NotNull(message = "Fare must not be null")
    @DecimalMin(value = "0.01", message = "Fare must be at least 0.01")
    private BigDecimal fare;

    public TrainClassDto() {}

    public TrainClassDto(String className, int totalSeats, BigDecimal fare) {
        this.className = className;
        this.totalSeats = totalSeats;
        this.fare = fare;
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
