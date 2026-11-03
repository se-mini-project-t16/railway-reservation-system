package com.railway.reservation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Content Security Policy for the site: scripts, styles and fonts load only
 * from this server, nothing may frame the pages, and forms post only here.
 * The optional H2 console is exempt because it relies on inline script.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; "
            + "img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; "
            + "frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/h2-console")) {
            response.setHeader("Content-Security-Policy", POLICY);
            response.setHeader("Referrer-Policy", "same-origin");
            response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        }
        chain.doFilter(request, response);
    }
}
