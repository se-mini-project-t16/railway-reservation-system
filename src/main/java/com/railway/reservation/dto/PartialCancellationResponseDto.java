package com.railway.reservation.dto;

import com.railway.reservation.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PartialCancellationResponseDto {

    private String pnr;
    private BookingStatus bookingStatus;
    private List<Long> cancelledPassengerIds;
    private List<String> cancelledPassengerNames;
    private int remainingActivePassengers;
    private BigDecimal refundAmount;
    private BigDecimal refundPercent;
    private String refundRef;
    private LocalDateTime cancelledAt;
    private String message;

    public PartialCancellationResponseDto() {}

    public PartialCancellationResponseDto(
            String pnr,
            BookingStatus bookingStatus,
            List<Long> cancelledPassengerIds,
            List<String> cancelledPassengerNames,
            int remainingActivePassengers,
            BigDecimal refundAmount,
            BigDecimal refundPercent,
            String refundRef,
            LocalDateTime cancelledAt,
            String message) {
        this.pnr = pnr;
        this.bookingStatus = bookingStatus;
        this.cancelledPassengerIds = cancelledPassengerIds;
        this.cancelledPassengerNames = cancelledPassengerNames;
        this.remainingActivePassengers = remainingActivePassengers;
        this.refundAmount = refundAmount;
        this.refundPercent = refundPercent;
        this.refundRef = refundRef;
        this.cancelledAt = cancelledAt;
        this.message = message;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public BookingStatus getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }

    public List<Long> getCancelledPassengerIds() { return cancelledPassengerIds; }
    public void setCancelledPassengerIds(List<Long> cancelledPassengerIds) { this.cancelledPassengerIds = cancelledPassengerIds; }

    public List<String> getCancelledPassengerNames() { return cancelledPassengerNames; }
    public void setCancelledPassengerNames(List<String> cancelledPassengerNames) { this.cancelledPassengerNames = cancelledPassengerNames; }

    public int getRemainingActivePassengers() { return remainingActivePassengers; }
    public void setRemainingActivePassengers(int remainingActivePassengers) { this.remainingActivePassengers = remainingActivePassengers; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getRefundPercent() { return refundPercent; }
    public void setRefundPercent(BigDecimal refundPercent) { this.refundPercent = refundPercent; }

    public String getRefundRef() { return refundRef; }
    public void setRefundRef(String refundRef) { this.refundRef = refundRef; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

