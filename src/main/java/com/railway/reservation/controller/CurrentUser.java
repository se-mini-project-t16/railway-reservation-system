package com.railway.reservation.controller;

import com.railway.reservation.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/** Reads the signed-in user from the JWT principal; the browser never supplies an owner id. */
final class CurrentUser {

    private CurrentUser() {}

    static Long id(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SIGN_IN_REQUIRED", "Sign in to continue.");
        }
        return id;
    }

    static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
