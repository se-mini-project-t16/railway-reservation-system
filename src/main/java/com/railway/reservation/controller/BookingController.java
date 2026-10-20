package com.railway.reservation.controller;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
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
    public ResponseEntity<BookingResponseDto> getBookingStatus(@PathVariable String pnr) {
        BookingResponseDto response = bookingService.getBookingByPnr(pnr);
        return ResponseEntity.ok(response);
    }
}

