package com.railway.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BookingReportRowDto {

    private String pnr;
    private String trainNo;
    private LocalDate journeyDate;
    private String status;
    private BigDecimal fare;
    private List<PassengerSeat> passengers;

    public BookingReportRowDto() {}

    public BookingReportRowDto(String pnr, String trainNo, LocalDate journeyDate, String status,
                               BigDecimal fare, List<PassengerSeat> passengers) {
        this.pnr = pnr;
        this.trainNo = trainNo;
        this.journeyDate = journeyDate;
        this.status = status;
        this.fare = fare;
        this.passengers = passengers;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }

    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }

    public List<PassengerSeat> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerSeat> passengers) { this.passengers = passengers; }

    public record PassengerSeat(String name, String seatNo) {}
}
