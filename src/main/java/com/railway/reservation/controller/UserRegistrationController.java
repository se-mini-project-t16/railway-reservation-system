package com.railway.reservation.controller;

import com.railway.reservation.dto.AccountDtos.GoogleCredential;
import com.railway.reservation.dto.AccountDtos.SignInOptions;
import com.railway.reservation.dto.RegisterRequestDto;
import com.railway.reservation.dto.RegisterResponseDto;
import com.railway.reservation.dto.SignUpDtos.ResendCodeRequest;
import com.railway.reservation.dto.SignUpDtos.SignUpStarted;
import com.railway.reservation.dto.SignUpDtos.VerifyEmailRequest;
import com.railway.reservation.dto.LoginRequestDto;
import com.railway.reservation.dto.LoginResponseDto;
import com.railway.reservation.service.AuthenticationService;
import com.railway.reservation.service.UserRegistrationService;
import com.railway.reservation.service.google.GoogleSignInService;
import com.railway.reservation.service.google.GoogleTokenVerifier;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final AuthenticationService authenticationService;
    private final GoogleSignInService googleSignInService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public UserRegistrationController(
            UserRegistrationService registrationService,
            AuthenticationService authenticationService,
            GoogleSignInService googleSignInService,
            GoogleTokenVerifier googleTokenVerifier) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.googleSignInService = googleSignInService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    /** Step 1 of sign-up: emails a 6-digit code. The account does not exist until step 2. */
    @PostMapping("/register")
    public ResponseEntity<SignUpStarted> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(registrationService.start(request));
    }

    /** Step 2: the code from the email creates the account and signs the passenger in. */
    @PostMapping("/register/verify")
    public ResponseEntity<RegisterResponseDto> verify(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.verify(request.email(), request.code()));
    }

    @PostMapping("/register/resend")
    public ResponseEntity<SignUpStarted> resend(@Valid @RequestBody ResendCodeRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(registrationService.resend(request.email()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    /** Signs in with a Google account that the owner has already linked. Never creates an account. */
    @PostMapping("/login/google")
    public ResponseEntity<LoginResponseDto> loginWithGoogle(@Valid @RequestBody GoogleCredential request) {
        return ResponseEntity.ok(googleSignInService.signIn(request.credential()));
    }

    @GetMapping("/sign-in-options")
    public SignInOptions signInOptions() {
        return new SignInOptions(googleTokenVerifier.enabled() ? googleTokenVerifier.clientId() : null);
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
