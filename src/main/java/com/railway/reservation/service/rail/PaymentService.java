package com.railway.reservation.service.rail;

import com.railway.reservation.dto.BookingDtos.BookingView;
import com.railway.reservation.dto.BookingDtos.PaymentRequest;
import com.railway.reservation.dto.BookingDtos.PaymentResult;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Simulated payment gateway (RRS-F-008, RRS-F-009). No money moves and no
 * card number, CVV or banking credential is stored: only a masked
 * description of the instrument. Paying an already paid booking returns the
 * original receipt, so a double click never charges twice.
 *
 * <p>For testing declines: card 4000 0000 0000 0002 and any UPI ID starting
 * with "decline" are refused.</p>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern UPI = Pattern.compile("^[A-Za-z0-9._-]{2,64}@[A-Za-z]{2,32}$");
    private static final String DECLINED_CARD = "4000000000000002";
    public static final Map<String, String> BANKS = Map.of(
            "SBI", "State Bank of India", "HDFC", "HDFC Bank", "ICICI", "ICICI Bank", "AXIS", "Axis Bank",
            "KOTAK", "Kotak Mahindra Bank", "PNB", "Punjab National Bank", "BOB", "Bank of Baroda",
            "CANARA", "Canara Bank", "UNION", "Union Bank of India", "IDFC", "IDFC FIRST Bank");

    private final BookingService bookingService;
    private final SeatOperations seatOperations;
    private final InventoryService inventoryService;
    private final PaymentRepository paymentRepository;
    private final BookingMapper mapper;

    public PaymentService(BookingService bookingService, SeatOperations seatOperations,
                          InventoryService inventoryService, PaymentRepository paymentRepository, BookingMapper mapper) {
        this.bookingService = bookingService;
        this.seatOperations = seatOperations;
        this.inventoryService = inventoryService;
        this.paymentRepository = paymentRepository;
        this.mapper = mapper;
    }

    private record Instrument(String description, String declineReason) {}

    @Transactional(noRollbackFor = ApiException.class)
    public PaymentResult pay(String pnr, Long userId, PaymentRequest r) {
        Booking b = bookingService.lockOwned(pnr, userId, false);
        if (b.getStatus().isLive()) {
            Payment paid = b.successfulPayment();
            return new PaymentResult("SUCCESS", "This booking is already paid.", paid == null ? null : paid.getTxnRef(),
                    mapper.toView(b));
        }
        if (b.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw ApiException.conflict("PAYMENT_CLOSED",
                    "This booking can no longer be paid because its seats were released. Search again to book.");
        }
        LocalDateTime now = inventoryService.now();
        if (b.getPaymentDueAt() != null && now.isAfter(b.getPaymentDueAt())) {
            expire(b);
            throw ApiException.conflict("HOLD_EXPIRED",
                    "Your seat hold expired before payment, so the seats were released. Search again to book.");
        }

        Instrument instrument = validate(r);
        String txnRef = newTxnRef();
        if (instrument.declineReason() != null) {
            b.addPayment(new Payment(txnRef, r.method(), instrument.description(), b.getTotalFare(),
                    PaymentStatus.FAILED, instrument.declineReason(), now));
            log.warn("Payment declined pnr={} txn={} reason={}", b.getPnr(), txnRef, instrument.declineReason());
            return new PaymentResult("FAILED", instrument.declineReason()
                    + " Your seats stay held until " + b.getPaymentDueAt().toLocalTime().withNano(0) + ".", txnRef, mapper.toView(b));
        }
        b.addPayment(new Payment(txnRef, r.method(), instrument.description(), b.getTotalFare(),
                PaymentStatus.SUCCESS, null, now));
        b.setStatus(BookingStatus.BOOKED);
        b.setBookedAt(now);
        log.info("Payment received pnr={} txn={} amount={}", b.getPnr(), txnRef, b.getTotalFare());
        return new PaymentResult("SUCCESS", "Payment received. Your ticket is booked.", txnRef, mapper.toView(b));
    }

    /** The passenger gives up on paying: the hold is released at once instead of waiting for expiry. */
    @Transactional
    public BookingView abandon(String pnr, Long userId) {
        Booking b = bookingService.lockOwned(pnr, userId, false);
        if (b.getStatus() == BookingStatus.PAYMENT_PENDING) {
            seatOperations.release(b, b.activePassengers());
            b.setStatus(BookingStatus.PAYMENT_FAILED);
            b.setClosedAt(inventoryService.now());
        }
        return mapper.toView(b);
    }

    /** Releases an unpaid hold whose time has run out. The booking must be locked. */
    public void expire(Booking b) {
        if (b.getStatus() != BookingStatus.PAYMENT_PENDING) return;
        seatOperations.release(b, b.activePassengers());
        b.setStatus(BookingStatus.EXPIRED);
        b.setClosedAt(inventoryService.now());
        log.info("Seat hold expired pnr={}", b.getPnr());
    }

    private Instrument validate(PaymentRequest r) {
        return switch (r.method()) {
            case UPI -> {
                String vpa = r.upiId() == null ? "" : r.upiId().trim();
                if (!UPI.matcher(vpa).matches()) {
                    throw ApiException.badRequest("INVALID_UPI", "Enter a UPI ID like name@bank.");
                }
                yield new Instrument("UPI " + vpa.toLowerCase(Locale.ROOT),
                        vpa.toLowerCase(Locale.ROOT).startsWith("decline")
                                ? "The UPI request was declined in your UPI app." : null);
            }
            case CARD -> {
                String number = r.cardNumber() == null ? "" : r.cardNumber().replaceAll("[\\s-]", "");
                if (!number.matches("\\d{13,19}") || !luhn(number)) {
                    throw ApiException.badRequest("INVALID_CARD", "Check the card number.");
                }
                YearMonth expiry = parseExpiry(r.cardExpiry());
                if (expiry == null) {
                    throw ApiException.badRequest("INVALID_EXPIRY", "Enter the expiry as MM/YY.");
                }
                if (expiry.isBefore(YearMonth.from(inventoryService.today()))) {
                    throw ApiException.badRequest("CARD_EXPIRED", "This card has expired.");
                }
                if (r.cardCvv() == null || !r.cardCvv().matches("\\d{3,4}")) {
                    throw ApiException.badRequest("INVALID_CVV", "Enter the 3 or 4 digit security code.");
                }
                if (r.cardName() == null || r.cardName().trim().length() < 2) {
                    throw ApiException.badRequest("INVALID_CARD_NAME", "Enter the name on the card.");
                }
                String description = brand(number) + " ending " + number.substring(number.length() - 4);
                yield new Instrument(description, DECLINED_CARD.equals(number)
                        ? "Your bank declined the card." : null);
            }
            case NETBANKING -> {
                String bank = r.bankCode() == null ? "" : r.bankCode().trim().toUpperCase(Locale.ROOT);
                if (!BANKS.containsKey(bank)) {
                    throw ApiException.badRequest("INVALID_BANK", "Choose your bank.");
                }
                yield new Instrument("Net banking, " + BANKS.get(bank), null);
            }
        };
    }

    static boolean luhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    static String brand(String n) {
        if (n.startsWith("4")) return "Visa";
        if (n.matches("^(5[1-5]|222[1-9]|22[3-9]\\d|2[3-6]\\d{2}|27[01]\\d|2720).*")) return "Mastercard";
        if (n.matches("^(60|65|81|82|508).*")) return "RuPay";
        if (n.matches("^3[47].*")) return "American Express";
        return "Card";
    }

    static YearMonth parseExpiry(String raw) {
        if (raw == null || !raw.trim().matches("^(0[1-9]|1[0-2])/?\\d{2}$")) return null;
        String digits = raw.replace("/", "").trim();
        return YearMonth.of(2000 + Integer.parseInt(digits.substring(2)), Integer.parseInt(digits.substring(0, 2)));
    }

    private String newTxnRef() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder("RRS");
            for (int i = 0; i < 12; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
            if (!paymentRepository.existsByTxnRef(sb.toString())) return sb.toString();
        }
        throw new IllegalStateException("Could not allocate a transaction reference");
    }
}
