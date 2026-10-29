package com.railway.reservation.controller;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.service.CancellationService;
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
     * Cancels the authenticated user's confirmed booking before departure
     * and releases its seats (RRS-F-012, TC-CAN-01).
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

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }
        // JwtAuthenticationFilter sets the userId claim as the principal
        return authentication.getPrincipal() instanceof Number num ? num.longValue() : null;
    }
}
