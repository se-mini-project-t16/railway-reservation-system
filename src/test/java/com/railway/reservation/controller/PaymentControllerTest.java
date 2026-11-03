package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.dto.PaymentCallbackRequestDto;
import com.railway.reservation.dto.PaymentCallbackResponseDto;
import com.railway.reservation.dto.PaymentInitiateRequestDto;
import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("TC-PAY-01: POST /api/v1/payments/initiate returns 201 Created with PaymentSessionDto")
    void testInitiatePayment_Success() throws Exception {
        PaymentInitiateRequestDto request = new PaymentInitiateRequestDto("PNR1234567");

        User owner = new User("Owner", "owner@example.com", "9876543210", "pass", Role.ROLE_PASSENGER);
        owner.setId(1L);
        Booking booking = new Booking();
        booking.setPnr("PNR1234567");
        booking.setUser(owner);
        when(bookingRepository.findByPnr(eq("PNR1234567"))).thenReturn(Optional.of(booking));

        PaymentSessionDto response = new PaymentSessionDto(
                1L, "TXN-TESTGATEWAY1", "PNR1234567", BigDecimal.valueOf(1150.00),
                "PENDING", "/payment.html?pnr=PNR1234567&gatewayRef=TXN-TESTGATEWAY1",
                "SANDBOX_MOCK", "12601", "Mangalore Mail", "Chennai Central", "Mangalore Central",
                LocalDate.now().plusDays(2), 1, LocalDateTime.now()
        );

        when(paymentService.initiatePaymentSession(eq("PNR1234567"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pnr").value("PNR1234567"))
                .andExpect(jsonPath("$.gatewayRef").value("TXN-TESTGATEWAY1"))
                .andExpect(jsonPath("$.amount").value(1150.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").value("/payment.html?pnr=PNR1234567&gatewayRef=TXN-TESTGATEWAY1"));
    }

    @Test
    @DisplayName("TC-PAY-01 (Negative): POST /api/v1/payments/initiate returns 400 when PNR is blank")
    void testInitiatePayment_BlankPnr() throws Exception {
        PaymentInitiateRequestDto request = new PaymentInitiateRequestDto("");

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-PAY-01 (Security): POST /api/v1/payments/initiate returns 401 Unauthorized when unauthenticated [RRS-SR-006]")
    void testInitiatePayment_Unauthenticated_Returns401() throws Exception {
        PaymentInitiateRequestDto request = new PaymentInitiateRequestDto("PNR1234567");

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-PAY-01 (Security): POST /api/v1/payments/initiate returns 403 Forbidden on cross-user booking [RRS-SR-006]")
    void testInitiatePayment_CrossUser_Returns403() throws Exception {
        PaymentInitiateRequestDto request = new PaymentInitiateRequestDto("PNR1234567");

        User owner = new User("Owner", "owner@example.com", "9876543210", "pass", Role.ROLE_PASSENGER);
        owner.setId(1L);
        Booking booking = new Booking();
        booking.setPnr("PNR1234567");
        booking.setUser(owner);
        when(bookingRepository.findByPnr(eq("PNR1234567"))).thenReturn(Optional.of(booking));

        // Authenticate as User 2 (not owner)
        mockMvc.perform(post("/api/v1/payments/initiate")
                        .with(authentication(new UsernamePasswordAuthenticationToken(2L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-PAY-01: GET /api/v1/payments/{gatewayRef} returns 200 OK with session")
    void testGetPaymentByGatewayRef_Success() throws Exception {
        PaymentSessionDto response = new PaymentSessionDto(
                1L, "TXN-REF12345", "PNR9998887", BigDecimal.valueOf(2300.00),
                "PENDING", "/payment.html?pnr=PNR9998887",
                "SANDBOX_MOCK", "12601", "Mangalore Mail", "Chennai Central", "Mangalore Central",
                LocalDate.now().plusDays(2), 2, LocalDateTime.now()
        );

        User owner = new User("Owner", "owner@example.com", "9876543210", "pass", Role.ROLE_PASSENGER);
        owner.setId(1L);
        Booking booking = new Booking();
        booking.setPnr("PNR9998887");
        booking.setUser(owner);
        when(bookingRepository.findByPnr(eq("PNR9998887"))).thenReturn(Optional.of(booking));
        when(paymentService.getPaymentSessionByGatewayRef(eq("TXN-REF12345"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/TXN-REF12345")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayRef").value("TXN-REF12345"))
                .andExpect(jsonPath("$.pnr").value("PNR9998887"))
                .andExpect(jsonPath("$.amount").value(2300.00));
    }

    @Test
    @DisplayName("TC-PAY-01 (Negative): GET /api/v1/payments/{gatewayRef} returns 404 when not found")
    void testGetPaymentByGatewayRef_NotFound() throws Exception {
        when(paymentService.getPaymentSessionByGatewayRef(eq("UNKNOWN_REF")))
                .thenThrow(new ResourceNotFoundException("Payment session not found"));

        mockMvc.perform(get("/api/v1/payments/UNKNOWN_REF")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER"))))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-PAY-01: GET /api/v1/payments/booking/{pnr} returns 200 OK")
    void testGetPaymentByPnr_Success() throws Exception {
        PaymentSessionDto response = new PaymentSessionDto(
                1L, "TXN-REF99999", "PNR1122334", BigDecimal.valueOf(550.00),
                "PENDING", "/payment.html?pnr=PNR1122334",
                "SANDBOX_MOCK", "12601", "Mangalore Mail", "Chennai Central", "Mangalore Central",
                LocalDate.now().plusDays(1), 1, LocalDateTime.now()
        );

        User owner = new User("Owner", "owner@example.com", "9876543210", "pass", Role.ROLE_PASSENGER);
        owner.setId(1L);
        Booking booking = new Booking();
        booking.setPnr("PNR1122334");
        booking.setUser(owner);
        when(bookingRepository.findByPnr(eq("PNR1122334"))).thenReturn(Optional.of(booking));
        when(paymentService.getPaymentSessionByPnr(eq("PNR1122334"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/booking/PNR1122334")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNR1122334"))
                .andExpect(jsonPath("$.gatewayRef").value("TXN-REF99999"))
                .andExpect(jsonPath("$.amount").value(550.00));
    }

    @Test
    @DisplayName("TC-PAY-01 (Security): GET /api/v1/payments/booking/{pnr} returns 403 Forbidden on cross-user booking [RRS-SR-006]")
    void testGetPaymentByPnr_CrossUser_Returns403() throws Exception {
        User owner = new User("Owner", "owner@example.com", "9876543210", "pass", Role.ROLE_PASSENGER);
        owner.setId(1L);
        Booking booking = new Booking();
        booking.setPnr("PNR1122334");
        booking.setUser(owner);
        when(bookingRepository.findByPnr(eq("PNR1122334"))).thenReturn(Optional.of(booking));

        mockMvc.perform(get("/api/v1/payments/booking/PNR1122334")
                        .with(authentication(new UsernamePasswordAuthenticationToken(2L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-BK-01: POST /api/v1/payments/callback with SUCCESS confirms booking")
    void testHandleCallback_Success() throws Exception {
        PaymentCallbackRequestDto request = new PaymentCallbackRequestDto(
                "TXN-CALL123", "PNR1234567", "SUCCESS", BigDecimal.valueOf(1150.00));

        PaymentCallbackResponseDto response = new PaymentCallbackResponseDto(
                "TXN-CALL123", "PNR1234567", "SUCCESS", BookingStatus.CONFIRMED,
                BigDecimal.valueOf(1150.00), LocalDateTime.now(), "Payment verified successfully. Booking is now CONFIRMED."
        );

        when(paymentService.handlePaymentCallback(any(PaymentCallbackRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayRef").value("TXN-CALL123"))
                .andExpect(jsonPath("$.pnr").value("PNR1234567"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.amount").value(1150.00));
    }

    @Test
    @DisplayName("RRS-SR-002: POST /api/v1/payments/callback accepts HMAC signature")
    void testHandleCallback_WithSignature_Success() throws Exception {
        PaymentCallbackRequestDto request = new PaymentCallbackRequestDto(
                "TXN-CALL123", "PNR1234567", "SUCCESS", BigDecimal.valueOf(1150.00), "mock_valid_hmac_hex_sig");

        PaymentCallbackResponseDto response = new PaymentCallbackResponseDto(
                "TXN-CALL123", "PNR1234567", "SUCCESS", BookingStatus.CONFIRMED,
                BigDecimal.valueOf(1150.00), LocalDateTime.now(), "Payment verified successfully."
        );

        when(paymentService.handlePaymentCallback(any(PaymentCallbackRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));
    }

    @Test
    @DisplayName("TC-PAY-02: POST /api/v1/payments/callback with FAILED updates booking to FAILED")
    void testHandleCallback_Failed() throws Exception {
        PaymentCallbackRequestDto request = new PaymentCallbackRequestDto(
                "TXN-CALL123", "PNR1234567", "FAILED", BigDecimal.valueOf(1150.00));

        PaymentCallbackResponseDto response = new PaymentCallbackResponseDto(
                "TXN-CALL123", "PNR1234567", "FAILED", BookingStatus.FAILED,
                BigDecimal.valueOf(1150.00), null, "Payment failed. Booking marked as FAILED."
        );

        when(paymentService.handlePaymentCallback(any(PaymentCallbackRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.bookingStatus").value("FAILED"));
    }

    @Test
    @DisplayName("TC-PAY-02: POST /api/v1/payments/callback returns 400 Bad Request on blank parameters")
    void testHandleCallback_BlankParameters() throws Exception {
        PaymentCallbackRequestDto request = new PaymentCallbackRequestDto("", "", "");

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-BK-01: POST /api/v1/payments/verify with SUCCESS confirms booking")
    void testVerifyPayment_Success() throws Exception {
        PaymentCallbackRequestDto request = new PaymentCallbackRequestDto(
                "TXN-VERIFY123", "PNR7654321", "SUCCESS", BigDecimal.valueOf(2300.00));

        PaymentCallbackResponseDto response = new PaymentCallbackResponseDto(
                "TXN-VERIFY123", "PNR7654321", "SUCCESS", BookingStatus.CONFIRMED,
                BigDecimal.valueOf(2300.00), LocalDateTime.now(), "Payment verified successfully."
        );

        when(paymentService.handlePaymentCallback(any(PaymentCallbackRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayRef").value("TXN-VERIFY123"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));
    }
}

