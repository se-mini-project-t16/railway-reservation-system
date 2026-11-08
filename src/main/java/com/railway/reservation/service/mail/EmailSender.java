package com.railway.reservation.service.mail;

/** Sends one email. Implementations throw {@link EmailNotSentException} when the message cannot be handed over. */
public interface EmailSender {

    void send(String to, String subject, String text, String html);
}
