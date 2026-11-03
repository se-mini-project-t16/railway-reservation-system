package com.railway.reservation.controller;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.dto.PartialCancellationRequestDto;
import com.railway.reservation.dto.PartialCancellationResponseDto;
import com.railway.reservation.dto.PartialRefundPreviewDto;
import com.railway.reservation.dto.RefundPreviewDto;
import com.railway.reservation.service.CancellationService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
public class CancellationController {

    private final CancellationService cancellationService;

    public CancellationController(CancellationService cancellationService) {
        this.cancellationService = cancellationService;
    }

    /**
     * Shows the refund amount under the cancellation rules before the user
     * confirms the cancellation (RRS-F-013, TC-CAN-02). Changes nothing.
     */
    @GetMapping("/{pnr}/refund-preview")
    public ResponseEntity<RefundPreviewDto> previewRefund(
            @PathVariable String pnr,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(cancellationService.previewRefund(pnr, userId));
    }

    /**
     * Cancels the authenticated user's confirmed booking before departure,
     * releases its seats and initiates the refund (RRS-F-012, RRS-F-013, TC-CAN-01).
     */
    @PostMapping("/{pnr}/cancel")
    public ResponseEntity<CancellationResponseDto> cancelBooking(
            @PathVariable String pnr,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(cancellationService.cancelBooking(pnr, userId));
    }

    /**
     * Previews tiered refund calculation for selectively chosen passengers within a multi-passenger booking (RRS-F-012-EXT, RRS-F-013).
     */
    @PostMapping("/{pnr}/cancel-passengers/preview")
    public ResponseEntity<PartialRefundPreviewDto> previewPartialRefund(
            @PathVariable String pnr,
            @Valid @RequestBody PartialCancellationRequestDto request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(cancellationService.previewPartialRefund(pnr, request.getPassengerIds(), userId));
    }

    /**
     * Selectively cancels chosen individual passengers from a multi-passenger booking,
     * immediately releases their seats, initiates partial refund, and writes to audit logs (RRS-F-012-EXT, TC-CAN-01).
     */
    @PostMapping("/{pnr}/cancel-passengers")
    public ResponseEntity<PartialCancellationResponseDto> cancelPassengers(
            @PathVariable String pnr,
            @Valid @RequestBody PartialCancellationRequestDto request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(cancellationService.cancelPassengers(pnr, request.getPassengerIds(), userId));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }
        // JwtAuthenticationFilter sets the userId claim as the principal
        return authentication.getPrincipal() instanceof Number num ? num.longValue() : null;
    }
}
