package com.railway.reservation.service.notify;

import com.railway.reservation.service.mail.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Password reset codes and the "your password was changed" notice. Sent in the background after the
 * change is committed, so the reset request answers the same way, in the same time, whether or not
 * the email belongs to an account.
 */
@Component
public class AccountEmails {

    private static final Logger log = LoggerFactory.getLogger(AccountEmails.class);

    private final EmailSender emailSender;

    public AccountEmails(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResetRequested(AccountEvents.PasswordResetRequested e) {
        send(e.email(), resetSubject(e.code()), resetText(e), resetHtml(e));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(AccountEvents.PasswordChanged e) {
        send(e.email(), "Your Railway Reservation password was changed", changedText(e), changedHtml(e));
    }

    private void send(String to, String subject, String text, String html) {
        try {
            emailSender.send(to, subject, text, html);
        } catch (RuntimeException ex) {
            log.warn("Account email not sent: {}", ex.getMessage());
        }
    }

    static String resetSubject(String code) {
        return code + " is your Railway Reservation password reset code";
    }

    static String resetText(AccountEvents.PasswordResetRequested e) {
        return "Hello " + e.name() + ",\n\nUse this code to set a new password:\n\n    " + e.code() + "\n\n"
                + "It expires in " + e.minutes() + " minutes. Never share it with anyone.\n\n"
                + "If you did not ask to reset your password, ignore this email. Your password stays as it is.\n";
    }

    static String resetHtml(AccountEvents.PasswordResetRequested e) {
        return EmailLayout.page("Reset your password",
                EmailLayout.para("Hello " + e.name() + ", use this code to set a new password:")
                        + "<p style=\"margin:0 0 20px;font-size:32px;font-weight:bold;letter-spacing:.3em;font-family:'Courier New',monospace\">"
                        + EmailLayout.esc(e.code()) + "</p>"
                        + EmailLayout.para("It expires in " + e.minutes() + " minutes. Never share it with anyone.")
                        + EmailLayout.small("If you did not ask to reset your password, ignore this email. Your password stays as it is."));
    }

    static String changedText(AccountEvents.PasswordChanged e) {
        return "Hello " + e.name() + ",\n\nThe password for your Railway Reservation account was just changed.\n\n"
                + "If this was you, there is nothing to do. If it was not, reset your password from the sign-in page "
                + "straight away using \"Forgot password\".\n";
    }

    static String changedHtml(AccountEvents.PasswordChanged e) {
        return EmailLayout.page("Your password was changed",
                EmailLayout.para("Hello " + e.name() + ", the password for your account was just changed.")
                        + EmailLayout.small("If this was you, there is nothing to do. If it was not, reset your password from "
                        + "the sign-in page straight away using \"Forgot password\"."));
    }
}
