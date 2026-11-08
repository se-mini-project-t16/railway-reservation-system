package com.railway.reservation.service.mail;

import java.util.List;

/** Sends one email. Implementations throw {@link EmailNotSentException} when the message cannot be handed over. */
public interface EmailSender {

    record Attachment(String filename, String contentType, byte[] bytes) {}

    record Email(String to, String subject, String text, String html, List<Attachment> attachments) {
        public Email {
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
        }
    }

    void send(Email email);

    default void send(String to, String subject, String text, String html) {
        send(new Email(to, subject, text, html, List.of()));
    }
}
