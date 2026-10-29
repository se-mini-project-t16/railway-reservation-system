package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private BookingService bookingService;

    @Test
    @DisplayName("TC-BK-01: POST /api/v1/bookings returns 201 Created on valid booking")
    void testBookTicket_Success() throws Exception {
        BookingRequestDto request = new BookingRequestDto(
                1L, 10L, 100L, LocalDate.now().plusDays(3),
                List.of(new PassengerDto("John Doe", 30, "Male"))
        );

        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNR1234567");
        response.setStatus(BookingStatus.PENDING);
        response.setTotalFare(BigDecimal.valueOf(1150.00));
        response.setPassengers(List.of(new PassengerDto("John Doe", 30, "Male", "AC3-1")));
        response.setPaymentUrl("/payment.html?pnr=PNR1234567&gatewayRef=TXN-123456");
        response.setGatewayRef("TXN-123456");
        response.setPaymentStatus("PENDING");

        when(bookingService.createBooking(any(BookingRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pnr").value("PNR1234567"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").value("/payment.html?pnr=PNR1234567&gatewayRef=TXN-123456"))
                .andExpect(jsonPath("$.gatewayRef").value("TXN-123456"))
                .andExpect(jsonPath("$.passengers[0].seatNo").value("AC3-1"));
    }

    @Test
    @DisplayName("TC-BK-02: POST /api/v1/bookings returns 400 Bad Request when passenger name is blank")
    void testBookTicket_BlankPassengerName() throws Exception {
        BookingRequestDto request = new BookingRequestDto(
                1L, 10L, 100L, LocalDate.now().plusDays(3),
                List.of(new PassengerDto("", 30, "Male")) // blank name
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("TC-BK-02: POST /api/v1/bookings returns 400 Bad Request when passenger age is invalid")
    void testBookTicket_InvalidPassengerAge() throws Exception {
        BookingRequestDto request = new BookingRequestDto(
                1L, 10L, 100L, LocalDate.now().plusDays(3),
                List.of(new PassengerDto("John Doe", 0, "Male")) // age 0 invalid
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("TC-BK-02: POST /api/v1/bookings returns 400 Bad Request when passenger list is empty")
    void testBookTicket_EmptyPassengerList() throws Exception {
        BookingRequestDto request = new BookingRequestDto(
                1L, 10L, 100L, LocalDate.now().plusDays(3),
                Collections.emptyList()
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("TC-PNR-01: GET /api/v1/bookings/{pnr} returns 200 OK with booking details")
    void testGetBookingStatus_Success() throws Exception {
        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNR1234567");
        response.setStatus(BookingStatus.CONFIRMED);

        when(bookingService.getBookingByPnr("PNR1234567")).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNR1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNR1234567"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("TC-PNR-01: GET /api/v1/bookings/{pnr} returns complete train, passenger, and fare details")
    void testGetBookingStatus_AllRequiredFields() throws Exception {
        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNR9876543");
        response.setStatus(BookingStatus.CONFIRMED);
        response.setTrainId(10L);
        response.setTrainNo("12601");
        response.setTrainName("Mangalore Mail");
        response.setSourceStation("Chennai Central");
        response.setDestinationStation("Mangalore Central");
        response.setDepartureTime(LocalTime.of(20, 15));
        response.setArrivalTime(LocalTime.of(12, 5));
        response.setClassName("AC 3 Tier");
        response.setFarePerSeat(BigDecimal.valueOf(1150.00));
        response.setTotalFare(BigDecimal.valueOf(2300.00));
        response.setJourneyDate(LocalDate.now().plusDays(5));
        response.setPassengers(List.of(
                new PassengerDto("Alice Smith", 28, "Female", "AC3-1"),
                new PassengerDto("Bob Smith", 32, "Male", "AC3-2")
        ));

        when(bookingService.getBookingByPnr("PNR9876543")).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNR9876543"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNR9876543"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.trainNo").value("12601"))
                .andExpect(jsonPath("$.trainName").value("Mangalore Mail"))
                .andExpect(jsonPath("$.sourceStation").value("Chennai Central"))
                .andExpect(jsonPath("$.destinationStation").value("Mangalore Central"))
                .andExpect(jsonPath("$.className").value("AC 3 Tier"))
                .andExpect(jsonPath("$.farePerSeat").value(1150.00))
                .andExpect(jsonPath("$.totalFare").value(2300.00))
                .andExpect(jsonPath("$.passengers.length()").value(2))
                .andExpect(jsonPath("$.passengers[0].name").value("Alice Smith"))
                .andExpect(jsonPath("$.passengers[0].seatNo").value("AC3-1"))
                .andExpect(jsonPath("$.passengers[1].name").value("Bob Smith"))
                .andExpect(jsonPath("$.passengers[1].seatNo").value("AC3-2"));
    }

    @Test
    @DisplayName("TC-PNR-01: GET /api/v1/bookings/{pnr} returns correct status for CANCELLED booking")
    void testGetBookingStatus_CancelledStatus() throws Exception {
        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNRCANCEL1");
        response.setStatus(BookingStatus.CANCELLED);

        when(bookingService.getBookingByPnr("PNRCANCEL1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNRCANCEL1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNRCANCEL1"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("TC-PNR-01: GET /api/v1/bookings/{pnr} returns correct status for PENDING booking")
    void testGetBookingStatus_PendingStatus() throws Exception {
        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNRPEND001");
        response.setStatus(BookingStatus.PENDING);

        when(bookingService.getBookingByPnr("PNRPEND001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNRPEND001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNRPEND001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("TC-PNR-01: GET /api/v1/bookings/{pnr} returns correct status for FAILED booking")
    void testGetBookingStatus_FailedStatus() throws Exception {
        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNRFAIL001");
        response.setStatus(BookingStatus.FAILED);

        when(bookingService.getBookingByPnr("PNRFAIL001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNRFAIL001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNRFAIL001"))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @DisplayName("TC-PNR-01: GET /api/v1/bookings/{pnr} returns 404 Not Found for non-existent PNR")
    void testGetBookingStatus_NotFound() throws Exception {
        when(bookingService.getBookingByPnr("NONEXISTENT"))
                .thenThrow(new ResourceNotFoundException("Booking not found with PNR: NONEXISTENT"));

        mockMvc.perform(get("/api/v1/bookings/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin' OR '1'='1",
            "UNION SELECT 1,2,3",
            "javascript:alert(1)",
            "onload=alert(1)"
    })
    @DisplayName("TC-SEC-03: GET /api/v1/bookings/{pnr} rejects malicious SQL injection and XSS with 400 Bad Request [RRS-SR-003]")
    void testGetBookingStatus_SecuritySanitization_RejectsMaliciousPnr(String maliciousPnr) throws Exception {
        mockMvc.perform(get("/api/v1/bookings/{pnr}", maliciousPnr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
