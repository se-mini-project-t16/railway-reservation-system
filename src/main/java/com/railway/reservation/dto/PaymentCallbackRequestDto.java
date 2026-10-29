package com.railway.reservation.dto;

import com.railway.reservation.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentCallbackRequestDto {

    @NotBlank(message = "Gateway reference is required")
    @Size(min = 5, max = 100, message = "Gateway reference must be between 5 and 100 characters")
    @SafeText(message = "Gateway reference contains invalid characters")
    private String gatewayRef;

    @NotBlank(message = "PNR is required")
    @Size(min = 8, max = 20, message = "PNR must be between 8 and 20 characters")
    @SafeText(message = "PNR contains invalid characters")
    private String pnr;

    @NotBlank(message = "Payment status is required")
    @Size(max = 30, message = "Payment status too long")
    @SafeText(message = "Payment status contains invalid characters")
    private String status;

    private BigDecimal amount;

    private String paymentMethod;

    public PaymentCallbackRequestDto() {}

    public PaymentCallbackRequestDto(String gatewayRef, String pnr, String status) {
        this.gatewayRef = gatewayRef;
        this.pnr = pnr;
        this.status = status;
    }

    public PaymentCallbackRequestDto(String gatewayRef, String pnr, String status, BigDecimal amount) {
        this.gatewayRef = gatewayRef;
        this.pnr = pnr;
        this.status = status;
        this.amount = amount;
    }

    public String getGatewayRef() { return gatewayRef; }
    public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}

