package com.railway.reservation.service;

import com.railway.reservation.dto.CancellationResponseDto;
import com.railway.reservation.dto.PartialCancellationResponseDto;
import com.railway.reservation.dto.PartialRefundPreviewDto;
import com.railway.reservation.dto.RefundPreviewDto;

import java.util.List;

public interface CancellationService {

    /**
     * Shows the refund the caller would receive if they cancelled now,
     * without changing the booking (RRS-F-013).
     */
    RefundPreviewDto previewRefund(String pnr, Long userId);

    /**
     * Shows tiered refund preview for selectively chosen passengers within a multi-passenger booking (RRS-F-012-EXT, RRS-F-013).
     */
    PartialRefundPreviewDto previewPartialRefund(String pnr, List<Long> passengerIds, Long userId);

    /**
     * Cancels the caller's CONFIRMED booking before departure, releases its seats,
     * initiates the rule-based refund and records an audit entry with user id and
     * timestamp (RRS-F-012, RRS-F-013, TC-CAN-01).
     */
    CancellationResponseDto cancelBooking(String pnr, Long userId);

    /**
     * Selectively cancels chosen individual passengers from a multi-passenger booking,
     * immediately releases their seats, initiates partial refund, and writes to audit logs (RRS-F-012-EXT, TC-CAN-01).
     */
    PartialCancellationResponseDto cancelPassengers(String pnr, List<Long> passengerIds, Long userId);
}
