package com.railway.reservation.controller;

import com.railway.reservation.dto.PaymentInitiateRequestDto;
import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Endpoint to initiate a payment session for a booking (RRS-F-008, TC-PAY-01).
     * Returns payment session details with gateway reference and redirect URL.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentSessionDto> initiatePayment(@Valid @RequestBody PaymentInitiateRequestDto request) {
        PaymentSessionDto session = paymentService.initiatePaymentSession(request.getPnr());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Endpoint to retrieve a payment session by its gateway reference.
     */
    @GetMapping("/{gatewayRef}")
    public ResponseEntity<PaymentSessionDto> getPaymentByGatewayRef(@PathVariable String gatewayRef) {
        PaymentSessionDto session = paymentService.getPaymentSessionByGatewayRef(gatewayRef);
        return ResponseEntity.ok(session);
    }

    /**
     * Endpoint to retrieve a payment session by booking PNR.
     */
    @GetMapping("/booking/{pnr}")
    public ResponseEntity<PaymentSessionDto> getPaymentByPnr(@PathVariable String pnr) {
        PaymentSessionDto session = paymentService.getPaymentSessionByPnr(pnr);
        return ResponseEntity.ok(session);
    }
}

