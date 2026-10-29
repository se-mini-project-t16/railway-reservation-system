package com.railway.reservation.dto;

import com.railway.reservation.model.BookingStatus;

import java.time.LocalDateTime;

public class CancellationResponseDto {

    private String pnr;
    private BookingStatus status;
    private LocalDateTime cancelledAt;

    public CancellationResponseDto() {}

    public CancellationResponseDto(String pnr, BookingStatus status, LocalDateTime cancelledAt) {
        this.pnr = pnr;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
