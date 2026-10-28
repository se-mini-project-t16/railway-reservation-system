package com.railway.reservation.service;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.PnrStatusResponseDto;

public interface BookingService {

    /**
     * Reserves seats, validates passenger details, calculates total fare,
     * and generates a unique PNR (RRS-F-006, RRS-F-007).
     */
    BookingResponseDto createBooking(BookingRequestDto request);

    /**
     * Retrieves the reduced public PNR-status projection (RRS-F-011).
     * The same response is returned for anonymous, owner and non-owner callers.
     */
    PnrStatusResponseDto getPublicPnrStatus(String pnr);

    /**
     * Retrieves the full e-ticket for the authenticated owner (RRS-F-010).
     * Payment must be verified before the ticket is returned.
     */
    BookingResponseDto getTicketByPnr(String pnr, Long userId);

    /**
     * Retrieves the full e-ticket for an administrator (RRS-F-010).
     * Payment must be verified before the ticket is returned.
     */
    BookingResponseDto getTicketByPnrForAdmin(String pnr);
}

