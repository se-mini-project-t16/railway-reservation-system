package com.railway.reservation.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CoachSeatMatrixDto {

    private Long trainId;
    private String trainNo;
    private String trainName;
    private String className;
    private String coachCode;
    private LocalDate journeyDate;
    private int totalSeats;
    private int bookedSeatsCount;
    private int availableSeatsCount;
    private double occupancyRate;
    private int rows;
    private int columns;
    private List<SeatCellDto> seats = new ArrayList<>();

    public CoachSeatMatrixDto() {}

    public Long getTrainId() { return trainId; }
    public void setTrainId(Long trainId) { this.trainId = trainId; }

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }

    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getCoachCode() { return coachCode; }
    public void setCoachCode(String coachCode) { this.coachCode = coachCode; }

    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getBookedSeatsCount() { return bookedSeatsCount; }
    public void setBookedSeatsCount(int bookedSeatsCount) { this.bookedSeatsCount = bookedSeatsCount; }

    public int getAvailableSeatsCount() { return availableSeatsCount; }
    public void setAvailableSeatsCount(int availableSeatsCount) { this.availableSeatsCount = availableSeatsCount; }

    public double getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(double occupancyRate) { this.occupancyRate = occupancyRate; }

    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }

    public int getColumns() { return columns; }
    public void setColumns(int columns) { this.columns = columns; }

    public List<SeatCellDto> getSeats() { return seats; }
    public void setSeats(List<SeatCellDto> seats) { this.seats = seats; }
}

