package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.dto.PaymentInitiateRequestDto;
import com.railway.reservation.dto.PaymentSessionDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

    @Test
    @DisplayName("TC-PAY-01: POST /api/v1/payments/initiate returns 201 Created with PaymentSessionDto")
    void testInitiatePayment_Success() throws Exception {
        PaymentInitiateRequestDto request = new PaymentInitiateRequestDto("PNR1234567");

        PaymentSessionDto response = new PaymentSessionDto(
                1L, "TXN-TESTGATEWAY1", "PNR1234567", BigDecimal.valueOf(1150.00),
                "PENDING", "/payment.html?pnr=PNR1234567&gatewayRef=TXN-TESTGATEWAY1",
                "SANDBOX_MOCK", "12601", "Mangalore Mail", "Chennai Central", "Mangalore Central",
                LocalDate.now().plusDays(2), 1, LocalDateTime.now()
        );

        when(paymentService.initiatePaymentSession(eq("PNR1234567"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/initiate")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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

        when(paymentService.getPaymentSessionByGatewayRef(eq("TXN-REF12345"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/TXN-REF12345"))
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

        mockMvc.perform(get("/api/v1/payments/UNKNOWN_REF"))
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

        when(paymentService.getPaymentSessionByPnr(eq("PNR1122334"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/booking/PNR1122334"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNR1122334"))
                .andExpect(jsonPath("$.gatewayRef").value("TXN-REF99999"))
                .andExpect(jsonPath("$.amount").value(550.00));
    }
}

