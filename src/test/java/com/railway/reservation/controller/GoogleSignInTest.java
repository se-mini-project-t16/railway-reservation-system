package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import com.railway.reservation.support.FakeGoogleConfig;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Google sign-in is link only: it opens an existing account whose owner linked Google, and never creates one. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(FakeGoogleConfig.class)
class GoogleSignInTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private final ObjectMapper json = new ObjectMapper();

    private User user(String email) {
        return userRepository.save(new User("Meera Nair", email, "9876543210", passwordEncoder.encode("Password#1"), Role.ROLE_PASSENGER));
    }

    private String body(String credential) throws Exception {
        return json.writeValueAsString(Map.of("credential", credential));
    }

    private ResultActions googleLogin(String credential) throws Exception {
        return mvc.perform(post("/api/v1/users/login/google").contentType(MediaType.APPLICATION_JSON).content(body(credential)));
    }

    private ResultActions link(String token, String credential) throws Exception {
        return mvc.perform(post("/api/v1/me/google").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body(credential)));
    }

    @Test
    @DisplayName("TC-GGL-01: The sign-in page is told the Google client ID")
    void signInOptions() throws Exception {
        mvc.perform(get("/api/v1/users/sign-in-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientId").value(FakeGoogleConfig.CLIENT_ID));
    }

    @Test
    @DisplayName("TC-GGL-02: An unlinked Google account cannot sign in and no account is created")
    void unlinkedGoogleIsRefused() throws Exception {
        long before = userRepository.count();
        googleLogin(FakeGoogleConfig.credential("g-100", "stranger@gmail.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GOOGLE_NOT_LINKED"))
                .andExpect(jsonPath("$.token").doesNotExist());
        assertThat(userRepository.count()).isEqualTo(before);
        assertThat(userRepository.findByEmail("stranger@gmail.com")).isEmpty();
    }

    @Test
    @DisplayName("TC-GGL-03: After linking from the account page, Google signs in to that same account")
    void linkThenSignIn() throws Exception {
        User u = user("meera@example.com");
        String token = jwtService.generateToken(u);
        link(token, FakeGoogleConfig.credential("g-200", "meera.personal@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("meera@example.com"))
                .andExpect(jsonPath("$.googleEmail").value("meera.personal@gmail.com"));

        String response = googleLogin(FakeGoogleConfig.credential("g-200", "meera.personal@gmail.com"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String googleToken = json.readTree(response).get("token").asText();
        assertThat(jwtService.parseAndValidateToken(googleToken).get("userId", Number.class).longValue()).isEqualTo(u.getId());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + googleToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("meera@example.com"));
    }

    @Test
    @DisplayName("TC-GGL-04: Linking needs a signed-in user")
    void linkNeedsSignIn() throws Exception {
        mvc.perform(post("/api/v1/me/google").contentType(MediaType.APPLICATION_JSON)
                        .content(body(FakeGoogleConfig.credential("g-300", "x@gmail.com"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-GGL-05: One Google account can be linked to only one account here")
    void googleAccountLinkedOnce() throws Exception {
        String first = jwtService.generateToken(user("first@example.com"));
        String second = jwtService.generateToken(user("second@example.com"));
        String credential = FakeGoogleConfig.credential("g-400", "shared@gmail.com");
        link(first, credential).andExpect(status().isOk());
        link(second, credential).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GOOGLE_IN_USE"));
        link(first, FakeGoogleConfig.credential("g-401", "other@gmail.com"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GOOGLE_ALREADY_LINKED"));
        // Linking the same Google account again is harmless
        link(first, credential).andExpect(status().isOk()).andExpect(jsonPath("$.googleEmail").value("shared@gmail.com"));
    }

    @Test
    @DisplayName("TC-GGL-06: A forged or malformed Google credential is rejected")
    void invalidCredential() throws Exception {
        googleLogin("eyJhbGciOiJub25lIn0.e30.").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GOOGLE_TOKEN_INVALID"));
        mvc.perform(post("/api/v1/users/login/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-GGL-07: A locked account stays locked for Google sign-in too")
    void lockedAccountStaysLocked() throws Exception {
        User u = user("locked@example.com");
        u.linkGoogle("g-500", "locked@gmail.com", LocalDateTime.now());
        u.setFailedLoginAttempts(5);
        u.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        userRepository.save(u);
        googleLogin(FakeGoogleConfig.credential("g-500", "locked@gmail.com"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("TC-GGL-08: After unlinking, the Google account no longer signs in")
    void unlink() throws Exception {
        User u = user("unlink@example.com");
        String token = jwtService.generateToken(u);
        link(token, FakeGoogleConfig.credential("g-600", "unlink@gmail.com")).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/me/google").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.googleEmail").doesNotExist());
        googleLogin(FakeGoogleConfig.credential("g-600", "unlink@gmail.com"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("GOOGLE_NOT_LINKED"));
    }

    @Test
    @DisplayName("TC-GGL-09: Only the sign-in and account pages may load Google's script")
    void contentSecurityPolicy() throws Exception {
        assertThat(mvc.perform(get("/signin.html")).andReturn().getResponse().getHeader("Content-Security-Policy"))
                .contains("script-src 'self' https://accounts.google.com/gsi/client")
                .contains("frame-src https://accounts.google.com/gsi/");
        assertThat(mvc.perform(get("/index.html")).andReturn().getResponse().getHeader("Content-Security-Policy"))
                .doesNotContain("google").doesNotContain("unsafe-inline").contains("script-src 'self';");
    }

    @Test
    @DisplayName("TC-SEC-08: Requests that arrive over HTTPS are told to stay on HTTPS; plain local HTTP is not")
    void strictTransportSecurity() throws Exception {
        assertThat(mvc.perform(get("/index.html").header("X-Forwarded-Proto", "https")).andReturn().getResponse()
                .getHeader("Strict-Transport-Security")).isEqualTo("max-age=31536000");
        assertThat(mvc.perform(get("/index.html")).andReturn().getResponse().getHeader("Strict-Transport-Security")).isNull();
    }
}
