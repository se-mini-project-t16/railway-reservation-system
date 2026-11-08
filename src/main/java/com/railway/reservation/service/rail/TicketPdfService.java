package com.railway.reservation.service.rail;

import com.railway.reservation.dto.BookingDtos.BookingView;
import com.railway.reservation.dto.BookingDtos.PassengerView;
import com.railway.reservation.dto.BookingDtos.PaymentView;
import com.railway.reservation.exception.ApiException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Electronic reservation slip as a one-page A4 PDF (RRS-F-010). */
@Service
public class TicketPdfService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final Color INK = new Color(18, 22, 33);
    private static final Color MUTED = new Color(95, 102, 118);
    private static final Color RULE = new Color(196, 201, 210);
    private static final Color COBALT = new Color(32, 74, 214);

    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDType1Font mono = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);

    public byte[] render(BookingView v) {
        if (!"BOOKED".equals(v.status()) && !"PARTIALLY_CANCELLED".equals(v.status()) && !"CANCELLED".equals(v.status())) {
            throw ApiException.conflict("NOT_PAID", "The ticket is issued once payment is complete.");
        }
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float w = page.getMediaBox().getWidth();
            float left = 48, right = w - 48;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 790;
                text(cs, bold, 18, left, y, INK, "Electronic Reservation Slip");
                text(cs, regular, 9, left, y - 16, MUTED, "Railway Reservation System. Simulated booking: not valid for travel.");
                text(cs, regular, 9, right - width(regular, 9, "PNR"), y + 2, MUTED, "PNR");
                text(cs, mono, 20, right - width(mono, 20, v.pnr()), y - 18, COBALT, v.pnr());
                y -= 34;
                rule(cs, left, right, y, 2f, INK);

                y -= 26;
                text(cs, bold, 13, left, y, INK, v.train().number() + "  " + v.train().name());
                String cls = v.classLabel() + " (" + v.classCode() + "), " + v.quotaLabel() + " quota";
                text(cs, regular, 10, right - width(regular, 10, cls), y, INK, cls);

                y -= 34;
                float mid = left + (right - left) / 2;
                text(cs, regular, 9, left, y + 12, MUTED, "From");
                text(cs, bold, 12, left, y - 4, INK, v.from().name() + " (" + v.from().code() + ")");
                text(cs, regular, 10, left, y - 19, INK, "Departs " + v.from().time() + ", " + v.from().date().format(DAY));
                text(cs, regular, 9, mid, y + 12, MUTED, "To");
                text(cs, bold, 12, mid, y - 4, INK, v.to().name() + " (" + v.to().code() + ")");
                text(cs, regular, 10, mid, y - 19, INK, "Arrives " + v.to().time() + ", " + v.to().date().format(DAY));
                y -= 38;
                String trip = v.distanceKm() + " km, " + (v.durationMinutes() / 60) + " h " + (v.durationMinutes() % 60)
                        + " min. Booked by " + v.bookedBy() + ", mobile " + v.contactMobile() + ".";
                text(cs, regular, 9, left, y, MUTED, trip);

                y -= 20;
                rule(cs, left, right, y, 0.6f, RULE);
                y -= 18;
                float[] cols = {left, left + 24, left + 200, left + 236, left + 270, left + 380};
                String[] heads = {"#", "Name", "Age", "Sex", "Booking status", "Current status"};
                for (int i = 0; i < heads.length; i++) text(cs, bold, 9, cols[i], y, INK, heads[i]);
                y -= 6;
                rule(cs, left, right, y, 1f, INK);
                for (PassengerView p : v.passengers()) {
                    y -= 16;
                    text(cs, regular, 10, cols[0], y, INK, String.valueOf(p.serial()));
                    text(cs, regular, 10, cols[1], y, INK, clip(p.name(), 28));
                    text(cs, regular, 10, cols[2], y, INK, String.valueOf(p.age()));
                    text(cs, regular, 10, cols[3], y, INK, p.gender().substring(0, 1));
                    text(cs, mono, 10, cols[4], y, INK, p.bookingStatus().text());
                    text(cs, mono, 10, cols[5], y, "CAN".equals(p.currentStatus().status()) ? MUTED : INK,
                            p.currentStatus().text());
                    y -= 5;
                    rule(cs, left, right, y, 0.4f, RULE);
                }

                y -= 28;
                text(cs, bold, 11, left, y, INK, "Fare");
                y -= 4;
                List<String[]> lines = new java.util.ArrayList<>();
                lines.add(new String[]{"Base fare", rs(v.fare().base())});
                lines.add(new String[]{"Reservation charge", rs(v.fare().reservation())});
                if (v.fare().superfast() > 0) lines.add(new String[]{"Superfast charge", rs(v.fare().superfast())});
                if (v.fare().tatkal() > 0) lines.add(new String[]{"Tatkal charge", rs(v.fare().tatkal())});
                if (v.fare().catering() > 0) lines.add(new String[]{"Catering", rs(v.fare().catering())});
                if (v.fare().gst() > 0) lines.add(new String[]{"GST (5%)", rs(v.fare().gst())});
                float fareRight = left + 260;
                for (String[] l : lines) {
                    y -= 15;
                    text(cs, regular, 10, left, y, INK, l[0]);
                    text(cs, regular, 10, fareRight - width(regular, 10, l[1]), y, INK, l[1]);
                }
                y -= 8;
                rule(cs, left, fareRight, y, 1f, INK);
                y -= 15;
                text(cs, bold, 11, left, y, INK, "Total paid");
                text(cs, bold, 11, fareRight - width(bold, 11, rs(v.fare().total())), y, INK, rs(v.fare().total()));
                if (v.refundTotal() > 0) {
                    y -= 15;
                    text(cs, regular, 10, left, y, INK, "Refunded");
                    text(cs, regular, 10, fareRight - width(regular, 10, rs(v.refundTotal())), y, INK, rs(v.refundTotal()));
                }

                PaymentView paid = v.payments().stream().filter(p -> "SUCCESS".equals(p.status())).findFirst().orElse(null);
                float px = left + 300;
                float py = y + 15 * (lines.size() + 1) + 8;
                text(cs, bold, 11, px, py, INK, "Payment");
                if (paid != null) {
                    text(cs, regular, 10, px, py - 19, INK, "Payment ID " + paid.txnRef());
                    text(cs, regular, 10, px, py - 34, INK, paid.instrument());
                    text(cs, regular, 10, px, py - 49, INK, "Paid " + paid.at().format(STAMP));
                }
                text(cs, regular, 10, px, py - 64, INK, "Status: " + v.statusLabel());

                y -= 40;
                rule(cs, left, right, y, 0.6f, RULE);
                y -= 16;
                String[] notes = {
                        "Carry a government photo ID that matches one traveller's name on this ticket.",
                        "RAC travellers share a side berth. Waitlisted e-tickets still waitlisted when the chart is",
                        "prepared are cancelled automatically and refunded, less a clerkage charge.",
                        "Chart preparation: " + v.chartAt().format(STAMP) + "."};
                for (String n : notes) {
                    text(cs, regular, 9, left, y, MUTED, n);
                    y -= 13;
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not render ticket PDF", e);
        }
    }

    private static String rs(int amount) { return "Rs " + String.format(Locale.ENGLISH, "%,d", amount); }

    private static String clip(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "."; }

    private static float width(PDType1Font font, float size, String s) throws IOException {
        return font.getStringWidth(s) / 1000 * size;
    }

    private static void text(PDPageContentStream cs, PDType1Font font, float size, float x, float y, Color color, String s)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(s);
        cs.endText();
    }

    private static void rule(PDPageContentStream cs, float x1, float x2, float y, float weight, Color color) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(weight);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }
}
