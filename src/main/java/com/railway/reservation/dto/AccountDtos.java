package com.railway.reservation.dto;

import com.railway.reservation.model.BerthPreference;
import com.railway.reservation.model.FoodChoice;
import com.railway.reservation.model.Gender;
import com.railway.reservation.validation.SafeText;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public final class AccountDtos {

    private AccountDtos() {}

    public record Profile(Long id, String name, String email, String phone, String role, LocalDateTime memberSince) {}

    public record ProfileUpdate(
            @NotBlank(message = "Enter your name.") @Size(max = 100) @SafeText String name,
            @NotBlank(message = "Enter your mobile number.")
            @Pattern(regexp = BookingDtos.MOBILE_PATTERN, message = "Enter a 10-digit Indian mobile number.") String phone) {}

    public record PasswordChange(
            @NotBlank(message = "Enter your current password.") String currentPassword,
            @NotBlank @Size(min = 8, max = 64, message = "Use at least 8 characters.") String newPassword) {}

    public record TravellerInput(
            @NotBlank(message = "Enter the traveller's name.")
            @Pattern(regexp = BookingDtos.NAME_PATTERN, message = "Use 2 to 40 letters; spaces, dots and apostrophes are allowed.")
            @SafeText String name,
            @NotNull @Min(value = 5, message = "Travellers must be 5 or older.") @Max(120) Integer age,
            @NotNull(message = "Choose a gender.") Gender gender,
            BerthPreference berthPreference,
            FoodChoice food) {}

    public record Traveller(Long id, String name, int age, String gender, String berthPreference, String food) {}
}
