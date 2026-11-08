package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.repository.PendingRegistrationRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import com.railway.reservation.service.UserRegistrationService;
import com.railway.reservation.support.MailCatcherConfig;
import com.railway.reservation.support.MutableClock;
import com.railway.reservation.support.SignUp;
import com.railway.reservation.support.TestClockConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sign-up confirms the email address with a 6-digit code before any account exists [RRS-F-001]. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({MailCatcherConfig.class, TestClockConfig.class})
class EmailVerificationTest {

    private static final String EMAIL = "priya@example.com";

    @Autowired MockMvc mvc;
    @Autowired MailCatcherConfig.MailCatcher mail;
    @Autowired MutableClock clock;
    @Autowired UserRepository userRepository;
    @Autowired PendingRegistrationRepository pendingRepository;
    @Autowired UserRegistrationService registrationService;
    @Autowired JwtService jwtService;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        clock.set(LocalDateTime.of(2026, 11, 9, 9, 0));
        mail.clear();
        pendingRepository.deleteAll();
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @AfterEach
    void tearDown() {
        clock.reset();
        mail.clear();
        pendingRepository.deleteAll();
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    private Map<String, String> details() {
        return Map.of("name", "Priya Shah", "email", EMAIL, "phone", "9876543210", "password", "Passenger#123");
    }

    private ResultActions resend() throws Exception {
        return mvc.perform(post("/api/v1/users/register/resend").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", EMAIL))));
    }

    private String wrong(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    @DisplayName("TC-REG-01: Sign-up emails a 6-digit code and creates nothing until it is confirmed")
    void codeIsEmailed() throws Exception {
        SignUp.start(mvc, details()).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                .andExpect(jsonPath("$.resendInSeconds").value(60));
        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
        assertThat(mail.sent()).hasSize(1);
        MailCatcherConfig.Mail m = mail.sent().get(0);
        String code = mail.codeFor(EMAIL);
        assertThat(code).matches("\\d{6}");
        assertThat(m.text()).contains(code).contains("Priya Shah").contains("10 minutes");
        assertThat(m.html()).contains(code);
        // The code is never stored in plain text
        assertThat(pendingRepository.findAll().get(0).getCodeHash()).startsWith("$2").doesNotContain(code);
    }

    @Test
    @DisplayName("TC-REG-02: The right code creates the account and signs the passenger in")
    void rightCodeCreatesAccount() throws Exception {
        JsonNode created = json.readTree(SignUp.complete(mvc, mail, details()));
        assertThat(userRepository.findByEmail(EMAIL)).isPresent();
        assertThat(jwtService.parseAndValidateToken(created.get("token").asText()).getSubject()).isEqualTo(EMAIL);
        assertThat(pendingRepository.count()).isZero();
        // The account now exists, so the address cannot be signed up again
        SignUp.start(mvc, details()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    @DisplayName("TC-REG-03: Wrong codes count down, and the fifth wrong code uses the code up")
    void wrongCodes() throws Exception {
        SignUp.start(mvc, details()).andExpect(status().isAccepted());
        String code = mail.codeFor(EMAIL);
        SignUp.verify(mvc, EMAIL, wrong(code)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INVALID"))
                .andExpect(jsonPath("$.message").value("That code is not right. 4 tries left."));
        for (int i = 0; i < 3; i++) SignUp.verify(mvc, EMAIL, wrong(code)).andExpect(jsonPath("$.code").value("CODE_INVALID"));
        SignUp.verify(mvc, EMAIL, wrong(code)).andExpect(jsonPath("$.code").value("CODE_USED_UP"));
        // Even the right code no longer works
        SignUp.verify(mvc, EMAIL, code).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_USED_UP"));
        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("TC-REG-04: A code expires after 10 minutes")
    void codeExpires() throws Exception {
        SignUp.start(mvc, details()).andExpect(status().isAccepted());
        String code = mail.codeFor(EMAIL);
        clock.advance(Duration.ofMinutes(11));
        SignUp.verify(mvc, EMAIL, code).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_EXPIRED"));
        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("TC-REG-05: A new code can be sent after a minute, and only the newest code works")
    void resendAfterAMinute() throws Exception {
        SignUp.start(mvc, details()).andExpect(status().isAccepted());
        String first = mail.codeFor(EMAIL);
        resend().andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("RESEND_TOO_SOON"));
        // Pressing "Continue" again inside the minute does not send a second email
        SignUp.start(mvc, details()).andExpect(status().isAccepted()).andExpect(jsonPath("$.resendInSeconds").value(60));
        assertThat(mail.sent()).hasSize(1);

        clock.advance(Duration.ofSeconds(61));
        resend().andExpect(status().isAccepted()).andExpect(jsonPath("$.resendInSeconds").value(60));
        assertThat(mail.sent()).hasSize(2);
        String second = mail.codeFor(EMAIL);
        if (!second.equals(first)) {
            SignUp.verify(mvc, EMAIL, first).andExpect(jsonPath("$.code").value("CODE_INVALID"));
        }
        SignUp.verify(mvc, EMAIL, second).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-REG-06: At most five codes an hour go to one address")
    void codesPerHour() throws Exception {
        SignUp.start(mvc, details()).andExpect(status().isAccepted());
        for (int i = 0; i < 4; i++) {
            clock.advance(Duration.ofSeconds(61));
            resend().andExpect(status().isAccepted());
        }
        clock.advance(Duration.ofSeconds(61));
        resend().andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("TOO_MANY_CODES"));
        assertThat(mail.sent()).hasSize(5);
        clock.advance(Duration.ofHours(1));
        resend().andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("TC-REG-07: When the email cannot be sent, the passenger is told and nothing is kept")
    void emailFailure() throws Exception {
        mail.failNext(true);
        SignUp.start(mvc, details()).andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("EMAIL_NOT_SENT"));
        assertThat(pendingRepository.count()).isZero();
        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("TC-REG-08: Confirming an email nobody signed up with is refused")
    void unknownEmail() throws Exception {
        SignUp.verify(mvc, "nobody@example.com", "123456").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SIGNUP_NOT_FOUND"));
        SignUp.verify(mvc, EMAIL, "12ab").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("TC-REG-09: Sign-ups left unconfirmed for a day are forgotten")
    void abandonedSignUpsArePurged() throws Exception {
        SignUp.start(mvc, details()).andExpect(status().isAccepted());
        assertThat(registrationService.purgeAbandoned()).isZero();
        clock.advance(Duration.ofHours(25));
        assertThat(registrationService.purgeAbandoned()).isEqualTo(1);
        assertThat(pendingRepository.count()).isZero();
    }
}
