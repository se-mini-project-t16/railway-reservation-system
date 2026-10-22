package com.railway.reservation.exception;

public class TrainDeletionException extends RuntimeException {

    public TrainDeletionException(String message) {
        super(message);
    }

    public TrainDeletionException() {
        super("Cannot delete train because bookings exist for it.");
    }
}
