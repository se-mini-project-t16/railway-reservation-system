package com.railway.reservation.config;

import com.railway.reservation.exception.ApiException;
import com.railway.reservation.service.DuplicateEmailException;
import com.railway.reservation.service.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts every failure into one JSON shape (RRS-NF-005, SAD 4.4):
 * {@code { timestamp, status, error, code, message, path, fieldErrors? }}.
 * No stack trace or SQL reaches the client; server errors are logged at
 * ERROR with timestamp, user id and correlation id.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            logError("Service error", ex.getStatus(), request, ex.getMessage(), ex);
        } else {
            logWarn(ex.getStatus(), request, ex.getCode() + ": " + ex.getMessage());
        }
        return respond(ex.getStatus(), ex.getCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors().forEach(e -> fields.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        String first = fields.values().stream().findFirst().orElse("Some details are missing or invalid.");
        logWarn(HttpStatus.BAD_REQUEST, request, "Validation failed " + fields);
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", first, request, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        String message = ex.getAllErrors().stream().findFirst()
                .map(e -> e.getDefaultMessage()).orElse("Some details are missing or invalid.");
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> handleBadInput(Exception ex, HttpServletRequest request) {
        String message = ex instanceof MissingServletRequestParameterException m
                ? "Missing " + m.getParameterName() + "."
                : ex instanceof MethodArgumentTypeMismatchException t
                ? "Invalid value for " + t.getName() + "."
                : "Request body is missing or malformed.";
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        logWarn(HttpStatus.BAD_REQUEST, request, message + " (" + cause.getClass().getSimpleName() + ": "
                + String.valueOf(cause.getMessage()).lines().findFirst().orElse("") + ")");
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logWarn(HttpStatus.BAD_REQUEST, request, ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        logWarn(HttpStatus.CONFLICT, request, "Duplicate email registration attempt");
        return respond(HttpStatus.CONFLICT, "EMAIL_TAKEN", ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        logError("Authentication failed: invalid credentials", HttpStatus.UNAUTHORIZED, request, "Invalid credentials.", null);
        return respond(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials.", request, null);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleDenied(AuthorizationDeniedException ex, HttpServletRequest request) {
        logWarn(HttpStatus.FORBIDDEN, request, "Access denied");
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this.", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "This address does not accept " + ex.getMethod() + ".", request, null);
    }

    /** Unknown addresses are a 404: JSON for the API, the site's own page for a browser. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) throws IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            ClassPathResource page = new ClassPathResource("static/404.html");
            if (page.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML)
                        .body(page.getContentAsString(StandardCharsets.UTF_8));
            }
        }
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "There is nothing at this address.", request, null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        logError("Database operation error", HttpStatus.INTERNAL_SERVER_ERROR, request, ex.getMessage(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                "A database error occurred. Please try again later.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, HttpServletRequest request) {
        logError("Uncaught server exception", HttpStatus.INTERNAL_SERVER_ERROR, request, ex.getMessage(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.", request, null);
    }

    private ResponseEntity<Map<String, Object>> respond(HttpStatus status, String code, String message,
                                                        HttpServletRequest request, Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", message != null ? message : "Error occurred.");
        body.put("path", request.getRequestURI());
        if (fields != null && !fields.isEmpty()) body.put("fieldErrors", fields);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private void logWarn(HttpStatus status, HttpServletRequest request, String detail) {
        logger.warn("WARN [RRS-NF-005] timestamp={}, userId={}, correlationId={}, status={}, path={}: {}",
                LocalDateTime.now(), resolveUserId(), resolveCorrelationId(), status.value(), request.getRequestURI(), detail);
    }

    private void logError(String summary, HttpStatus status, HttpServletRequest request, String detail, Throwable ex) {
        if (ex != null) {
            logger.error("ERROR [RRS-NF-005] timestamp={}, userId={}, correlationId={}, status={}, path={}, summary={}: {}",
                    LocalDateTime.now(), resolveUserId(), resolveCorrelationId(), status.value(), request.getRequestURI(),
                    summary, detail, ex);
        } else {
            logger.error("ERROR [RRS-NF-005] timestamp={}, userId={}, correlationId={}, status={}, path={}, summary={}: {}",
                    LocalDateTime.now(), resolveUserId(), resolveCorrelationId(), status.value(), request.getRequestURI(),
                    summary, detail);
        }
    }

    private String resolveUserId() {
        String mdcUser = MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY);
        if (mdcUser != null && !mdcUser.isBlank()) return mdcUser;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) return "anonymous";
        Object principal = auth.getPrincipal();
        return principal instanceof Number num ? num.toString() : auth.getName();
    }

    private String resolveCorrelationId() {
        String corr = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        return (corr != null && !corr.isBlank()) ? corr : "N/A";
    }
}
