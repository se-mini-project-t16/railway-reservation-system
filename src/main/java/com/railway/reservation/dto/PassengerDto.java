package com.railway.reservation.dto;

import com.railway.reservation.validation.SafeText;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PassengerDto {

    @NotBlank(message = "Passenger name is required and cannot be blank")
    @SafeText
    private String name;

    @Min(value = 1, message = "Passenger age must be at least 1")
    @Max(value = 120, message = "Passenger age must be at most 120")
    private int age;

    @NotBlank(message = "Passenger gender is required")
    @Pattern(regexp = "^(?i)(Male|Female|Other|M|F|O)$", message = "Gender must be Male, Female, or Other")
    private String gender;

    private String seatNo;

    @SafeText
    private String berthPreference = "NO_PREFERENCE";

    @SafeText
    private String mealPreference = "NO_FOOD";

    private Boolean seniorConcession = false;

    private Long id;

    private String status = "CONFIRMED";

    public PassengerDto() {}

    public PassengerDto(String name, int age, String gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    public PassengerDto(String name, int age, String gender, String seatNo) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.seatNo = seatNo;
    }

    public PassengerDto(String name, int age, String gender, String seatNo, String berthPreference, String mealPreference, Boolean seniorConcession) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.seatNo = seatNo;
        this.berthPreference = berthPreference != null ? berthPreference : "NO_PREFERENCE";
        this.mealPreference = mealPreference != null ? mealPreference : "NO_FOOD";
        this.seniorConcession = Boolean.TRUE.equals(seniorConcession);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSeatNo() { return seatNo; }
    public void setSeatNo(String seatNo) { this.seatNo = seatNo; }

    public String getBerthPreference() { return berthPreference; }
    public void setBerthPreference(String berthPreference) { this.berthPreference = berthPreference; }

    public String getMealPreference() { return mealPreference; }
    public void setMealPreference(String mealPreference) { this.mealPreference = mealPreference; }

    public Boolean getSeniorConcession() { return seniorConcession; }
    public void setSeniorConcession(Boolean seniorConcession) { this.seniorConcession = seniorConcession; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatus() { return status != null ? status : "CONFIRMED"; }
    public void setStatus(String status) { this.status = status; }
}

