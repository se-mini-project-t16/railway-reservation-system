package com.railway.reservation.dto;

/** The account created once the email code is confirmed, with a session token so the passenger is signed in. */
public record RegisterResponseDto(Long userId, String token) {
}
