package com.railway.reservation.service.notify;

import com.railway.reservation.dto.BookingDtos.BookingView;
import com.railway.reservation.dto.BookingDtos.PassengerView;
import com.railway.reservation.dto.BookingDtos.PaymentView;
import com.railway.reservation.dto.TrainDtos.EndPoint;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.service.mail.EmailSender;
import com.railway.reservation.service.mail.EmailSender.Attachment;
import com.railway.reservation.service.mail.EmailSender.Email;
import com.railway.reservation.service.rail.BookingMapper;
import com.railway.reservation.service.rail.TicketPdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Emails the passenger the e-ticket when a booking is paid, and a notice when travellers are cancelled
 * (RRS-F-010, RRS-F-012). Mail goes out only after the change is committed, on a background thread, so a
 * slow or failing mail server never delays or undoes a booking; a failure is logged and nothing else.
 */
@Component
public class BookingEmails {

    private static final Logger log = LoggerFactory.getLogger(BookingEmails.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);

    private final BookingRepository bookingRepository;
    private final BookingMapper mapper;
    private final TicketPdfService ticketPdfService;
    private final EmailSender emailSender;
    private final TransactionTemplate tx;

    public BookingEmails(BookingRepository bookingRepository, BookingMapper mapper, TicketPdfService ticketPdfService,
                         EmailSender emailSender, TransactionTemplate tx) {
        this.bookingRepository = bookingRepository;
        this.mapper = mapper;
        this.ticketPdfService = ticketPdfService;
        this.emailSender = emailSender;
        this.tx = tx;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaid(BookingEvents.Paid event) {
        try {
            BookingView v = load(event.pnr());
            if (v == null) return;
            byte[] pdf = ticketPdfService.render(v);
            emailSender.send(new Email(v.contactEmail(), bookedSubject(v), bookedText(v), bookedHtml(v),
                    List.of(new Attachment("e-ticket-" + v.pnr() + ".pdf", "application/pdf", pdf))));
        } catch (RuntimeException e) {
            log.warn("Booking confirmation email not sent pnr={}: {}", event.pnr(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelled(BookingEvents.Cancelled event) {
        try {
            BookingView v = load(event.pnr());
            if (v == null) return;
            emailSender.send(new Email(v.contactEmail(), cancelledSubject(v, event), cancelledText(v, event),
                    cancelledHtml(v, event), List.of()));
        } catch (RuntimeException e) {
            log.warn("Cancellation email not sent pnr={}: {}", event.pnr(), e.getMessage());
        }
    }

    private BookingView load(String pnr) {
        return tx.execute(status -> bookingRepository.findByPnr(pnr).map(mapper::toView).orElse(null));
    }

    // ------------------------------------------------------------------ booking confirmed

    static String bookedSubject(BookingView v) {
        return "Booking confirmed: PNR " + v.pnr() + ", " + v.train().number() + " " + v.from().code() + " to " + v.to().code()
                + " on " + v.from().date().format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH));
    }

    static List<String[]> journeyFacts(BookingView v) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"PNR", v.pnr()});
        rows.add(new String[]{"Train", v.train().number() + " " + v.train().name()});
        rows.add(new String[]{"From", endPoint(v.from())});
        rows.add(new String[]{"To", endPoint(v.to())});
        rows.add(new String[]{"Class", v.classLabel() + ", " + v.quotaLabel()});
        return rows;
    }

    static String bookedText(BookingView v) {
        StringBuilder sb = new StringBuilder("Hello " + v.bookedBy() + ",\n\nYour ticket is booked. The e-ticket is attached as a PDF.\n\n");
        journeyFacts(v).forEach(r -> sb.append(r[0]).append(": ").append(r[1]).append('\n'));
        sb.append("\nTravellers:\n");
        for (PassengerView p : v.passengers()) sb.append("  ").append(traveller(p)).append('\n');
        sb.append('\n').append(paidLine(v)).append("\n\n")
          .append("RAC and waitlisted travellers move up automatically as others cancel. Check My trips for the latest status.\n")
          .append("Carry a photo ID matching one traveller on this ticket.\n");
        return sb.toString();
    }

