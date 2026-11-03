package com.railway.reservation.dto;

public class ReservationChartEntryDto {

    private String pnr;
    private String seatNumber;
    private String className;
    private String passengerName;
    private Integer age;
    private String gender;
    private String status;
    private String boardingStation;
    private String destinationStation;

    public ReservationChartEntryDto() {}

    public ReservationChartEntryDto(String pnr, String seatNumber, String className, String passengerName,
                                    Integer age, String gender, String status, String boardingStation,
                                    String destinationStation) {
        this.pnr = pnr;
        this.seatNumber = seatNumber;
        this.className = className;
        this.passengerName = passengerName;
        this.age = age;
        this.gender = gender;
        this.status = status;
        this.boardingStation = boardingStation;
        this.destinationStation = destinationStation;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBoardingStation() { return boardingStation; }
    public void setBoardingStation(String boardingStation) { this.boardingStation = boardingStation; }

    public String getDestinationStation() { return destinationStation; }
    public void setDestinationStation(String destinationStation) { this.destinationStation = destinationStation; }
}

