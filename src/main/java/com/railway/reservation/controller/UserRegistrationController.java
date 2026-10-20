package com.railway.reservation.controller;

import com.railway.reservation.dto.RegisterRequestDto;
import com.railway.reservation.dto.RegisterResponseDto;
import com.railway.reservation.service.UserRegistrationService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserRegistrationController {

    private final UserRegistrationService registrationService;

    public UserRegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableRequest(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Request body is missing or malformed.");
        body.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(body);
    }
}
