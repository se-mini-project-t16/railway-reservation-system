package com.railway.reservation.config;

import com.railway.reservation.exception.CancellationNotAllowedException;
import com.railway.reservation.exception.DuplicateTrainException;
import com.railway.reservation.exception.PaymentNotVerifiedException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.exception.SeatUnavailableException;
import com.railway.reservation.exception.TrainDeletionException;
import com.railway.reservation.service.DuplicateEmailException;
import com.railway.reservation.service.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler (RRS-NF-005, SAD Section 4.4).
 * Converts uncaught exceptions to standardized JSON:
 *   { timestamp, status, error, message, path }
 * Ensures no stack traces or raw SQL leak to end users, and logs all errors at ERROR
 * level with timestamp and user ID for post-incident debugging and auditing.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        logError("Validation failed", HttpStatus.BAD_REQUEST, request, errors.toString(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "Bad Request", errors.toString(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(PaymentNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotVerified(
            PaymentNotVerifiedException ex, HttpServletRequest request) {
        logError("Payment not verified", HttpStatus.CONFLICT, request, ex.getMessage(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        logError("Resource not found", HttpStatus.NOT_FOUND, request, ex.getMessage(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleSeatUnavailable(
            SeatUnavailableException ex, HttpServletRequest request) {
        logError("Seat unavailable", HttpStatus.CONFLICT, request, ex.getMessage(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(CancellationNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleCancellationNotAllowed(
            CancellationNotAllowedException ex, HttpServletRequest request) {
        logError("Cancellation not allowed", HttpStatus.BAD_REQUEST, request, ex.getMessage(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        logError("Invalid argument", HttpStatus.BAD_REQUEST, request, ex.getMessage(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        logError("Duplicate email registration attempt", HttpStatus.CONFLICT, request, ex.getMessage(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(DuplicateTrainException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateTrain(
            DuplicateTrainException ex, HttpServletRequest request) {
        logError("Duplicate train configuration", HttpStatus.CONFLICT, request, ex.getMessage(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(TrainDeletionException.class)
    public ResponseEntity<Map<String, Object>> handleTrainDeletion(
            TrainDeletionException ex, HttpServletRequest request) {
        logError("Train deletion conflict", HttpStatus.CONFLICT, request, ex.getMessage(), null);

        Map<String, Object> body = buildErrorBody(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        logError("Authentication failed: invalid credentials", HttpStatus.UNAUTHORIZED, request, "Invalid credentials.", null);

        Map<String, Object> body = buildErrorBody(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Database access exception handler (RRS-NF-005, SAD Section 4.4).
     * Prevents raw SQL, table schemas, or query strings from leaking to end users.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        logError("Database operation error", HttpStatus.INTERNAL_SERVER_ERROR, request, ex.getMessage(), ex);

        Map<String, Object> body = buildErrorBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "A database error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * General uncaught exception fallback handler (RRS-NF-005, SAD Section 4.4).
     * Converts uncaught exceptions into standard JSON without leaking internal stack traces.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        logError("Uncaught server exception", HttpStatus.INTERNAL_SERVER_ERROR, request, ex.getMessage(), ex);

        Map<String, Object> body = buildErrorBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> buildErrorBody(HttpStatus status, String error, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message != null ? message : "Error occurred.");
        body.put("path", path);
        return body;
    }

    private void logError(String summary, HttpStatus status, HttpServletRequest request, String detail, Throwable ex) {
        String userId = resolveUserId();
        String correlationId = resolveCorrelationId();
        LocalDateTime now = LocalDateTime.now();

        if (ex != null) {
            logger.error("ERROR [RRS-NF-005] timestamp={}, userId={}, correlationId={}, status={}, path={}, summary={}: {}",
                    now, userId, correlationId, status.value(), request.getRequestURI(), summary, detail, ex);
        } else {
            logger.error("ERROR [RRS-NF-005] timestamp={}, userId={}, correlationId={}, status={}, path={}, summary={}: {}",
                    now, userId, correlationId, status.value(), request.getRequestURI(), summary, detail);
        }
    }

    private String resolveUserId() {
        String mdcUser = MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY);
        if (mdcUser != null && !mdcUser.isBlank()) {
            return mdcUser;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return "anonymous";
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Number num) {
            return num.toString();
        }
        return auth.getName();
    }

    private String resolveCorrelationId() {
        String corr = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        return (corr != null && !corr.isBlank()) ? corr : "N/A";
    }
}
