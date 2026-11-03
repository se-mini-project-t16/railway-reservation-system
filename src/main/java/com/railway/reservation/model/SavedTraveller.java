package com.railway.reservation.model;

import jakarta.persistence.*;

/** A traveller the account holder books for often, offered when filling a booking. */
@Entity
@Table(name = "saved_travellers")
public class SavedTraveller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "berth_preference", nullable = false, length = 12)
    private BerthPreference berthPreference = BerthPreference.NONE;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private FoodChoice food;

    protected SavedTraveller() {}

    public SavedTraveller(User user, String name, int age, Gender gender, BerthPreference pref, FoodChoice food) {
        this.user = user;
        update(name, age, gender, pref, food);
    }

    public void update(String name, int age, Gender gender, BerthPreference pref, FoodChoice food) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.berthPreference = pref == null ? BerthPreference.NONE : pref;
        this.food = food;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public Gender getGender() { return gender; }
    public BerthPreference getBerthPreference() { return berthPreference; }
    public FoodChoice getFood() { return food; }
}
