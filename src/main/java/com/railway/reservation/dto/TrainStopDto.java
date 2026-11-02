package com.railway.reservation.dto;

import java.time.LocalTime;

/**
 * Data transfer object for intermediate halts and timetable stops (RRS-F-004-EXT).
 */
public class TrainStopDto {

    private int stopOrder;
    private String stationCode;
    private String stationName;
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private int haltMinutes;
    private int platformNo;
    private int distanceKm;

    public TrainStopDto() {}

    public TrainStopDto(int stopOrder, String stationCode, String stationName,
                        LocalTime arrivalTime, LocalTime departureTime,
                        int haltMinutes, int platformNo, int distanceKm) {
        this.stopOrder = stopOrder;
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.haltMinutes = haltMinutes;
        this.platformNo = platformNo;
        this.distanceKm = distanceKm;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(int stopOrder) {
        this.stopOrder = stopOrder;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public int getHaltMinutes() {
        return haltMinutes;
    }

    public void setHaltMinutes(int haltMinutes) {
        this.haltMinutes = haltMinutes;
    }

    public int getPlatformNo() {
        return platformNo;
    }

    public void setPlatformNo(int platformNo) {
        this.platformNo = platformNo;
    }

    public int getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(int distanceKm) {
        this.distanceKm = distanceKm;
    }
}
