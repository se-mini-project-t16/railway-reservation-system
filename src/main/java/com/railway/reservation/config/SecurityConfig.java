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
            .headers(headers -> headers.frameOptions(frame -> frame.disable())) // for H2 console
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
                        body.put("message", "Session expired due to inactivity. Please sign in again.");
                    } else {
                        body.put("error", "Unauthorized");
                        body.put("message", authException.getMessage() != null ? authException.getMessage() : "Full authentication is required to access this resource");
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
                    body.put("message", accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Access is denied");
                    body.put("path", request.getRequestURI());
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                .requestMatchers("/api/v1/test/**").permitAll()
                .requestMatchers("/api/v1/users/register", "/api/v1/users/login").permitAll()
                .requestMatchers("/api/v1/trains/**").permitAll()
                // Specific booking endpoints requiring authentication (must precede generic /bookings/*)
                .requestMatchers("/api/v1/bookings/*/ticket",
                        "/api/v1/bookings/*/ticket.pdf",
                        "/api/v1/bookings/*/cancel",
                        "/api/v1/bookings/*/refund-preview").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/bookings/fare-quote").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/bookings").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/bookings", "/api/v1/bookings/", "/api/v1/bookings/history").authenticated()
                // Public PNR status lookup for guests [RRS-F-011]
                .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*").permitAll()
                // Payment callbacks for gateway simulation
                .requestMatchers("/api/v1/payments/callback", "/api/v1/payments/verify").permitAll()
                // Payment session initiation and reads require authentication [RRS-SR-006]
                .requestMatchers(HttpMethod.POST, "/api/v1/payments/initiate").authenticated()
                .requestMatchers("/api/v1/payments/**").authenticated()
                .requestMatchers("/", "/index.html", "/payment.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

