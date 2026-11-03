package com.railway.reservation.service;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.FareBreakdownDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.dto.PnrStatusResponseDto;
import java.util.List;

public interface BookingService {

    /**
     * Calculates authoritative itemized fare breakdown and senior citizen concession (RRS-F-007-EXT).
     */
    FareBreakdownDto calculateFareQuote(Long classId, List<PassengerDto> passengers);

    /**
     * Reserves seats, validates passenger details, calculates total fare,
     * and generates a unique PNR (RRS-F-006, RRS-F-007).
     */
    BookingResponseDto createBooking(BookingRequestDto request);

    /**
     * Retrieves booking details by PNR (RRS-F-011).
     */
    BookingResponseDto getBookingByPnr(String pnr);

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

    /**
     * Retrieves the chronological booking history for the authenticated user (RRS-F-014).
     * Past and upcoming bookings are sorted newest first by journey date and creation date.
     *
     * @param userId ID of the user requesting their booking history
     * @return list of bookings belonging strictly to the user, ordered chronologically
     */
    List<BookingResponseDto> getBookingHistory(Long userId);
}

