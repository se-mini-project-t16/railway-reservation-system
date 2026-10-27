package com.railway.reservation.service;

import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.model.Booking;

public interface PaymentService {

    /**
     * Initiates a payment session for a newly created booking (RRS-F-008, TC-PAY-01).
     * Calculates total fare and creates a payment transaction with gateway reference and redirect URL.
     * Supports sandbox payment gateway and mock payment fallback.
     */
    PaymentSessionDto initiatePaymentSession(Booking booking);

    /**
     * Initiates or retrieves an existing payment session given a booking PNR.
     */
    PaymentSessionDto initiatePaymentSession(String pnr);

    /**
     * Retrieves an existing payment session by its unique gateway reference.
     */
    PaymentSessionDto getPaymentSessionByGatewayRef(String gatewayRef);

    /**
     * Retrieves an existing payment session by its booking PNR.
     */
    PaymentSessionDto getPaymentSessionByPnr(String pnr);
}

