package com.railway.reservation.controller;

import com.railway.reservation.dto.BookingDtos.BookingView;
import com.railway.reservation.dto.BookingDtos.PaymentRequest;
import com.railway.reservation.dto.BookingDtos.PaymentResult;
import com.railway.reservation.service.rail.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.TreeMap;

/** Simulated payment for a held booking (RRS-F-008, RRS-F-009). */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/banks")
    public Map<String, String> banks() {
        return new TreeMap<>(PaymentService.BANKS);
    }

    @PostMapping("/{pnr}")
    public PaymentResult pay(@PathVariable String pnr, @Valid @RequestBody PaymentRequest request, Authentication auth) {
        return paymentService.pay(pnr, CurrentUser.id(auth), request);
    }

    @PostMapping("/{pnr}/abandon")
    public BookingView abandon(@PathVariable String pnr, Authentication auth) {
        return paymentService.abandon(pnr, CurrentUser.id(auth));
    }
}
