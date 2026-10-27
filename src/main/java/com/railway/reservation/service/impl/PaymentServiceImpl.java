package com.railway.reservation.service.impl;

import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.Payment;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.PaymentRepository;
import com.railway.reservation.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String REF_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REF_LENGTH = 12;
    private final SecureRandom random = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Value("${app.payment.gateway-url:}")
    private String externalGatewayUrl;

    @Value("${app.payment.mock-fallback:true}")
    private boolean mockFallback;

    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public PaymentSessionDto initiatePaymentSession(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking must not be null to initiate payment session");
        }

        // Return existing payment session if already created
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment == null) {
            String gatewayRef = generateUniqueGatewayRef();
            BigDecimal amount = booking.getTotalFare();

            payment = new Payment(booking, amount, gatewayRef, "PENDING");
            payment = paymentRepository.save(payment);
            booking.setPayment(payment);

            logger.info("Initiated payment session for booking PNR={} with gatewayRef={} amount={} [RRS-F-008, TC-PAY-01]",
                    booking.getPnr(), gatewayRef, amount);
        }

        return mapToDto(payment, booking);
    }

    @Override
    @Transactional
    public PaymentSessionDto initiatePaymentSession(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));
        return initiatePaymentSession(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSessionDto getPaymentSessionByGatewayRef(String gatewayRef) {
        Payment payment = paymentRepository.findByGatewayRef(gatewayRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment session not found for gateway reference: " + gatewayRef));
        return mapToDto(payment, payment.getBooking());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSessionDto getPaymentSessionByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No payment session found for PNR: " + pnr));

        return mapToDto(payment, booking);
    }

    private String generateUniqueGatewayRef() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder("TXN-");
            for (int i = 0; i < REF_LENGTH; i++) {
                sb.append(REF_CHARACTERS.charAt(random.nextInt(REF_CHARACTERS.length())));
            }
            ref = sb.toString();
        } while (paymentRepository.findByGatewayRef(ref).isPresent());
        return ref;
    }

    private PaymentSessionDto mapToDto(Payment payment, Booking booking) {
        String paymentUrl = resolvePaymentUrl(booking, payment);
        String provider = (externalGatewayUrl != null && !externalGatewayUrl.isBlank()) ? "SANDBOX_EXTERNAL" : "SANDBOX_MOCK";

        return new PaymentSessionDto(
                payment.getId(),
                payment.getGatewayRef(),
                booking.getPnr(),
                payment.getAmount(),
                payment.getStatus(),
                paymentUrl,
                provider,
                booking.getTrain().getTrainNo(),
                booking.getTrain().getName(),
                booking.getTrain().getSourceStation(),
                booking.getTrain().getDestinationStation(),
                booking.getJourneyDate(),
                booking.getPassengers() != null ? booking.getPassengers().size() : 0,
                booking.getCreatedAt() != null ? booking.getCreatedAt() : LocalDateTime.now()
        );
    }

    private String resolvePaymentUrl(Booking booking, Payment payment) {
        // Format amount with 2 decimal places for consistent currency representation (TC-PAY-01)
        String formattedAmount = (payment.getAmount() != null)
                ? payment.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                : "0.00";

        // Construct query parameters
        String query = String.format("pnr=%s&gatewayRef=%s&amount=%s&trainNo=%s",
                URLEncoder.encode(booking.getPnr(), StandardCharsets.UTF_8),
                URLEncoder.encode(payment.getGatewayRef(), StandardCharsets.UTF_8),
                URLEncoder.encode(formattedAmount, StandardCharsets.UTF_8),
                URLEncoder.encode(booking.getTrain().getTrainNo(), StandardCharsets.UTF_8));

        // Fallback to internal sandbox gateway if external URL is not set or mock fallback is enabled
        if (externalGatewayUrl != null && !externalGatewayUrl.isBlank()) {
            try {
                URI uri = URI.create(externalGatewayUrl);
                String separator = (uri.getQuery() == null || uri.getQuery().isEmpty()) ? "?" : "&";
                return externalGatewayUrl + separator + query;
            } catch (Exception e) {
                logger.warn("Configured external gateway URL is invalid ({}), falling back to mock sandbox: {}",
                        externalGatewayUrl, e.getMessage());
            }
        }

        return "/payment.html?" + query;
    }
}

