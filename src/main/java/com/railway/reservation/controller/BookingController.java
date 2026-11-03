package com.railway.reservation.controller;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.FareBreakdownDto;
import com.railway.reservation.dto.FareQuoteRequestDto;
import com.railway.reservation.dto.PnrStatusResponseDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.User;
import com.railway.reservation.service.BookingService;
import com.railway.reservation.service.TicketPdfService;
import com.railway.reservation.validation.SafeTextRules;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.railway.reservation.repository.UserRepository;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final TicketPdfService ticketPdfService;
    private final UserRepository userRepository;

    public BookingController(
            BookingService bookingService,
            TicketPdfService ticketPdfService,
            UserRepository userRepository) {
        this.bookingService = bookingService;
        this.ticketPdfService = ticketPdfService;
        this.userRepository = userRepository;
    }



    /**
     * Endpoint to preview authoritative itemized fare breakdown and senior concession quote (RRS-F-007-EXT).
     */
    @PostMapping("/fare-quote")
    public ResponseEntity<FareBreakdownDto> getFareQuote(@Valid @RequestBody FareQuoteRequestDto request) {
        FareBreakdownDto quote = bookingService.calculateFareQuote(request.getClassId(), request.getPassengers());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(quote);
    }

    /**
     * Endpoint to book one or more seats on a train (RRS-F-006, RRS-F-007, RRS-SR-006).
     * Accepts passenger details and derives booking owner from authenticated principal.
     */
    @PostMapping
    public ResponseEntity<BookingResponseDto> bookTicket(
            @Valid @RequestBody BookingRequestDto request,
            Authentication authentication) {
        Long authenticatedUserId = resolveUserId(authentication);
        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Strict ownership derivation: server overrides/derives booking owner from authenticated principal
        request.setUserId(authenticatedUserId);
        BookingResponseDto response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to retrieve the chronological booking history of the authenticated user (RRS-F-014).
     * Returns past and upcoming bookings sorted newest first.
     */
    @GetMapping({"", "/", "/history"})
    public ResponseEntity<List<BookingResponseDto>> getBookingHistory(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<BookingResponseDto> history = bookingService.getBookingHistory(userId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(history);
    }

    /**
     * Endpoint to check PNR status and retrieve booking details (RRS-F-011).
     * Validates and sanitizes PNR input against SQL injection and XSS (RRS-SR-003).
     */
    @GetMapping("/{pnr}")
    public ResponseEntity<BookingResponseDto> getBookingStatus(@PathVariable String pnr) {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new ResourceNotFoundException("Booking not found with PNR: " + pnr);
        }
        SafeTextRules.requireSafe("PNR", pnr);
        BookingResponseDto response = bookingService.getBookingByPnr(pnr.trim());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    /**
     * Full e-ticket for the authenticated booking owner or an administrator
     * (RRS-F-010). The service enforces verified payment before returning data.
     */
    @GetMapping("/{pnr}/ticket")
    public ResponseEntity<BookingResponseDto> getTicket(
            @PathVariable String pnr,
            Authentication authentication) {
        Optional<BookingResponseDto> ticket = resolveAuthorizedTicket(pnr, authentication);
        if (ticket.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ticket.get());
    }

    /**
     * Generates and downloads the e-ticket electronic reservation slip (ERS) as a PDF file (RRS-F-010).
     * Accessible to the authorized owner or an administrator after verified payment.
     */
    @GetMapping(value = "/{pnr}/ticket.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadTicket(
            @PathVariable String pnr,
            Authentication authentication) {
        Optional<BookingResponseDto> ticket = resolveAuthorizedTicket(pnr, authentication);
        if (ticket.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        byte[] pdfBytes = ticketPdfService.generate(ticket.get());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ticket-" + safeFilename(pnr) + ".pdf\"")
                .body(pdfBytes);
    }

    private Optional<BookingResponseDto> resolveAuthorizedTicket(String pnr, Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return Optional.of(bookingService.getTicketByPnrForAdmin(pnr));
        }

        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return Optional.empty();
        }

        return Optional.of(bookingService.getTicketByPnr(pnr, userId));
    }

    private String safeFilename(String pnr) {
        return pnr == null ? "ticket" : pnr.replaceAll("[^A-Za-z0-9_-]", "_");
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
