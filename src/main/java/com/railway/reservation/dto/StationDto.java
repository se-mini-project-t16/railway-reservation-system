package com.railway.reservation.dto;

/**
 * Data transfer object representing a railway station in the master catalog (RRS-F-004-EXT).
 */
public class StationDto {

    private String code;
    private String name;
    private String city;
    private String state;
    private String zone;

    public StationDto() {}

    public StationDto(String code, String name, String city, String state, String zone) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.state = state;
        this.zone = zone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
