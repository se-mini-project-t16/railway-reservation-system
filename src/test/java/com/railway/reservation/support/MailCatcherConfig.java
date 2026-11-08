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

    public record Mail(String to, String subject, String text, String html) {}

    public static class MailCatcher implements EmailSender {
        private final List<Mail> sent = new CopyOnWriteArrayList<>();
        private volatile boolean failing;

        @Override
        public void send(String to, String subject, String text, String html) {
            if (failing) throw new com.railway.reservation.service.mail.EmailNotSentException("test", null);
            sent.add(new Mail(to, subject, text, html));
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
