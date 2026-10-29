package com.railway.reservation.config;

import com.railway.reservation.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String RENEWED_TOKEN_HEADER = "X-Auth-Token";
    public static final String SESSION_EXPIRED_ATTRIBUTE = "auth.expired";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    Claims claims = jwtService.parseAndValidateToken(token);
                    String email = claims.getSubject();
                    String role = claims.get("role", String.class);
                    Number userId = claims.get("userId", Number.class);

                    if (email != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId != null ? userId.longValue() : email,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // Sliding renewal (RRS-SR-005): set before the chain runs, since headers
                        // cannot be added once the response is committed.
                        response.setHeader(RENEWED_TOKEN_HEADER, jwtService.renewToken(claims));
                    }
                } catch (ExpiredJwtException expired) {
                    // Lets the 401 entry point report an inactivity timeout instead of a generic 401
                    request.setAttribute(SESSION_EXPIRED_ATTRIBUTE, Boolean.TRUE);
                } catch (JwtException | IllegalArgumentException ignored) {
                    // Invalid, expired, or malformed token; continue unauthenticated
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
