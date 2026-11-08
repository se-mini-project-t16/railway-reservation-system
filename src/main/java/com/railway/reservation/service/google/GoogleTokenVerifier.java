package com.railway.reservation.service.google;

/**
 * Checks a Google ID token (the "credential" Google's sign-in button hands the page)
 * and returns who it belongs to. Implementations throw {@link com.railway.reservation.exception.ApiException}
 * with a client-facing code when the token cannot be accepted.
 */
public interface GoogleTokenVerifier {

    /** True when a Google OAuth client ID is configured. */
    boolean enabled();

    /** The OAuth client ID the sign-in button is created with, or null when Google sign-in is off. */
    String clientId();

    GoogleIdentity verify(String credential);
}
