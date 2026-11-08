package com.railway.reservation.service.google;

/** The parts of a verified Google ID token this system uses. {@code subject} is Google's stable account id. */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {}
