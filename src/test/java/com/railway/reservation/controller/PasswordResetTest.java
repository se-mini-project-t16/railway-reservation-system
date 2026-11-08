package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.PasswordResetCodeRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import com.railway.reservation.service.PasswordResetService;
import com.railway.reservation.support.MailCatcherConfig;
import com.railway.reservation.support.MutableClock;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A forgotten password is reset with a code emailed to the account's address [RRS-F-002]. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({MailCatcherConfig.class, TestClockConfig.class})
class PasswordResetTest {

    private static final String EMAIL = "ravi@example.com";
    private static final String OLD = "OldPassword#1";
    private static final String NEW = "NewPassword#2";

    @Autowired MockMvc mvc;
    @Autowired MailCatcherConfig.MailCatcher mail;
    @Autowired MutableClock clock;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetCodeRepository codeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PasswordResetService resetService;
    @Autowired JwtService jwtService;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        clock.set(LocalDateTime.of(2026, 11, 9, 9, 0));
        mail.clear();
        codeRepository.deleteAll();
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
        userRepository.save(new User("Ravi Kumar", EMAIL, "9812345670", passwordEncoder.encode(OLD), Role.ROLE_PASSENGER));
    }

    @AfterEach
    void tearDown() {
        clock.reset();
        mail.clear();
        codeRepository.deleteAll();
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    private ResultActions forgot(String email) throws Exception {
        return mvc.perform(post("/api/v1/users/password/forgot").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email))));
    }

    private ResultActions reset(String code, String password) throws Exception {
        return mvc.perform(post("/api/v1/users/password/reset").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", EMAIL, "code", code, "newPassword", password))));
    }

    private ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/v1/users/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", EMAIL, "password", password))));
    }

    /** The code in the newest reset email, once it has arrived. */
    private String code() {
        mail.await(EMAIL, "password reset code");
        return mail.codeFor(EMAIL);
    }

    private String wrong(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    @DisplayName("TC-PWD-01: A reset code goes only to registered addresses, and the answer never reveals which")
    void noAccountEnumeration() throws Exception {
        String known = forgot(EMAIL).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String unknown = forgot("nobody@example.com").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertThat(unknown.replace("nobody@example.com", EMAIL)).isEqualTo(known);
        assertThat(code()).matches("\\d{6}");
        assertThat(mail.noneWithin("nobody", 300)).isTrue();
        assertThat(mail.sent()).noneMatch(m -> m.to().equals("nobody@example.com"));
        assertThat(codeRepository.findAll().getFirst().getCodeHash()).startsWith("$2");
    }

    @Test
    @DisplayName("TC-PWD-02: The right code sets the new password, signs the passenger in and emails a notice")
    void resetWorks() throws Exception {
        forgot(EMAIL).andExpect(status().isAccepted());
        String token = json.readTree(reset(code(), NEW).andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("token").asText();
        assertThat(jwtService.parseAndValidateToken(token).getSubject()).isEqualTo(EMAIL);
        login(OLD).andExpect(status().isUnauthorized());
        login(NEW).andExpect(status().isOk());
        assertThat(mail.await(EMAIL, "password was changed").text()).contains("Forgot password");
        assertThat(codeRepository.count()).isZero();
    }

    @Test
    @DisplayName("TC-PWD-03: Wrong codes are refused the same way, and five of them use the code up")
    void wrongCodes() throws Exception {
        forgot(EMAIL).andExpect(status().isAccepted());
        String code = code();
        for (int i = 0; i < 5; i++) {
            reset(wrong(code), NEW).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_INVALID"));
        }
        reset(code, NEW).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_INVALID"));
        login(OLD).andExpect(status().isOk());
        // An address with no account gets exactly the same answer
        mvc.perform(post("/api/v1/users/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "nobody@example.com", "code", "123456", "newPassword", NEW))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_INVALID"));
    }

    @Test
    @DisplayName("TC-PWD-04: A reset code expires after 10 minutes")
    void expiry() throws Exception {
        forgot(EMAIL).andExpect(status().isAccepted());
        String code = code();
        clock.advance(Duration.ofMinutes(11));
        reset(code, NEW).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_EXPIRED"));
        login(OLD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-PWD-05: A new code can be sent after a minute, at most five an hour, and only the newest works")
    void resendLimits() throws Exception {
        forgot(EMAIL).andExpect(status().isAccepted());
        String first = code();
        forgot(EMAIL).andExpect(status().isAccepted());
        assertThat(mail.noneWithin("never", 300)).isTrue();
        assertThat(mail.sent()).hasSize(1);
        for (int i = 0; i < 6; i++) {
            clock.advance(Duration.ofSeconds(61));
            forgot(EMAIL).andExpect(status().isAccepted());
        }
        assertThat(mail.noneWithin("never", 500)).isTrue();
        assertThat(mail.sent()).hasSize(5);
        String newest = code();
        if (!newest.equals(first)) reset(first, NEW).andExpect(status().isBadRequest());
        reset(newest, NEW).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-PWD-06: Resetting the password lifts a lockout from wrong passwords")
    void clearsLockout() throws Exception {
        User u = userRepository.findByEmail(EMAIL).orElseThrow();
        u.setFailedLoginAttempts(5);
        u.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        userRepository.save(u);
        forgot(EMAIL).andExpect(status().isAccepted());
        reset(code(), NEW).andExpect(status().isOk());
        login(NEW).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-PWD-07: The new password follows the sign-up rules; reset codes left unused for a day are removed")
    void rulesAndCleanup() throws Exception {
        forgot(EMAIL).andExpect(status().isAccepted());
        String code = code();
        reset(code, "short").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.newPassword").exists());
        reset(code, "é".repeat(37)).andExpect(status().isBadRequest());
        clock.advance(Duration.ofHours(25));
        assertThat(resetService.purgeOld()).isEqualTo(1);
    }
}
