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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.railway.reservation.service.JwtService;
import com.railway.reservation.model.User;
import com.railway.reservation.model.Role;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private BookingService bookingService;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("TC-BK-01: POST /api/v1/bookings returns 201 Created on valid booking")
    void testBookTicket_Success() throws Exception {
        BookingRequestDto request = new BookingRequestDto(
                1L, 10L, 100L, LocalDate.now().plusDays(3),
                List.of(new PassengerDto("John Doe", 30, "Male"))
        );

        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNR1234567");
        response.setStatus(BookingStatus.CONFIRMED);
        response.setTotalFare(BigDecimal.valueOf(1150.00));
        response.setPassengers(List.of(new PassengerDto("John Doe", 30, "Male", "AC3-1")));

        when(bookingService.createBooking(any(BookingRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pnr").value("PNR1234567"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
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
        response.setPaymentStatus("NOT_VERIFIED");
        response.setTrainNo("12601");
        response.setTrainName("Mangalore Mail");
        response.setSourceStation("Chennai Central");
        response.setDestinationStation("Mangalore Central");
        response.setJourneyDate(LocalDate.of(2026, 10, 25));
        response.setClassName("AC 3 Tier");
        response.setTotalFare(BigDecimal.valueOf(1150));
        response.setPassengers(List.of(new PassengerDto("John Doe", 30, "Male", "AC3-1")));

        when(bookingService.getTicketByPnr("PNR1234567", 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNR1234567")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNR1234567"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_VERIFIED"))
                .andExpect(jsonPath("$.trainNo").value("12601"))
                .andExpect(jsonPath("$.trainName").value("Mangalore Mail"))
                .andExpect(jsonPath("$.sourceStation").value("Chennai Central"))
                .andExpect(jsonPath("$.destinationStation").value("Mangalore Central"))
                .andExpect(jsonPath("$.journeyDate").value("2026-10-25"))
                .andExpect(jsonPath("$.className").value("AC 3 Tier"))
                .andExpect(jsonPath("$.passengers[0].seatNo").value("AC3-1"))
                .andExpect(jsonPath("$.totalFare").value(1150));
    }

    @Test
    @DisplayName("TC-PNR-01: GET /api/v1/bookings/{pnr} returns 404 Not Found for non-existent PNR")
    void testGetBookingStatus_NotFound() throws Exception {
        when(bookingService.getTicketByPnr("NONEXISTENT", 1L))
                .thenThrow(new ResourceNotFoundException("Booking not found with PNR: NONEXISTENT"));

        mockMvc.perform(get("/api/v1/bookings/NONEXISTENT")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void bookingTicketRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/PNR1234567"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ticketLookupUsesVerifiedJwtIdentityAndHidesOtherOwnersPnr() throws Exception {
        User otherPassenger = new User("Other", "other@example.com", "1234567890", "hash", Role.ROLE_PASSENGER);
        otherPassenger.setId(2L);
        String token = jwtService.generateToken(otherPassenger);
        when(bookingService.getTicketByPnr("OWNED-BY-SOMEONE-ELSE", 2L))
                .thenThrow(new ResourceNotFoundException("Booking not found."));

        mockMvc.perform(get("/api/v1/bookings/OWNED-BY-SOMEONE-ELSE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found."));

        verify(bookingService).getTicketByPnr("OWNED-BY-SOMEONE-ELSE", 2L);
        verify(bookingService, never()).getBookingByPnr("OWNED-BY-SOMEONE-ELSE");
    }

    @Test
    void downloadsTicketPdfWithAttachmentHeaders() throws Exception {
        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNR1234567");
        response.setStatus(BookingStatus.CONFIRMED);
        response.setPaymentStatus("NOT_VERIFIED");
        response.setTrainNo("12601");
        response.setTrainName("Mangalore Mail");
        response.setSourceStation("Chennai Central");
        response.setDestinationStation("Mangalore Central");
        response.setClassName("AC 3 Tier");
        response.setJourneyDate(LocalDate.now().plusDays(2));
        response.setTotalFare(BigDecimal.valueOf(1150));
        response.setPassengers(List.of(new PassengerDto("John Doe", 30, "Male", "AC3-1")));
        when(bookingService.getTicketByPnr("PNR1234567", 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket.pdf")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ticket-PNR1234567.pdf\""));
    }
}