    static String bookedHtml(BookingView v) {
        List<String[]> rows = journeyFacts(v);
        for (PassengerView p : v.passengers()) rows.add(new String[]{"Traveller " + p.serial(), traveller(p)});
        rows.add(new String[]{"Paid", money(v.fare().total())});
        PaymentView paid = paid(v);
        if (paid != null) rows.add(new String[]{"Payment ID", paid.txnRef() + " (" + paid.instrument() + ")"});
        return EmailLayout.page("Your ticket is booked",
                EmailLayout.para("Hello " + v.bookedBy() + ", your e-ticket is attached as a PDF.")
                        + EmailLayout.facts(rows)
                        + EmailLayout.small("RAC and waitlisted travellers move up automatically as others cancel. "
                        + "Check My trips for the latest status. Carry a photo ID matching one traveller on this ticket."));
    }

    // ------------------------------------------------------------------ travellers cancelled

    static String cancelledSubject(BookingView v, BookingEvents.Cancelled e) {
        return "Cancelled: PNR " + v.pnr() + ", " + e.travellers() + (e.travellers() == 1 ? " traveller" : " travellers")
                + ", refund " + money(e.refund());
    }

    static String why(BookingEvents.Cancelled e) {
        return switch (e.reason()) {
            case "ADMIN" -> "Railway staff cancelled ";
            case "AUTO_WAITLIST" -> "The chart is prepared and the ticket was still waitlisted, so we cancelled ";
            default -> "You cancelled ";
        } + e.travellers() + (e.travellers() == 1 ? " traveller" : " travellers") + " on PNR " + e.pnr() + ".";
    }

    static String refundLine(BookingEvents.Cancelled e) {
        if (e.refund() <= 0) return "No refund is due under the refund rules.";
        return money(e.refund()) + " is being refunded to the card, UPI account, bank or wallet you paid with"
                + (e.refundRef() == null ? "" : " (refund " + e.refundRef() + ")")
                + ". Banks usually show it within 5 to 7 working days.";
    }

    static String cancelledText(BookingView v, BookingEvents.Cancelled e) {
        StringBuilder sb = new StringBuilder("Hello " + v.bookedBy() + ",\n\n" + why(e) + "\n\n" + refundLine(e) + "\n\n");
        journeyFacts(v).forEach(r -> sb.append(r[0]).append(": ").append(r[1]).append('\n'));
        sb.append("\nTravellers now:\n");
        for (PassengerView p : v.passengers()) sb.append("  ").append(traveller(p)).append('\n');
        return sb.toString();
    }

    static String cancelledHtml(BookingView v, BookingEvents.Cancelled e) {
        List<String[]> rows = journeyFacts(v);
        for (PassengerView p : v.passengers()) rows.add(new String[]{"Traveller " + p.serial(), traveller(p)});
        rows.add(new String[]{"Refund", money(e.refund()) + (e.refundRef() == null ? "" : " (" + e.refundRef() + ")")});
        return EmailLayout.page(v.status().equals("CANCELLED") ? "Your ticket is cancelled" : "Travellers cancelled",
                EmailLayout.para("Hello " + v.bookedBy() + ". " + why(e)) + EmailLayout.para(refundLine(e)) + EmailLayout.facts(rows));
    }

    // ------------------------------------------------------------------ pieces

    static String endPoint(EndPoint p) {
        return p.code() + " " + p.name() + ", " + p.time() + " on " + p.date().format(DAY);
    }

    static String traveller(PassengerView p) {
        return p.name() + ", " + p.age() + ": " + p.currentStatus().text();
    }

    static PaymentView paid(BookingView v) {
        return v.payments().stream().filter(p -> "SUCCESS".equals(p.status())).findFirst().orElse(null);
    }

    static String paidLine(BookingView v) {
        PaymentView p = paid(v);
        return "Paid " + money(v.fare().total()) + (p == null ? "" : " (payment " + p.txnRef() + ", " + p.instrument() + ")");
    }

    static String money(int rupees) {
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN"));
        return "Rs " + f.format(rupees);
    }
}
