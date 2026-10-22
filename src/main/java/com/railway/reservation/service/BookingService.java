package com.railway.reservation.service;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;

public interface BookingService {

    /**
     * Reserves seats, validates passenger details, calculates total fare,
     * and generates a unique PNR (RRS-F-006, RRS-F-007).
     */
    BookingResponseDto createBooking(BookingRequestDto request);

    /**
     * Retrieves booking details by PNR (RRS-F-011).
     */
    BookingResponseDto getBookingByPnr(String pnr);

    BookingResponseDto getTicketByPnr(String pnr, Long userId);
}

