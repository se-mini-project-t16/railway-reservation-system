package com.railway.reservation.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "passengers")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passenger_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonBackReference
    private Booking booking;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(name = "seat_no", length = 20)
    private String seatNo;

    @Column(name = "berth_preference", length = 30)
    private String berthPreference = "NO_PREFERENCE";

    @Column(name = "meal_preference", length = 20)
    private String mealPreference = "NO_FOOD";

    @Column(name = "senior_concession")
    private Boolean seniorConcession = false;

    @Column(name = "status", length = 20)
    private String status = "CONFIRMED";

    public Passenger() {}

    public Passenger(String name, int age, String gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    public Passenger(String name, int age, String gender, String seatNo) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.seatNo = seatNo;
    }

    public Passenger(String name, int age, String gender, String seatNo, String berthPreference, String mealPreference, Boolean seniorConcession) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.seatNo = seatNo;
        this.berthPreference = berthPreference != null ? berthPreference : "NO_PREFERENCE";
        this.mealPreference = mealPreference != null ? mealPreference : "NO_FOOD";
        this.seniorConcession = Boolean.TRUE.equals(seniorConcession);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

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

    public String getStatus() { return status != null ? status : "CONFIRMED"; }
    public void setStatus(String status) { this.status = status; }
}

