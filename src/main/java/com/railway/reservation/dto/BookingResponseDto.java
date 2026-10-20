package com.railway.reservation.dto;

import com.railway.reservation.model.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class BookingResponseDto {

    private String pnr;
    private BookingStatus status;
    private Long bookingId;
    private Long userId;
    private String passengerAccountName;

    // Train info
    private Long trainId;
    private String trainNo;
    private String trainName;
    private String sourceStation;
    private String destinationStation;
    private LocalTime departureTime;
    private LocalTime arrivalTime;

    // Class info
    private String className;
    private BigDecimal farePerSeat;
    private BigDecimal totalFare;

    // Journey info
    private LocalDate journeyDate;
    private LocalDateTime bookingTime;
    private List<PassengerDto> passengers;

    public BookingResponseDto() {}

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPassengerAccountName() { return passengerAccountName; }
    public void setPassengerAccountName(String passengerAccountName) { this.passengerAccountName = passengerAccountName; }

    public Long getTrainId() { return trainId; }
    public void setTrainId(Long trainId) { this.trainId = trainId; }

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }

    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }

    public String getSourceStation() { return sourceStation; }
    public void setSourceStation(String sourceStation) { this.sourceStation = sourceStation; }

    public String getDestinationStation() { return destinationStation; }
    public void setDestinationStation(String destinationStation) { this.destinationStation = destinationStation; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public BigDecimal getFarePerSeat() { return farePerSeat; }
    public void setFarePerSeat(BigDecimal farePerSeat) { this.farePerSeat = farePerSeat; }

    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }

    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }

    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }

    public List<PassengerDto> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerDto> passengers) { this.passengers = passengers; }
}

