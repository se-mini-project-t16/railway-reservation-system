package com.railway.reservation.dto;

import com.railway.reservation.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Public PNR-status projection (RRS-F-011).
 *
 * <p>This response deliberately excludes passenger names, age and gender, account
 * identity, payment state, gateway references, payment URLs, and internal booking
 * or train identifiers. Seat numbers are non-identifying and are the only
 * passenger-adjacent data exposed by the public status lookup.</p>
 */
public record PnrStatusResponseDto(
        String pnr,
        BookingStatus status,
        String trainNo,
        String trainName,
        String sourceStation,
        String destinationStation,
        LocalDate journeyDate,
        String className,
        BigDecimal totalFare,
        List<String> seatNumbers) {
}
