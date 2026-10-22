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

import java.math.BigDecimal;
import java.time.LocalDate;
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

        when(bookingService.getBookingByPnr("PNR1234567")).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNR1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNR1234567"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
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
}

