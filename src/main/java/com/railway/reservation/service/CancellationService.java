package com.railway.reservation.service;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.dto.RefundPreviewDto;

public interface CancellationService {

    /**
     * Shows the refund the caller would receive if they cancelled now,
     * without changing the booking (RRS-F-013).
     */
    RefundPreviewDto previewRefund(String pnr, Long userId);

    /**
     * Cancels the caller's CONFIRMED booking before departure, releases its seats,
     * initiates the rule-based refund and records an audit entry with user id and
     * timestamp (RRS-F-012, RRS-F-013, TC-CAN-01).
     */
    CancellationResponseDto cancelBooking(String pnr, Long userId);
}
