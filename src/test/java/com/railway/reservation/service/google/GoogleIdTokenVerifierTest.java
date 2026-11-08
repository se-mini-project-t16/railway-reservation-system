package com.railway.reservation.service.google;

import com.railway.reservation.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The checks applied to a Google ID token once its signature has been verified. */
class GoogleIdTokenVerifierTest {

    private static final String CLIENT = "123-abc.apps.googleusercontent.com";
    private final OAuth2TokenValidator<Jwt> validator = GoogleIdTokenVerifier.validator(CLIENT);

    private Jwt token(String issuer, String audience, Instant expires) {
        return Jwt.withTokenValue("t").header("alg", "RS256")
                .issuer(issuer).audience(List.of(audience)).subject("1234567890")
                .issuedAt(expires.minusSeconds(3600)).expiresAt(expires).build();
    }

    @Test
    @DisplayName("TC-GGL-10: A current token from Google for this client is accepted")
    void acceptsGoodToken() {
        Instant later = Instant.now().plusSeconds(600);
        assertThat(validator.validate(token("https://accounts.google.com", CLIENT, later)).hasErrors()).isFalse();
        assertThat(validator.validate(token("accounts.google.com", CLIENT, later)).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("TC-GGL-11: Tokens for another app, from another issuer, or expired are refused")
    void refusesBadClaims() {
        Instant later = Instant.now().plusSeconds(600);
        assertThat(validator.validate(token("https://accounts.google.com", "someone-else", later)).hasErrors()).isTrue();
        assertThat(validator.validate(token("https://evil.example.com", CLIENT, later)).hasErrors()).isTrue();
        assertThat(validator.validate(token("https://accounts.google.com", CLIENT, Instant.now().minusSeconds(3600))).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("TC-GGL-12: Without a client ID, Google sign-in is off and refuses every credential")
    void offWithoutClientId() {
        GoogleIdTokenVerifier off = new GoogleIdTokenVerifier("  ");
        assertThat(off.enabled()).isFalse();
        assertThat(off.clientId()).isNull();
        assertThatThrownBy(() -> off.verify("anything"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("GOOGLE_DISABLED");
    }
}
