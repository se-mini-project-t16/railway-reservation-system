package com.railway.reservation.support;

import com.railway.reservation.exception.ApiException;
import com.railway.reservation.service.google.GoogleIdentity;
import com.railway.reservation.service.google.GoogleTokenVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

/**
 * Stands in for Google in tests. A credential of the form {@code test-google|<subject>|<email>}
 * is accepted as a verified Google identity; anything else is rejected the way a bad token is.
 */
@TestConfiguration
public class FakeGoogleConfig {

    public static final String CLIENT_ID = "test-client.apps.googleusercontent.com";

    public static String credential(String subject, String email) {
        return "test-google|" + subject + "|" + email;
    }

    @Bean
    @Primary
    public GoogleTokenVerifier fakeGoogleTokenVerifier() {
        return new GoogleTokenVerifier() {
            @Override public boolean enabled() { return true; }
            @Override public String clientId() { return CLIENT_ID; }
            @Override public GoogleIdentity verify(String credential) {
                String[] parts = credential == null ? new String[0] : credential.split("\\|");
                if (parts.length != 3 || !parts[0].equals("test-google")) {
                    throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID",
                            "Google could not confirm this sign-in. Try again.");
                }
                return new GoogleIdentity(parts[1], parts[2], true, "Test User");
            }
        };
    }
}
