package com.railway.reservation.service.impl;

import com.railway.reservation.dto.PaymentCallbackRequestDto;
import com.railway.reservation.dto.PaymentCallbackResponseDto;
import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
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
import java.math.RoundingMode;
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

    @Override
    @Transactional
    public PaymentCallbackResponseDto handlePaymentCallback(PaymentCallbackRequestDto callbackRequest) {
        if (callbackRequest == null) {
            throw new IllegalArgumentException("Payment callback payload must not be null");
        }

        String gatewayRef = callbackRequest.getGatewayRef().trim();
        String pnr = callbackRequest.getPnr().trim();
        String status = callbackRequest.getStatus().trim();

        // 1. Locate payment session by gatewayRef
        Payment payment = paymentRepository.findByGatewayRef(gatewayRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for gateway reference: " + gatewayRef));

        Booking booking = payment.getBooking();
        if (booking == null) {
            throw new ResourceNotFoundException("No booking associated with payment reference: " + gatewayRef);
        }

        // 2. Prevent tampering: Ensure booking PNR matches (STRIDE Tampering)
        if (!booking.getPnr().equalsIgnoreCase(pnr)) {
            logger.error("AUDIT SECURITY ALERT: Payment gateway reference {} PNR mismatch! Expected {}, Received {} [STRIDE Tampering]",
                    gatewayRef, booking.getPnr(), pnr);
            throw new IllegalArgumentException("Gateway reference does not match the provided booking PNR");
        }

        // 3. Prevent amount mismatch / price tampering if amount is provided
        if (callbackRequest.getAmount() != null) {
            BigDecimal expectedAmount = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal receivedAmount = callbackRequest.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (expectedAmount.compareTo(receivedAmount) != 0) {
                logger.error("AUDIT SECURITY ALERT: Payment amount tampering! Expected {}, Received {} for gatewayRef={}",
                        expectedAmount, receivedAmount, gatewayRef);
                throw new IllegalArgumentException("Payment amount mismatch: Expected " + expectedAmount + ", received " + receivedAmount);
            }
        }

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status);

        if (isSuccess) {
            // RRS-F-009, TC-BK-01: Update booking to CONFIRMED, record gateway_ref and paid_at
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            // STRIDE Repudiation Mitigation: Log verified payment with user id and timestamp
            logger.info("AUDIT: Payment SUCCESS verified. gatewayRef={}, pnr={}, amount={}, paidAt={}, userId={} [RRS-F-009, TC-BK-01]",
                    gatewayRef, booking.getPnr(), payment.getAmount(), payment.getPaidAt(), booking.getUser().getId());

            return new PaymentCallbackResponseDto(
                    gatewayRef,
                    booking.getPnr(),
                    "SUCCESS",
                    BookingStatus.CONFIRMED,
                    payment.getAmount(),
                    payment.getPaidAt(),
                    "Payment verified successfully. Booking is now CONFIRMED."
            );
        } else {
            // RRS-F-009, TC-PAY-02: Update booking to FAILED, immediately release held seats
            payment.setStatus("FAILED");
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.FAILED);
            bookingRepository.save(booking);

            // STRIDE Repudiation Mitigation: Log failed payment with user id and timestamp
            logger.warn("AUDIT: Payment FAILED/DECLINED. gatewayRef={}, pnr={}, amount={}, timestamp={}, userId={}. Held seats released. [RRS-F-009, TC-PAY-02]",
                    gatewayRef, booking.getPnr(), payment.getAmount(), LocalDateTime.now(), booking.getUser().getId());

            return new PaymentCallbackResponseDto(
                    gatewayRef,
                    booking.getPnr(),
                    "FAILED",
                    BookingStatus.FAILED,
                    payment.getAmount(),
                    null,
                    "Payment failed or was declined. Booking is marked as FAILED and held seats have been released."
            );
        }
    }

    @Override
    @Transactional
    public String initiateRefund(Booking booking, BigDecimal amount) {
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for PNR: " + booking.getPnr()));

        BigDecimal refundAmount = amount.setScale(2, RoundingMode.HALF_UP);
        payment.setRefundAmount(refundAmount);

        if (refundAmount.signum() == 0) {
            payment.setStatus("CANCELLED_NO_REFUND");
            paymentRepository.save(payment);
            logger.info("AUDIT: No refund due under cancellation rules. gatewayRef={}, pnr={}, userId={} [RRS-F-013]",
                    payment.getGatewayRef(), booking.getPnr(), booking.getUser().getId());
            return null;
        }

        // Sandbox gateway: the refund is recorded as initiated with its own reference
        String refundRef = generateUniqueRefundRef();
        payment.setRefundRef(refundRef);
        payment.setStatus("REFUND_INITIATED");
        paymentRepository.save(payment);

        logger.info("AUDIT: Refund INITIATED. refundRef={}, gatewayRef={}, pnr={}, amount={}, timestamp={}, userId={} [RRS-F-013, TC-CAN-02]",
                refundRef, payment.getGatewayRef(), booking.getPnr(), refundAmount, LocalDateTime.now(), booking.getUser().getId());
        return refundRef;
    }

    private String generateUniqueGatewayRef() {
        String ref;
        do {
            ref = randomRef("TXN-");
        } while (paymentRepository.findByGatewayRef(ref).isPresent());
        return ref;
    }

    private String generateUniqueRefundRef() {
        String ref;
        do {
            ref = randomRef("RFD-");
        } while (paymentRepository.existsByRefundRef(ref));
        return ref;
    }

    private String randomRef(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < REF_LENGTH; i++) {
            sb.append(REF_CHARACTERS.charAt(random.nextInt(REF_CHARACTERS.length())));
        }
        return sb.toString();
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

