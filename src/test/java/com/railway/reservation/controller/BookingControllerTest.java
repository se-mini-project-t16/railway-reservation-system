package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.dto.PnrStatusResponseDto;
import com.railway.reservation.exception.PaymentNotVerifiedException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.model.BookingStatus;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.service.BookingService;
import com.railway.reservation.service.JwtService;
import com.railway.reservation.service.TicketPdfService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    private static final String PAYMENT_NOT_VERIFIED = "Payment has not been verified for this booking.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private TicketPdfService ticketPdfService;

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
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")))))
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
                List.of(new PassengerDto("", 30, "Male"))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")))))
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
                List.of(new PassengerDto("John Doe", 0, "Male"))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")))))
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
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("TC-BK-04: POST /api/v1/bookings without authentication returns 401 Unauthorized [RRS-SR-006]")
    void testBookTicket_Unauthenticated_Returns401() throws Exception {
        BookingRequestDto request = new BookingRequestDto(
                1L, 10L, 100L, LocalDate.now().plusDays(3),
                List.of(new PassengerDto("John Doe", 30, "Male"))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BK-05: POST /api/v1/bookings ignores spoofed client userId and derives from authenticated principal [RRS-SR-006]")
    void testBookTicket_IgnoresClientUserId_DerivesFromPrincipal() throws Exception {
        BookingRequestDto request = new BookingRequestDto(
                999L, 10L, 100L, LocalDate.now().plusDays(3),
                List.of(new PassengerDto("John Doe", 30, "Male"))
        );

        BookingResponseDto response = new BookingResponseDto();
        response.setPnr("PNR1234567");
        response.setStatus(BookingStatus.PENDING);
        when(bookingService.createBooking(any(BookingRequestDto.class))).thenAnswer(invocation -> {
            BookingRequestDto req = invocation.getArgument(0);
            assertEquals(1L, req.getUserId());
            return response;
        });

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
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

    @Test
    @DisplayName("TC-BK-03: Full ticket endpoint requires authentication")
    void testFullTicket_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BK-03: Full ticket endpoint rejects anonymous HEAD requests")
    void testFullTicket_RejectsAnonymousHead() throws Exception {
        mockMvc.perform(head("/api/v1/bookings/PNR1234567/ticket"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BK-03: Verified owner receives the full ticket JSON")
    void testFullTicket_OwnerVerifiedPayment() throws Exception {
        BookingResponseDto response = fullTicket("PNR1234567");
        when(bookingService.getTicketByPnr("PNR1234567", 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.pnr").value("PNR1234567"))
                .andExpect(jsonPath("$.passengers[0].name").value("John Doe"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("TC-BK-03: Another user's full ticket is inaccessible")
    void testFullTicket_OtherOwnerForbidden() throws Exception {
        when(bookingService.getTicketByPnr("OTHER-PNR", 2L))
                .thenThrow(new ResourceNotFoundException("Booking not found."));

        mockMvc.perform(get("/api/v1/bookings/OTHER-PNR/ticket")
                        .with(authentication(new UsernamePasswordAuthenticationToken(2L, null, List.of()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found."));
    }

    @Test
    @DisplayName("TC-BK-03: Administrator receives the full ticket JSON after verified payment")
    void testFullTicket_AdminVerifiedPayment() throws Exception {
        BookingResponseDto response = fullTicket("ADMIN-VIEW-PNR");
        when(bookingService.getTicketByPnrForAdmin("ADMIN-VIEW-PNR")).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/ADMIN-VIEW-PNR/ticket")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                999L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("ADMIN-VIEW-PNR"));

        verify(bookingService).getTicketByPnrForAdmin("ADMIN-VIEW-PNR");
        verify(bookingService, never()).getTicketByPnr(eq("ADMIN-VIEW-PNR"), anyLong());
    }

    @Test
    @DisplayName("TC-BK-03: Unresolvable authenticated identity is rejected for full ticket")
    void testFullTicket_UnresolvedOwnerFailsClosed() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "ghost@example.com", null, List.of()))))
                .andExpect(status().isUnauthorized());

        verify(bookingService, never()).getTicketByPnr(eq("PNR1234567"), anyLong());
        verify(bookingService, never()).getTicketByPnrForAdmin("PNR1234567");
    }

    @Test
    @DisplayName("TC-BK-03: Pending payment blocks full ticket even with payment=success")
    void testFullTicket_PendingPaymentBlockedAndQueryParameterIgnored() throws Exception {
        when(bookingService.getTicketByPnr("PNR1234567", 1L))
                .thenThrow(new PaymentNotVerifiedException(PAYMENT_NOT_VERIFIED));

        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket")
                        .param("payment", "success")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(PAYMENT_NOT_VERIFIED));
    }

    @Test
    @DisplayName("TC-BK-03: HEAD full ticket is also blocked for pending payment")
    void testFullTicket_PendingPaymentHeadBlocked() throws Exception {
        when(bookingService.getTicketByPnr("PNR1234567", 1L))
                .thenThrow(new PaymentNotVerifiedException(PAYMENT_NOT_VERIFIED));

        mockMvc.perform(head("/api/v1/bookings/PNR1234567/ticket")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-BK-03: JWT identity is used for full-ticket ownership")
    void testFullTicket_UsesVerifiedJwtIdentityAndHidesOtherOwnersPnr() throws Exception {
        User otherPassenger = new User("Other", "other@example.com", "1234567890", "hash", Role.ROLE_PASSENGER);
        otherPassenger.setId(2L);
        String token = jwtService.generateToken(otherPassenger);
        when(bookingService.getTicketByPnr("OWNED-BY-SOMEONE-ELSE", 2L))
                .thenThrow(new ResourceNotFoundException("Booking not found."));

        mockMvc.perform(get("/api/v1/bookings/OWNED-BY-SOMEONE-ELSE/ticket")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found."));

        verify(bookingService).getTicketByPnr("OWNED-BY-SOMEONE-ELSE", 2L);
    }

    @Test
    @DisplayName("TC-BK-03: Ticket PDF download requires authentication")
    void testDownloadTicketPdf_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket.pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BK-03: Ticket PDF download rejects anonymous HEAD requests")
    void testDownloadTicketPdf_RejectsAnonymousHead() throws Exception {
        mockMvc.perform(head("/api/v1/bookings/PNR1234567/ticket.pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BK-03: Verified owner downloads PDF with attachment headers")
    void testDownloadsTicketPdfWithAttachmentHeaders() throws Exception {
        BookingResponseDto response = fullTicket("PNR1234567");
        when(bookingService.getTicketByPnr("PNR1234567", 1L)).thenReturn(response);
        when(ticketPdfService.generate(response)).thenReturn("%PDF-1.4 test bytes".getBytes());

        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket.pdf")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ticket-PNR1234567.pdf\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().bytes("%PDF-1.4 test bytes".getBytes()));
    }

    @Test
    @DisplayName("TC-BK-03: Pending payment blocks PDF even with payment=success")
    void testDownloadTicketPdf_PendingPaymentBlocked() throws Exception {
        when(bookingService.getTicketByPnr("PNR1234567", 1L))
                .thenThrow(new PaymentNotVerifiedException(PAYMENT_NOT_VERIFIED));

        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket.pdf")
                        .param("payment", "success")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(PAYMENT_NOT_VERIFIED));

        verify(ticketPdfService, never()).generate(any(BookingResponseDto.class));
    }

    @Test
    @DisplayName("TC-BK-03: Failed payment blocks PDF")
    void testDownloadTicketPdf_FailedPaymentBlocked() throws Exception {
        when(bookingService.getTicketByPnr("PNR1234567", 1L))
                .thenThrow(new PaymentNotVerifiedException(PAYMENT_NOT_VERIFIED));

        mockMvc.perform(get("/api/v1/bookings/PNR1234567/ticket.pdf")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("TC-BK-03: HEAD PDF is also blocked for pending payment")
    void testDownloadTicketPdf_PendingPaymentHeadBlocked() throws Exception {
        when(bookingService.getTicketByPnr("PNR1234567", 1L))
                .thenThrow(new PaymentNotVerifiedException(PAYMENT_NOT_VERIFIED));

        mockMvc.perform(head("/api/v1/bookings/PNR1234567/ticket.pdf")
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))))
                .andExpect(status().isConflict());

        verify(ticketPdfService, never()).generate(any(BookingResponseDto.class));
    }

    @Test
    @DisplayName("TC-BK-03: Ticket PDF download returns 404 for another owner's PNR")
    void testDownloadTicketPdf_ForbiddenForOtherOwner() throws Exception {
        when(bookingService.getTicketByPnr("OTHER-PNR", 2L))
                .thenThrow(new ResourceNotFoundException("Booking not found."));

        mockMvc.perform(get("/api/v1/bookings/OTHER-PNR/ticket.pdf")
                        .with(authentication(new UsernamePasswordAuthenticationToken(2L, null, List.of()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found."));

        verify(bookingService).getTicketByPnr("OTHER-PNR", 2L);
    }

    @Test
    @DisplayName("TC-BK-03: Administrator downloads a verified ticket PDF")
    void testDownloadTicketPdf_AdminVerifiedPayment() throws Exception {
        BookingResponseDto response = fullTicket("ADMIN-VIEW-PNR");
        when(bookingService.getTicketByPnrForAdmin("ADMIN-VIEW-PNR")).thenReturn(response);
        when(ticketPdfService.generate(response)).thenReturn("%PDF-1.4 admin ticket".getBytes());

        mockMvc.perform(get("/api/v1/bookings/ADMIN-VIEW-PNR/ticket.pdf")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                999L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ticket-ADMIN-VIEW-PNR.pdf\""))
                .andExpect(content().bytes("%PDF-1.4 admin ticket".getBytes()));
    }

    @Test
    @DisplayName("TC-BK-03: Administrator is also blocked for unverified payment")
    void testDownloadTicketPdf_AdminPendingPaymentBlocked() throws Exception {
        when(bookingService.getTicketByPnrForAdmin("ADMIN-VIEW-PNR"))
                .thenThrow(new PaymentNotVerifiedException(PAYMENT_NOT_VERIFIED));

        mockMvc.perform(get("/api/v1/bookings/ADMIN-VIEW-PNR/ticket.pdf")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                999L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(PAYMENT_NOT_VERIFIED));

        verify(ticketPdfService, never()).generate(any(BookingResponseDto.class));
    }

    private PnrStatusResponseDto publicStatus(String pnr) {
        return new PnrStatusResponseDto(
                pnr,
                BookingStatus.CONFIRMED,
                "12601",
                "Mangalore Mail",
                "Chennai Central",
                "Mangalore Central",
                LocalDate.of(2026, 10, 25),
                "AC 3 Tier",
                BigDecimal.valueOf(1150),
                List.of("AC3-1"));
    }

    private BookingResponseDto fullTicket(String pnr) {
        BookingResponseDto response = new BookingResponseDto();
        response.setPnr(pnr);
        response.setStatus(BookingStatus.CONFIRMED);
        response.setPaymentStatus("SUCCESS");
        response.setPaymentUrl("/payment.html?pnr=" + pnr + "&gatewayRef=TXN-TEST");
        response.setGatewayRef("TXN-TEST");
        response.setTrainNo("12601");
        response.setTrainName("Mangalore Mail");
        response.setSourceStation("Chennai Central");
        response.setDestinationStation("Mangalore Central");
        response.setJourneyDate(LocalDate.of(2026, 10, 25));
        response.setClassName("AC 3 Tier");
        response.setTotalFare(BigDecimal.valueOf(1150));
        response.setPassengers(List.of(new PassengerDto("John Doe", 30, "Male", "AC3-1")));
        return response;
    }

    @Test
    @DisplayName("TC-HIS-01: GET /api/v1/bookings returns 200 OK with chronological booking list for authenticated user [RRS-F-014]")
    void testGetBookingHistory_SuccessWithJwt() throws Exception {
        User passenger = new User("Sujeet", "sujeet@example.com", "9876543210", "hash", Role.ROLE_PASSENGER);
        passenger.setId(42L);
        String token = jwtService.generateToken(passenger);

        BookingResponseDto b1 = new BookingResponseDto();
        b1.setPnr("HISTPNR001");
        b1.setStatus(BookingStatus.CONFIRMED);
        b1.setTrainNo("12601");
        b1.setTrainName("Mangalore Mail");
        b1.setSourceStation("Chennai Central");
        b1.setDestinationStation("Mangalore Central");
        b1.setJourneyDate(LocalDate.now().plusDays(5));
        b1.setTotalFare(BigDecimal.valueOf(1150.00));
        b1.setUpcoming(true);
        b1.setCancellable(true);
        b1.setTicketUrl("/ticket.html?pnr=HISTPNR001");
        b1.setCancelUrl("/api/v1/bookings/HISTPNR001/cancel");
        b1.setPassengers(List.of(new PassengerDto("Sujeet", 22, "Male", "AC3-1")));

        BookingResponseDto b2 = new BookingResponseDto();
        b2.setPnr("HISTPNR002");
        b2.setStatus(BookingStatus.CONFIRMED);
        b2.setTrainNo("12602");
        b2.setTrainName("Chennai Mail");
        b2.setSourceStation("Mangalore Central");
        b2.setDestinationStation("Chennai Central");
        b2.setJourneyDate(LocalDate.now().minusDays(3));
        b2.setTotalFare(BigDecimal.valueOf(1150.00));
        b2.setUpcoming(false);
        b2.setCancellable(false);
        b2.setTicketUrl("/ticket.html?pnr=HISTPNR002");

        when(bookingService.getBookingHistory(42L)).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pnr").value("HISTPNR001"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].upcoming").value(true))
                .andExpect(jsonPath("$[0].cancellable").value(true))
                .andExpect(jsonPath("$[0].ticketUrl").value("/ticket.html?pnr=HISTPNR001"))
                .andExpect(jsonPath("$[0].cancelUrl").value("/api/v1/bookings/HISTPNR001/cancel"))
                .andExpect(jsonPath("$[0].passengers[0].seatNo").value("AC3-1"))
                .andExpect(jsonPath("$[1].pnr").value("HISTPNR002"))
                .andExpect(jsonPath("$[1].upcoming").value(false))
                .andExpect(jsonPath("$[1].cancellable").value(false));

        verify(bookingService).getBookingHistory(42L);
    }

    @Test
    @DisplayName("TC-HIS-01: GET /api/v1/bookings/history alias endpoint routes to booking history [RRS-F-014]")
    void testGetBookingHistory_HistoryPathAlias() throws Exception {
        User passenger = new User("Sujeet", "sujeet@example.com", "9876543210", "hash", Role.ROLE_PASSENGER);
        passenger.setId(42L);
        String token = jwtService.generateToken(passenger);

        when(bookingService.getBookingHistory(42L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bookings/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(bookingService).getBookingHistory(42L);
    }

    @Test
    @DisplayName("TC-HIS-02: GET /api/v1/bookings returns 200 OK with empty array when user has no bookings [RRS-F-014]")
    void testGetBookingHistory_EmptyList() throws Exception {
        User passenger = new User("Sujeet", "sujeet@example.com", "9876543210", "hash", Role.ROLE_PASSENGER);
        passenger.setId(42L);
        String token = jwtService.generateToken(passenger);

        when(bookingService.getBookingHistory(42L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("TC-HIS-03: GET /api/v1/bookings without authentication returns 401 Unauthorized [RRS-F-014, RRS-SR-005]")
    void testGetBookingHistory_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized());

        verify(bookingService, never()).getBookingHistory(anyLong());
    }

    @Test
    @DisplayName("TC-HIS-04: User isolation - caller only receives their own bookings [RRS-F-014]")
    void testGetBookingHistory_UserIsolation() throws Exception {
        User user1 = new User("User One", "user1@example.com", "1111111111", "hash", Role.ROLE_PASSENGER);
        user1.setId(101L);
        String token1 = jwtService.generateToken(user1);

        when(bookingService.getBookingHistory(101L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        verify(bookingService).getBookingHistory(101L);
        verify(bookingService, never()).getBookingHistory(eq(102L));
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
