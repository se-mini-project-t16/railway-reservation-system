package com.railway.reservation.service;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("An account with this email already exists.");
    }
}
