package com.railway.reservation.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Sends mail through the SMTP server in {@code spring.mail.*} (Gmail by default). When no mail
 * account is configured, as on a fresh checkout, the message is written to the server log
 * instead so sign-up can still be completed locally; a warning says so at start-up.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public SmtpEmailSender(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${spring.mail.username:}") String username,
                           @Value("${app.mail.from:}") String fromAddress,
                           @Value("${app.mail.from-name:Railway Reservation}") String fromName) {
        boolean configured = username != null && !username.isBlank();
        this.mailSender = configured ? mailSender.getIfAvailable() : null;
        // The account that signs in to SMTP, unless another verified "Send mail as" address is given
        this.from = !configured ? null : fromAddress != null && !fromAddress.isBlank() ? fromAddress.trim() : username.trim();
        this.fromName = fromName;
        if (this.mailSender == null) {
            log.warn("Email is not configured (spring.mail.username is empty): verification codes will be "
                    + "written to this log instead of being emailed");
        }
    }

    public boolean configured() {
        return mailSender != null;
    }

    @Override
    public void send(Email email) {
        String logSubject = email.subject().replaceAll("\\b\\d{6}\\b", "******");
        if (mailSender == null) {
            log.warn("Email not configured. Message for {}: {}{}\n{}", email.to(), email.subject(),
                    email.attachments().isEmpty() ? "" : " (" + email.attachments().size() + " attachment)", email.text());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, fromName);
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.text(), email.html());
            for (Attachment a : email.attachments()) {
                helper.addAttachment(a.filename(), new ByteArrayResource(a.bytes()), a.contentType());
            }
            mailSender.send(message);
            log.info("Email sent to {}: {}", email.to(), logSubject);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.error("Email to {} failed: {}", email.to(), e.getMessage());
            throw new EmailNotSentException("Email could not be sent", e);
        }
    }
}
