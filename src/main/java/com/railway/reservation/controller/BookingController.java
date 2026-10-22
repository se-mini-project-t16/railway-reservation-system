package com.railway.reservation.controller;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.railway.reservation.service.TicketPdfService;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final TicketPdfService ticketPdfService;

    public BookingController(BookingService bookingService, TicketPdfService ticketPdfService) {
        this.bookingService = bookingService;
        this.ticketPdfService = ticketPdfService;
    }

    /**
     * Endpoint to book one or more seats on a train (RRS-F-006, RRS-F-007).
     * Accepts passenger details and returns confirmed booking with unique PNR.
     */
    @PostMapping
    public ResponseEntity<BookingResponseDto> bookTicket(@Valid @RequestBody BookingRequestDto request) {
        BookingResponseDto response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to check PNR status and retrieve booking details (RRS-F-011).
     */
    @GetMapping("/{pnr}")
    public ResponseEntity<BookingResponseDto> getBookingStatus(@PathVariable String pnr, @AuthenticationPrincipal Long userId) {
        BookingResponseDto response = bookingService.getTicketByPnr(pnr, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{pnr}/ticket.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadTicket(@PathVariable String pnr, @AuthenticationPrincipal Long userId) {
        BookingResponseDto ticket = bookingService.getTicketByPnr(pnr, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket-" + pnr + ".pdf\"")
                .body(ticketPdfService.generate(ticket));
    }
}

