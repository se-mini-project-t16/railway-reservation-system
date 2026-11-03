package com.railway.reservation.controller;

import com.railway.reservation.dto.BookingDtos.*;
import com.railway.reservation.service.rail.BookingService;
import com.railway.reservation.service.rail.TicketPdfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Booking, tickets and cancellation for the signed-in traveller (RRS-F-006 to RRS-F-014). */
@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService bookingService;
    private final TicketPdfService ticketPdfService;

    public BookingController(BookingService bookingService, TicketPdfService ticketPdfService) {
        this.bookingService = bookingService;
        this.ticketPdfService = ticketPdfService;
    }

    /** Public PNR enquiry: statuses only (RRS-F-011). */
    @GetMapping("/pnr/{pnr}")
    public PnrStatus pnr(@PathVariable String pnr) {
        return bookingService.pnrStatus(pnr);
    }

    @PostMapping("/bookings/quote")
    public Quote quote(@Valid @RequestBody QuoteRequest request) {
        return bookingService.quote(request);
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingView> create(@Valid @RequestBody BookingRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(CurrentUser.id(auth), request));
    }

    @GetMapping("/bookings")
    public List<BookingSummary> mine(@RequestParam(required = false, defaultValue = "all") String scope, Authentication auth) {
        return bookingService.history(CurrentUser.id(auth), scope);
    }

    @GetMapping("/bookings/{pnr}")
    public BookingView one(@PathVariable String pnr, Authentication auth) {
        return bookingService.view(pnr, CurrentUser.id(auth), CurrentUser.isAdmin(auth));
    }

    @GetMapping(value = "/bookings/{pnr}/ticket.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable String pnr, Authentication auth) {
        BookingView view = bookingService.view(pnr, CurrentUser.id(auth), CurrentUser.isAdmin(auth));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket-" + view.pnr() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(ticketPdfService.render(view));
    }

    @PostMapping("/bookings/{pnr}/cancel/preview")
    public CancelPreview cancelPreview(@PathVariable String pnr, @Valid @RequestBody CancelRequest request, Authentication auth) {
        return bookingService.previewCancel(pnr, CurrentUser.id(auth), CurrentUser.isAdmin(auth), request.passengerIds());
    }

    @PostMapping("/bookings/{pnr}/cancel")
    public BookingView cancel(@PathVariable String pnr, @Valid @RequestBody CancelRequest request, Authentication auth) {
        return bookingService.cancel(pnr, CurrentUser.id(auth), CurrentUser.isAdmin(auth), request.passengerIds());
    }
}
