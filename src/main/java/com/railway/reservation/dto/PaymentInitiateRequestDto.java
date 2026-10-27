package com.railway.reservation.dto;

import com.railway.reservation.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PaymentInitiateRequestDto {

    @NotBlank(message = "PNR is required")
    @Size(min = 8, max = 20, message = "PNR must be between 8 and 20 characters")
    @SafeText(message = "PNR contains invalid characters")
    private String pnr;

    public PaymentInitiateRequestDto() {}

    public PaymentInitiateRequestDto(String pnr) {
        this.pnr = pnr;
    }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
}

