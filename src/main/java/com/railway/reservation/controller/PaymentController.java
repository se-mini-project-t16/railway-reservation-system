package com.railway.reservation.controller;

import com.railway.reservation.dto.PaymentCallbackRequestDto;
import com.railway.reservation.dto.PaymentCallbackResponseDto;
import com.railway.reservation.dto.PaymentInitiateRequestDto;
import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public PaymentController(
            PaymentService paymentService,
            @Autowired(required = false) BookingRepository bookingRepository,
            @Autowired(required = false) UserRepository userRepository) {
        this.paymentService = paymentService;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Endpoint to initiate a payment session for a booking (RRS-F-008, TC-PAY-01, RRS-SR-006).
     * Enforces ownership authorization: only the booking owner or an admin can initiate payment.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentSessionDto> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequestDto request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (bookingRepository != null) {
            Booking booking = bookingRepository.findByPnr(request.getPnr()).orElse(null);
            if (booking != null && !isOwnerOrAdmin(booking, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        PaymentSessionDto session = paymentService.initiatePaymentSession(request.getPnr());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Endpoint to retrieve a payment session by its gateway reference.
     * Enforces ownership authorization: only the booking owner or an admin can retrieve details.
     */
    @GetMapping("/{gatewayRef}")
    public ResponseEntity<PaymentSessionDto> getPaymentByGatewayRef(
            @PathVariable String gatewayRef,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        PaymentSessionDto session = paymentService.getPaymentSessionByGatewayRef(gatewayRef);
        if (bookingRepository != null && session != null && session.getPnr() != null) {
            Booking booking = bookingRepository.findByPnr(session.getPnr()).orElse(null);
            if (booking != null && !isOwnerOrAdmin(booking, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(session);
    }

    /**
     * Endpoint to retrieve a payment session by booking PNR.
     * Enforces ownership authorization: only the booking owner or an admin can retrieve details.
     */
    @GetMapping("/booking/{pnr}")
    public ResponseEntity<PaymentSessionDto> getPaymentByPnr(
            @PathVariable String pnr,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (bookingRepository != null) {
            Booking booking = bookingRepository.findByPnr(pnr).orElse(null);
            if (booking != null && !isOwnerOrAdmin(booking, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        PaymentSessionDto session = paymentService.getPaymentSessionByPnr(pnr);
        return ResponseEntity.ok(session);
    }

    /**
     * Endpoint to handle payment gateway callbacks (RRS-F-009, TC-BK-01, TC-PAY-02).
     * If payment is verified SUCCESS, booking status transitions to CONFIRMED.
     * If payment is FAILED/DECLINED, booking transitions to FAILED and held seats are released.
     */
    @PostMapping("/callback")
    public ResponseEntity<PaymentCallbackResponseDto> handleCallback(@Valid @RequestBody PaymentCallbackRequestDto request) {
        PaymentCallbackResponseDto response = paymentService.handlePaymentCallback(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to verify payment status (alias / verification endpoint).
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentCallbackResponseDto> verifyPayment(@Valid @RequestBody PaymentCallbackRequestDto request) {
        PaymentCallbackResponseDto response = paymentService.handlePaymentCallback(request);
        return ResponseEntity.ok(response);
    }

    private boolean isOwnerOrAdmin(Booking booking, Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return false;
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return true;
        }
        Long userId = resolveUserId(authentication);
        return userId != null && booking.getUser() != null && userId.equals(booking.getUser().getId());
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number num) {
            return num.longValue();
        }
        if (principal instanceof User user) {
            return user.getId();
        }
        if (principal instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                if (userRepository != null) {
                    return userRepository.findByEmail(str).map(User::getId).orElse(null);
                }
            }
        }
        return null;
    }
}
