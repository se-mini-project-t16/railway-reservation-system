package com.railway.reservation.support;

import com.railway.reservation.service.mail.EmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps every email the app sends during a test, so tests can read the verification code. */
@TestConfiguration
public class MailCatcherConfig {

    public record Mail(String to, String subject, String text, String html, List<EmailSender.Attachment> attachments) {}

    public static class MailCatcher implements EmailSender {
        private final List<Mail> sent = new CopyOnWriteArrayList<>();
        private volatile boolean failing;

        @Override
        public void send(EmailSender.Email email) {
            if (failing) throw new com.railway.reservation.service.mail.EmailNotSentException("test", null);
            sent.add(new Mail(email.to(), email.subject(), email.text(), email.html(), email.attachments()));
        }

        /** Waits up to five seconds for an email (sent in the background) whose subject contains the text. */
        public Mail await(String to, String subjectContains) {
            long until = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < until) {
                for (Mail m : sent) if (m.to().equalsIgnoreCase(to) && m.subject().contains(subjectContains)) return m;
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            throw new AssertionError("No email to " + to + " with subject containing \"" + subjectContains + "\"; sent: "
                    + sent.stream().map(Mail::subject).toList());
        }

        /** True when no email whose subject contains the text arrives within the given time. */
        public boolean noneWithin(String subjectContains, long millis) {
            long until = System.currentTimeMillis() + millis;
            while (System.currentTimeMillis() < until) {
                if (sent.stream().anyMatch(m -> m.subject().contains(subjectContains))) return false;
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            return true;
        }

        public List<Mail> sent() { return sent; }
        public void clear() { sent.clear(); failing = false; }
        public void failNext(boolean failing) { this.failing = failing; }

        /** The code in the newest email to this address. */
        public String codeFor(String email) {
            for (int i = sent.size() - 1; i >= 0; i--) {
                Mail m = sent.get(i);
                if (!m.to().equalsIgnoreCase(email)) continue;
                Matcher match = Pattern.compile("\\b(\\d{6})\\b").matcher(m.subject());
                if (match.find()) return match.group(1);
            }
            throw new AssertionError("No code was emailed to " + email);
        }
    }

    @Bean
    @Primary
    public MailCatcher mailCatcher() {
        return new MailCatcher();
    }
}
