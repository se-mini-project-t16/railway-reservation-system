package com.railway.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Email verification at sign-up: a code is sent, then confirmed to create the account. */
public final class SignUpDtos {

    private SignUpDtos() {}

    /** A code is on its way. {@code resendInSeconds} is how long before another may be requested. */
    public record SignUpStarted(String email, long expiresInSeconds, long resendInSeconds) {}

    public record VerifyEmailRequest(
            @NotBlank(message = "Enter your email.") @Email @Size(max = 150) String email,
            @NotBlank(message = "Enter the 6-digit code.")
            @Pattern(regexp = "^\\d{6}$", message = "The code is 6 digits.") String code) {}

    public record ResendCodeRequest(@NotBlank(message = "Enter your email.") @Email @Size(max = 150) String email) {}
}
