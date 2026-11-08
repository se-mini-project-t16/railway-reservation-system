package com.railway.reservation.controller;

import com.railway.reservation.dto.BookingDtos.BookingView;
import com.railway.reservation.dto.BookingDtos.CheckoutOrder;
import com.railway.reservation.dto.BookingDtos.PaymentConfirmation;
import com.railway.reservation.dto.BookingDtos.PaymentFailure;
import com.railway.reservation.dto.BookingDtos.PaymentResult;
import com.railway.reservation.service.rail.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Paying for a held booking through Razorpay Checkout (RRS-F-008, RRS-F-009). */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** The Razorpay order for this booking, created on first use. */
    @PostMapping("/{pnr}/order")
    public CheckoutOrder order(@PathVariable String pnr, Authentication auth) {
        return paymentService.checkout(pnr, CurrentUser.id(auth));
    }

    /** Checkout's success callback, verified on the server before the ticket is booked. */
    @PostMapping("/{pnr}/confirm")
    public PaymentResult confirm(@PathVariable String pnr, @Valid @RequestBody PaymentConfirmation body, Authentication auth) {
        return paymentService.confirm(pnr, CurrentUser.id(auth), body);
    }

    @PostMapping("/{pnr}/failure")
    public BookingView failure(@PathVariable String pnr, @Valid @RequestBody PaymentFailure body, Authentication auth) {
        return paymentService.reportFailure(pnr, CurrentUser.id(auth), body.paymentId());
    }

    /** Checks Razorpay for a payment made earlier whose result never came back (for example, a closed tab). */
    @PostMapping("/{pnr}/sync")
    public BookingView sync(@PathVariable String pnr, Authentication auth) {
        return paymentService.sync(pnr, CurrentUser.id(auth));
    }

    @PostMapping("/{pnr}/abandon")
    public BookingView abandon(@PathVariable String pnr, Authentication auth) {
        return paymentService.abandon(pnr, CurrentUser.id(auth));
    }
}
