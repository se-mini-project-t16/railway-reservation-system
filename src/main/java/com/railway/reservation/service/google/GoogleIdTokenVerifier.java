package com.railway.reservation.service.google;

import com.railway.reservation.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Verifies Google ID tokens on the server: the RS256 signature against Google's published keys,
 * the issuer, the audience (our client ID) and the expiry. Nothing from the browser is trusted
 * until this passes.
 */
@Component
public class GoogleIdTokenVerifier implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifier.class);
    static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private final NimbusJwtDecoder decoder;

    public GoogleIdTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null || clientId.isBlank() ? null : clientId.trim();
        if (this.clientId == null) {
            this.decoder = null;
            log.info("Google sign-in is off: app.google.client-id is not set");
            return;
        }
        // Keys are fetched on first use and cached, so start-up does not depend on Google being reachable
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
        decoder.setJwtValidator(validator(this.clientId));
    }

    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtClaimValidator<String>(JwtClaimNames.ISS, ISSUERS::contains),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId)));
    }

    @Override
    public boolean enabled() {
        return decoder != null;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public GoogleIdentity verify(String credential) {
        if (!enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_DISABLED", "Google sign-in is not available right now.");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(credential);
        } catch (BadJwtException e) {
            log.warn("Rejected Google ID token: {}", e.getMessage());
            throw invalid();
        } catch (JwtException e) {
            log.error("Could not verify Google ID token: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_UNAVAILABLE",
                    "We could not reach Google to confirm your sign-in. Try again in a moment.");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) throw invalid();
        return new GoogleIdentity(subject, jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")), jwt.getClaimAsString("name"));
    }

    static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID",
                "Google could not confirm this sign-in. Try again.");
    }
}
