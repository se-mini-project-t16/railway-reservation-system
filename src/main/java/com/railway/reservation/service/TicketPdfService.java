package com.railway.reservation.service;

import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.PassengerDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class TicketPdfService {
    private static final float LEFT = 54;
    private static final float LINE_HEIGHT = 18;

    public byte[] generate(BookingResponseDto ticket) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (TicketWriter writer = new TicketWriter(document, regular, bold)) {
                writer.write(bold, 18, "RAILWAY E-TICKET");
                writer.write(regular, 11, "PNR: " + ticket.getPnr());
                writer.write(regular, 11, "Booking status: " + ticket.getStatus());
                writer.write(regular, 11, "Payment status: " + paymentLabel(ticket.getPaymentStatus()));
                writer.write(bold, 12, "Journey details");
                writer.write(regular, 11, "Train: " + ticket.getTrainNo() + " - " + ticket.getTrainName());
                writer.write(regular, 11, "Route: " + ticket.getSourceStation() + " to " + ticket.getDestinationStation());
                writer.write(regular, 11, "Date: " + ticket.getJourneyDate().format(DateTimeFormatter.ISO_DATE));
                writer.write(regular, 11, "Class: " + ticket.getClassName());
                writer.write(bold, 12, "Passengers");
                for (int i = 0; i < ticket.getPassengers().size(); i++) {
                    PassengerDto passenger = ticket.getPassengers().get(i);
                    writer.write(regular, 11, String.format("%d. %s | Age %d | %s | Seat %s",
                            i + 1, passenger.getName(), passenger.getAge(), passenger.getGender(), passenger.getSeatNo()));
                }
                writer.write(bold, 12, "Total fare: " + ticket.getTotalFare());
                writer.write(regular, 9, "Payment is not shown as successful unless confirmed by the payment system.");
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate ticket PDF.", ex);
        }
    }

    private String paymentLabel(String status) {
        return status == null || status.isBlank() || "PENDING".equalsIgnoreCase(status)
                || "NOT_VERIFIED".equalsIgnoreCase(status)
                ? "NOT VERIFIED" : status;
    }

    private String asPdfText(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private final class TicketWriter implements AutoCloseable {
        private final PDDocument document;
        private final PDType1Font regular;
        private final PDType1Font bold;
        private PDPageContentStream stream;
        private float y;

        private TicketWriter(PDDocument document, PDType1Font regular, PDType1Font bold) throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            newPage();
        }

        private void write(PDType1Font font, int size, String value) throws IOException {
            if (y < 54) newPage();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(LEFT, y);
            stream.showText(asPdfText(value));
            stream.endText();
            y -= LINE_HEIGHT;
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            document.addPage(new PDPage());
            stream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages() - 1));
            y = 740;
            if (document.getNumberOfPages() > 1) write(bold, 14, "RAILWAY E-TICKET - CONTINUED");
        }

        @Override
        public void close() throws IOException {
            if (stream != null) stream.close();
        }
    }
}
