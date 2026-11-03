package com.railway.reservation.dto;

public class SeatCellDto {

    private String seatNumber;
    private int seatIndex;
    private int row;
    private int col;
    private String berthType;
    private String status;
    private String passengerName;
    private Integer passengerAge;
    private String passengerGender;
    private String pnr;

    public SeatCellDto() {}

    public SeatCellDto(String seatNumber, int seatIndex, int row, int col, String berthType, String status) {
        this.seatNumber = seatNumber;
        this.seatIndex = seatIndex;
        this.row = row;
        this.col = col;
        this.berthType = berthType;
        this.status = status;
    }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public int getSeatIndex() { return seatIndex; }
    public void setSeatIndex(int seatIndex) { this.seatIndex = seatIndex; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }

    public String getBerthType() { return berthType; }
    public void setBerthType(String berthType) { this.berthType = berthType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public Integer getPassengerAge() { return passengerAge; }
    public void setPassengerAge(Integer passengerAge) { this.passengerAge = passengerAge; }

    public String getPassengerGender() { return passengerGender; }
    public void setPassengerGender(String passengerGender) { this.passengerGender = passengerGender; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
}

