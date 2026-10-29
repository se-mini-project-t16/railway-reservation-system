package com.railway.reservation.service;

import com.railway.reservation.dto.CancellationResponseDto;

public interface CancellationService {

    /**
     * Cancels the caller's CONFIRMED booking before departure, releases its seats
     * and records an audit entry with user id and timestamp (RRS-F-012, TC-CAN-01).
     */
    CancellationResponseDto cancelBooking(String pnr, Long userId);
}
