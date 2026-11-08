package com.railway.reservation.service.mail;

public class EmailNotSentException extends RuntimeException {

    public EmailNotSentException(String message, Throwable cause) {
        super(message, cause);
    }
}
