package com.railway.reservation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Content Security Policy for the site: scripts, styles and fonts load only
 * from this server, nothing may frame the pages, and forms post only here.
 * The sign-in and account pages additionally allow Google's sign-in button,
 * and nothing else does. The optional H2 console is exempt because it relies
 * on inline script.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; "
            + "img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; "
            + "frame-ancestors 'none'";

    /**
     * Hash of the {@code <style id="googleidentityservice_button_styles">} block Google's library adds.
     * If Google changes it, the browser only logs a warning: the button is drawn inside Google's frame.
     */
    static final String GSI_BUTTON_STYLES = "sha256-RU4sU0AaS8IBGZx8XrGt/pa9A5SLA3dQszGeqT5L3Kw=";

    /**
     * Google Identity Services, as listed in Google's own CSP guidance for the sign-in button.
     * The library sizes its iframe with style attributes, so inline style attributes (never
     * script) are allowed on these two pages only.
     */
    static final String GOOGLE_POLICY = "default-src 'self'; "
            + "script-src 'self' https://accounts.google.com/gsi/client; "
            + "style-src 'self' https://accounts.google.com/gsi/style '" + GSI_BUTTON_STYLES + "'; "
            + "style-src-attr 'unsafe-inline'; font-src 'self'; "
            + "img-src 'self' data:; connect-src 'self' https://accounts.google.com/gsi/; "
            + "frame-src https://accounts.google.com/gsi/; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    static final Set<String> GOOGLE_PAGES = Set.of("/signin.html", "/account.html");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/h2-console")) {
            response.setHeader("Content-Security-Policy", policyFor(path));
            // Google's button needs the page origin (never the path or query) to confirm where it is shown
            response.setHeader("Referrer-Policy", GOOGLE_PAGES.contains(path) ? "strict-origin-when-cross-origin" : "same-origin");
            response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        }
        chain.doFilter(request, response);
    }

    static String policyFor(String path) {
        return GOOGLE_PAGES.contains(path) ? GOOGLE_POLICY : POLICY;
    }
}
