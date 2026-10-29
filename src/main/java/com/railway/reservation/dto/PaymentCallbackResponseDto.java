package com.railway.reservation.dto;

import com.railway.reservation.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentCallbackResponseDto {

    private String gatewayRef;
    private String pnr;
    private String paymentStatus;
    private BookingStatus bookingStatus;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private String message;

    public PaymentCallbackResponseDto() {}

    public PaymentCallbackResponseDto(
            String gatewayRef,
            String pnr,
            String paymentStatus,
            BookingStatus bookingStatus,
            BigDecimal amount,
            LocalDateTime paidAt,
            String message) {
        this.gatewayRef = gatewayRef;
        this.pnr = pnr;
        this.paymentStatus = paymentStatus;
        this.bookingStatus = bookingStatus;
        this.amount = amount;
        this.paidAt = paidAt;
        this.message = message;
    }

    public String getGatewayRef() { return gatewayRef; }
    public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public BookingStatus getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

