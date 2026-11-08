package com.railway.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // H2 console, when enabled, uses frames
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = new HashMap<>();
                    body.put("timestamp", LocalDateTime.now());
                    body.put("status", HttpStatus.UNAUTHORIZED.value());
                    if (Boolean.TRUE.equals(request.getAttribute(JwtAuthenticationFilter.SESSION_EXPIRED_ATTRIBUTE))) {
                        // RRS-SR-005: token outlived the 15-minute inactivity window
                        body.put("error", "Session Expired");
                        body.put("code", "SESSION_EXPIRED");
                        body.put("message", "Session expired due to inactivity. Please sign in again.");
                    } else {
                        body.put("error", "Unauthorized");
                        body.put("code", "SIGN_IN_REQUIRED");
                        body.put("message", "Sign in to continue.");
                    }
                    body.put("path", request.getRequestURI());
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = new HashMap<>();
                    body.put("timestamp", LocalDateTime.now());
                    body.put("status", HttpStatus.FORBIDDEN.value());
                    body.put("error", "Forbidden");
                    body.put("code", "FORBIDDEN");
                    body.put("message", "You do not have access to this.");
                    body.put("path", request.getRequestURI());
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**", "/error").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                // Diagnostic routes registered only by the reliability tests
                .requestMatchers("/api/v1/test/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/register", "/api/v1/users/login",
                        "/api/v1/users/login/google", "/api/v1/users/register/verify", "/api/v1/users/register/resend").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/sign-in-options").permitAll()
                // Guests can discover journeys and check a PNR (statuses only, no names) [RRS-F-004, RRS-F-011]
                .requestMatchers(HttpMethod.GET, "/api/v1/stations/**", "/api/v1/trains/**", "/api/v1/pnr/**",
                        "/api/v1/policies/**", "/api/v1/payments/banks").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/bookings/quote").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Booking, payment, tickets and the account need a signed-in owner [RRS-SR-006]
                .requestMatchers("/api/**").authenticated()
                // Pages, styles, scripts, fonts and the 404 page are public
                .anyRequest().permitAll()
            )
            .addFilterBefore(new SecurityHeadersFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

