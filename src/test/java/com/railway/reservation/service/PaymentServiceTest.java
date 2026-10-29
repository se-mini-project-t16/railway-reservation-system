package com.railway.reservation.service;

import com.railway.reservation.dto.PaymentCallbackRequestDto;
import com.railway.reservation.dto.PaymentCallbackResponseDto;
import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.PaymentRepository;
import com.railway.reservation.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User sampleUser;
    private Train sampleTrain;
    private TrainClass sampleClass;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Shakthi Raja", "shakthi@example.com", "9876543210", "hashedPassword", Role.ROLE_PASSENGER);
        sampleUser.setId(1L);

        sampleTrain = new Train("12601", "Mangalore Mail", "Chennai Central", "Mangalore Central",
                LocalTime.of(20, 15), LocalTime.of(12, 5));
        sampleTrain.setId(10L);

        sampleClass = new TrainClass("AC 3 Tier", 64, BigDecimal.valueOf(1150.00));
        sampleClass.setId(100L);

        sampleBooking = new Booking("PNR9876543", sampleUser, sampleTrain, sampleClass,
                LocalDate.now().plusDays(2), BigDecimal.valueOf(2300.00));
        sampleBooking.setId(50L);
    }

    @Test
    @DisplayName("TC-PAY-01: Initiate payment session calculates total fare and returns sandbox URL")
    void testInitiatePaymentSession_Success() {
        when(paymentRepository.findByBookingId(50L)).thenReturn(Optional.empty());
        when(paymentRepository.findByGatewayRef(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(101L);
            return p;
        });

        PaymentSessionDto session = paymentService.initiatePaymentSession(sampleBooking);

        assertNotNull(session);
        assertEquals(101L, session.getPaymentId());
        assertEquals("PNR9876543", session.getPnr());
        assertEquals(BigDecimal.valueOf(2300.00), session.getAmount());
        assertEquals("PENDING", session.getStatus());
        assertTrue(session.getGatewayRef().startsWith("TXN-"));
        assertTrue(session.getPaymentUrl().contains("/payment.html?"));
        assertTrue(session.getPaymentUrl().contains("pnr=PNR9876543"));
        assertTrue(session.getPaymentUrl().contains("amount=2300.00"));
        assertEquals("SANDBOX_MOCK", session.getGatewayProvider());

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("TC-PAY-01: Returns existing payment session idempotently if already initialized")
    void testInitiatePaymentSession_ExistingPaymentReturned() {
        Payment existing = new Payment(sampleBooking, BigDecimal.valueOf(2300.00), "TXN-EXISTING123", "PENDING");
        existing.setId(99L);
        when(paymentRepository.findByBookingId(50L)).thenReturn(Optional.of(existing));

        PaymentSessionDto session = paymentService.initiatePaymentSession(sampleBooking);

        assertNotNull(session);
        assertEquals(99L, session.getPaymentId());
        assertEquals("TXN-EXISTING123", session.getGatewayRef());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("TC-PAY-01: Initiate payment session by PNR delegates to booking entity")
    void testInitiatePaymentSession_ByPnr_Success() {
        when(bookingRepository.findByPnr("PNR9876543")).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.findByBookingId(50L)).thenReturn(Optional.empty());
        when(paymentRepository.findByGatewayRef(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(202L);
            return p;
        });

        PaymentSessionDto session = paymentService.initiatePaymentSession("PNR9876543");

        assertNotNull(session);
        assertEquals(202L, session.getPaymentId());
        assertEquals("PNR9876543", session.getPnr());
    }

    @Test
    @DisplayName("TC-PAY-01 (Negative): Fail when booking PNR does not exist")
    void testInitiatePaymentSession_ByPnr_NotFound() {
        when(bookingRepository.findByPnr("INVALIDPNR")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.initiatePaymentSession("INVALIDPNR"));
    }

    @Test
    @DisplayName("TC-PAY-01 (Negative): Fail when booking is null")
    void testInitiatePaymentSession_NullBooking() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.initiatePaymentSession((Booking) null));
    }

    @Test
    @DisplayName("TC-PAY-01: Lookup payment session by gateway reference")
    void testGetPaymentSessionByGatewayRef_Success() {
        Payment payment = new Payment(sampleBooking, BigDecimal.valueOf(2300.00), "TXN-LOOKUP1234", "PENDING");
        payment.setId(303L);

        when(paymentRepository.findByGatewayRef("TXN-LOOKUP1234")).thenReturn(Optional.of(payment));

        PaymentSessionDto session = paymentService.getPaymentSessionByGatewayRef("TXN-LOOKUP1234");

        assertNotNull(session);
        assertEquals(303L, session.getPaymentId());
        assertEquals("TXN-LOOKUP1234", session.getGatewayRef());
        assertEquals("PNR9876543", session.getPnr());
    }

    @Test
    @DisplayName("TC-PAY-01 (Negative): Lookup payment by unknown gateway reference throws 404")
    void testGetPaymentSessionByGatewayRef_NotFound() {
        when(paymentRepository.findByGatewayRef("UNKNOWN_REF")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.getPaymentSessionByGatewayRef("UNKNOWN_REF"));
    }

    @Test
    @DisplayName("TC-PAY-01: Lookup payment session by PNR")
    void testGetPaymentSessionByPnr_Success() {
        Payment payment = new Payment(sampleBooking, BigDecimal.valueOf(2300.00), "TXN-PNRMATCH1", "PENDING");
        payment.setId(404L);

        when(bookingRepository.findByPnr("PNR9876543")).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.findByBookingId(50L)).thenReturn(Optional.of(payment));

        PaymentSessionDto session = paymentService.getPaymentSessionByPnr("PNR9876543");

        assertNotNull(session);
        assertEquals("TXN-PNRMATCH1", session.getGatewayRef());
        assertEquals(BigDecimal.valueOf(2300.00), session.getAmount());
    }

    @Test
    @DisplayName("TC-PAY-01: External gateway URL configured uses external URL")
    void testInitiatePaymentSession_ExternalGatewayUrl() {
        ReflectionTestUtils.setField(paymentService, "externalGatewayUrl", "https://sandbox.payment-gateway.org/checkout");

        when(paymentRepository.findByBookingId(50L)).thenReturn(Optional.empty());
        when(paymentRepository.findByGatewayRef(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(505L);
            return p;
        });

        PaymentSessionDto session = paymentService.initiatePaymentSession(sampleBooking);

        assertNotNull(session);
        assertEquals("SANDBOX_EXTERNAL", session.getGatewayProvider());
        assertTrue(session.getPaymentUrl().startsWith("https://sandbox.payment-gateway.org/checkout?"));
        assertTrue(session.getPaymentUrl().contains("pnr=PNR9876543"));
    }

    @Test
    @DisplayName("TC-BK-01: handlePaymentCallback confirms booking and records paidAt on SUCCESS")
    void testHandlePaymentCallback_Success() {
        Payment payment = new Payment(sampleBooking, BigDecimal.valueOf(2300.00), "TXN-SUCCESS123", "PENDING");
        sampleBooking.setStatus(BookingStatus.PENDING);
        payment.setBooking(sampleBooking);

        when(paymentRepository.findByGatewayRef("TXN-SUCCESS123")).thenReturn(Optional.of(payment));

        PaymentCallbackRequestDto callbackDto = new PaymentCallbackRequestDto(
                "TXN-SUCCESS123", "PNR9876543", "SUCCESS", BigDecimal.valueOf(2300.00));

        PaymentCallbackResponseDto response = paymentService.handlePaymentCallback(callbackDto);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, response.getBookingStatus());
        assertNotNull(response.getPaidAt());
        assertEquals("PNR9876543", response.getPnr());
        assertEquals("TXN-SUCCESS123", response.getGatewayRef());

        assertEquals(BookingStatus.CONFIRMED, sampleBooking.getStatus());
        assertEquals("SUCCESS", payment.getStatus());
        assertNotNull(payment.getPaidAt());

        verify(paymentRepository, times(1)).save(payment);
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    @DisplayName("TC-PAY-02: handlePaymentCallback marks booking as FAILED and releases seats on FAILED/DECLINED")
    void testHandlePaymentCallback_Failed() {
        Payment payment = new Payment(sampleBooking, BigDecimal.valueOf(2300.00), "TXN-FAIL123", "PENDING");
        sampleBooking.setStatus(BookingStatus.PENDING);
        payment.setBooking(sampleBooking);

        when(paymentRepository.findByGatewayRef("TXN-FAIL123")).thenReturn(Optional.of(payment));

        PaymentCallbackRequestDto callbackDto = new PaymentCallbackRequestDto(
                "TXN-FAIL123", "PNR9876543", "FAILED", BigDecimal.valueOf(2300.00));

        PaymentCallbackResponseDto response = paymentService.handlePaymentCallback(callbackDto);

        assertNotNull(response);
        assertEquals("FAILED", response.getPaymentStatus());
        assertEquals(BookingStatus.FAILED, response.getBookingStatus());
        assertNull(response.getPaidAt());

        assertEquals(BookingStatus.FAILED, sampleBooking.getStatus());
        assertEquals("FAILED", payment.getStatus());

        verify(paymentRepository, times(1)).save(payment);
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    @DisplayName("TC-PAY-02: Throw ResourceNotFoundException when gatewayRef is unknown in callback")
    void testHandlePaymentCallback_GatewayRefNotFound() {
        when(paymentRepository.findByGatewayRef("UNKNOWN_TXN")).thenReturn(Optional.empty());

        PaymentCallbackRequestDto callbackDto = new PaymentCallbackRequestDto(
                "UNKNOWN_TXN", "PNR9876543", "SUCCESS", BigDecimal.valueOf(2300.00));

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.handlePaymentCallback(callbackDto));
    }

    @Test
    @DisplayName("TC-PAY-02: Throw IllegalArgumentException when PNR does not match gatewayRef (STRIDE Tampering)")
    void testHandlePaymentCallback_PnrMismatch() {
        Payment payment = new Payment(sampleBooking, BigDecimal.valueOf(2300.00), "TXN-TAMPER123", "PENDING");
        payment.setBooking(sampleBooking);

        when(paymentRepository.findByGatewayRef("TXN-TAMPER123")).thenReturn(Optional.of(payment));

        PaymentCallbackRequestDto callbackDto = new PaymentCallbackRequestDto(
                "TXN-TAMPER123", "WRONG_PNR", "SUCCESS", BigDecimal.valueOf(2300.00));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.handlePaymentCallback(callbackDto));
        assertTrue(ex.getMessage().contains("does not match the provided booking PNR"));
    }

    @Test
    @DisplayName("TC-PAY-02: Throw IllegalArgumentException when payment amount does not match")
    void testHandlePaymentCallback_AmountMismatch() {
        Payment payment = new Payment(sampleBooking, BigDecimal.valueOf(2300.00), "TXN-AMOUNT123", "PENDING");
        payment.setBooking(sampleBooking);

        when(paymentRepository.findByGatewayRef("TXN-AMOUNT123")).thenReturn(Optional.of(payment));

        PaymentCallbackRequestDto callbackDto = new PaymentCallbackRequestDto(
                "TXN-AMOUNT123", "PNR9876543", "SUCCESS", BigDecimal.valueOf(500.00)); // manipulated amount

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.handlePaymentCallback(callbackDto));
        assertTrue(ex.getMessage().contains("Payment amount mismatch"));
    }

    @Test
    @DisplayName("TC-PAY-02: Throw IllegalArgumentException when callback request is null")
    void testHandlePaymentCallback_NullRequest() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.handlePaymentCallback(null));
    }
}

