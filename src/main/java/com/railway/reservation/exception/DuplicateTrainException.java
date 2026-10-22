package com.railway.reservation.exception;

public class DuplicateTrainException extends RuntimeException {

    public DuplicateTrainException(String message) {
        super(message);
    }

    public DuplicateTrainException() {
        super("A train with this train number already exists.");
    }
}
