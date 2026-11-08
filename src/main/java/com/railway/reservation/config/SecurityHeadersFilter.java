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
 * and the pay page allows Razorpay Checkout; no other page loads anything
 * from outside. The optional H2 console is exempt because it relies
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

    /** Hash of the one {@code <style>} block Razorpay Checkout adds to the page for its overlay. */
    static final String CHECKOUT_STYLES = "sha256-VYoJM+B5VwF3YDs5YIEt5XASBqk8M2apkiuz5wugOu4=";

    /**
     * Razorpay Checkout: its script, and the frame on Razorpay's own domain where the payment is made.
     * Checkout positions its overlay with style attributes, so those (never script) are allowed here.
     */
    static final String RAZORPAY_POLICY = "default-src 'self'; "
            + "script-src 'self' https://checkout.razorpay.com; style-src 'self' '" + CHECKOUT_STYLES + "'; "
            + "style-src-attr 'unsafe-inline'; font-src 'self'; "
            + "img-src 'self' data: https://*.razorpay.com; connect-src 'self' https://*.razorpay.com; "
            + "frame-src https://api.razorpay.com https://checkout.razorpay.com; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    static final String PAY_PAGE = "/pay.html";

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
        if (GOOGLE_PAGES.contains(path)) return GOOGLE_POLICY;
        if (PAY_PAGE.equals(path)) return RAZORPAY_POLICY;
        return POLICY;
    }
}
