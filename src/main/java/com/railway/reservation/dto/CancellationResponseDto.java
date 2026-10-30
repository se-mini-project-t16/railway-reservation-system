package com.railway.reservation.dto;

import com.railway.reservation.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CancellationResponseDto {

    private String pnr;
    private BookingStatus status;
    private BigDecimal refund;
    private BigDecimal refundPercent;
    private String refundRef;
    private LocalDateTime cancelledAt;

    public CancellationResponseDto() {}

    public CancellationResponseDto(String pnr, BookingStatus status, BigDecimal refund,
                                   BigDecimal refundPercent, String refundRef, LocalDateTime cancelledAt) {
        this.pnr = pnr;
        this.status = status;
        this.refund = refund;
        this.refundPercent = refundPercent;
        this.refundRef = refundRef;
        this.cancelledAt = cancelledAt;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public BigDecimal getRefund() { return refund; }
    public void setRefund(BigDecimal refund) { this.refund = refund; }

    public BigDecimal getRefundPercent() { return refundPercent; }
    public void setRefundPercent(BigDecimal refundPercent) { this.refundPercent = refundPercent; }

    public String getRefundRef() { return refundRef; }
    public void setRefundRef(String refundRef) { this.refundRef = refundRef; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
