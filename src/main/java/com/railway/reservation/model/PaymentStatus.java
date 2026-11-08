package com.railway.reservation.model;

/** SUCCESS: captured and kept. FAILED: declined or abandoned at the provider. REFUNDED: captured, then returned in full. */
public enum PaymentStatus { SUCCESS, FAILED, REFUNDED }
