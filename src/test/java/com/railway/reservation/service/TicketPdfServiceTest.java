package com.railway.reservation.service;

import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.model.BookingStatus;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPdfServiceTest {
    private final TicketPdfService service = new TicketPdfService();

    @Test
    void pdfContainsTicketDetailsAndDoesNotClaimUnverifiedPayment() throws IOException {
        BookingResponseDto ticket = ticket("PENDING");

        try (var document = Loader.loadPDF(service.generate(ticket))) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("ABC2345678", "12601", "Mangalore Mail", "Chennai Central to Mangalore Central",
                    "2026-10-25", "AC 3 Tier", "Alice Smith", "AC3-1", "1150.00", "Payment status: NOT VERIFIED");
            assertThat(text).doesNotContain("Payment status: PAID", "Payment status: SUCCESS");
        }
    }

    @Test
    void pdfShowsServerVerifiedPaymentStatus() throws IOException {
        BookingResponseDto ticket = ticket("SUCCESS");

        try (var document = Loader.loadPDF(service.generate(ticket))) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Payment status: SUCCESS");
            assertThat(text).doesNotContain("Payment status: NOT VERIFIED");
        }
    }

    private BookingResponseDto ticket(String paymentStatus) {
        BookingResponseDto ticket = new BookingResponseDto();
        ticket.setPnr("ABC2345678");
        ticket.setStatus(BookingStatus.CONFIRMED);
        ticket.setPaymentStatus(paymentStatus);
        ticket.setTrainNo("12601");
        ticket.setTrainName("Mangalore Mail");
        ticket.setSourceStation("Chennai Central");
        ticket.setDestinationStation("Mangalore Central");
        ticket.setJourneyDate(LocalDate.of(2026, 10, 25));
        ticket.setClassName("AC 3 Tier");
        ticket.setPassengers(List.of(new PassengerDto("Alice Smith", 28, "Female", "AC3-1")));
        ticket.setTotalFare(new BigDecimal("1150.00"));
        return ticket;
    }
}
