package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.Station;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.CancellationRuleRepository;
import com.railway.reservation.repository.StationRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import com.railway.reservation.service.rail.Housekeeping;
import com.railway.reservation.support.FakeRazorpay;
import com.railway.reservation.support.MailCatcherConfig;
import com.railway.reservation.support.MutableClock;
import com.railway.reservation.support.RailFixtures;
import com.railway.reservation.support.TestClockConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end behaviour over HTTP on a small known network (see {@link RailFixtures}), with the clock
 * fixed at 09:00 on a Monday so Tatkal opening, chart preparation and hold expiry are deterministic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestClockConfig.class, FakeRazorpay.Config.class, MailCatcherConfig.class})
class RailJourneyIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CancellationRuleRepository rules;
    @Autowired StationRepository stationRepository;
    @Autowired TrainRepository trainRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired MutableClock clock;
    @Autowired Housekeeping housekeeping;
    @Autowired FakeRazorpay razorpay;
    @Autowired MailCatcherConfig.MailCatcher mail;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);
    private String asha;
    private String bob;
    private String admin;

    @BeforeEach
    void setUp() {
        clock.set(MONDAY.atTime(9, 0));
        razorpay.reset();
        mail.clear();
        RailFixtures.reset(jdbc, rules);
        Map<String, Station> st = RailFixtures.stations(stationRepository);
        RailFixtures.superfast(st, trainRepository);
        RailFixtures.weekday(st, trainRepository);
        asha = token("asha@example.com", Role.ROLE_PASSENGER);
        bob = token("bob@example.com", Role.ROLE_PASSENGER);
        admin = token("ops@example.com", Role.ROLE_ADMIN);
    }

    @AfterEach
    void tearDown() {
        clock.reset();
        RailFixtures.reset(jdbc, rules);
    }

    private String token(String email, Role role) {
        User u = userRepository.save(new User("Test " + email, email, "9876543210", passwordEncoder.encode("Password#1"), role));
        return jwtService.generateToken(u);
    }

    private ResultActions as(String token, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    private JsonNode body(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private Map<String, Object> bookingBody(LocalDate date, String cls, String quota, int travellers) {
        List<Map<String, Object>> pax = new ArrayList<>();
        for (int i = 0; i < travellers; i++) {
            pax.add(Map.of("name", "Traveller " + (char) ('A' + i), "age", 30 + i, "gender", "FEMALE"));
        }
        Map<String, Object> m = new HashMap<>();
        m.put("trainNumber", "12901");
        m.put("from", "AAA");
        m.put("to", "DDD");
        m.put("date", date.toString());
        m.put("classCode", cls);
        m.put("quota", quota);
        m.put("passengers", pax);
        m.put("mobile", "9876543210");
        return m;
    }

    private JsonNode book(String token, LocalDate date, String cls, int travellers) throws Exception {
        return body(as(token, post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(bookingBody(date, cls, "GN", travellers)))).andExpect(status().isCreated()));
    }

    /** Opens Checkout for the booking: the server creates (or reuses) the Razorpay order. */
    private String order(String token, String pnr) throws Exception {
        return body(as(token, post("/api/v1/payments/" + pnr + "/order")).andExpect(status().isOk())).get("orderId").asText();
    }

    private ResultActions confirm(String token, String pnr, FakeRazorpay.Paid paid) throws Exception {
        return as(token, post("/api/v1/payments/" + pnr + "/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("orderId", paid.orderId(), "paymentId", paid.paymentId(),
                        "signature", paid.signature()))));
    }

    /** The passenger pays by UPI in Checkout and the page reports the result. */
    private JsonNode pay(String token, String pnr) throws Exception {
        FakeRazorpay.Paid paid = razorpay.pay(order(token, pnr), "upi", "UPI asha@okaxis");
        return body(confirm(token, pnr, paid).andExpect(status().isOk()));
    }

    // ------------------------------------------------------------------ search

    @Test
    @DisplayName("TC-SRC-01: Guests can search; results carry times, classes, fares and availability")
    void guestSearch() throws Exception {
        JsonNode r = body(as(null, get("/api/v1/trains/search").param("from", "AAA").param("to", "DDD")
                .param("date", MONDAY.plusDays(1).toString())).andExpect(status().isOk()));
        assertThat(r.get("results")).hasSize(2);
        JsonNode t = r.get("results").get(0);
        assertThat(t.get("number").asText()).isEqualTo("12901");
        assertThat(t.get("from").get("time").asText()).isEqualTo("08:00");
        assertThat(t.get("to").get("day").asInt()).isEqualTo(2);
        assertThat(t.get("classes").get(0).get("status").asText()).isEqualTo("AVAILABLE");
        assertThat(t.get("classes").get(0).get("fare").asInt()).isPositive();
    }

    @Test
    @DisplayName("TC-SRC-02: Search finds a train between two intermediate stops, with that stretch's distance")
    void intermediateStops() throws Exception {
        JsonNode r = body(as(null, get("/api/v1/trains/search").param("from", "BBB").param("to", "CCC")
                .param("date", MONDAY.plusDays(1).toString())).andExpect(status().isOk()));
        assertThat(r.get("results")).hasSize(1);
        assertThat(r.get("results").get(0).get("distanceKm").asInt()).isEqualTo(250);
        JsonNode reverse = body(as(null, get("/api/v1/trains/search").param("from", "CCC").param("to", "BBB")
                .param("date", MONDAY.plusDays(1).toString())));
        assertThat(reverse.get("results")).isEmpty();
    }

    @Test
    @DisplayName("TC-SRC-03: Trains are listed only on the days they run")
    void runningDays() throws Exception {
        LocalDate saturday = MONDAY.plusDays(5);
        JsonNode r = body(as(null, get("/api/v1/trains/search").param("from", "AAA").param("to", "DDD").param("date", saturday.toString())));
        assertThat(r.get("results")).extracting(n -> n.get("number").asText()).containsExactly("12901");
    }

    @Test
    @DisplayName("TC-SRC-04: Same station, a past date, a date past 60 days and an unknown station are rejected")
    void searchValidation() throws Exception {
        as(null, get("/api/v1/trains/search").param("from", "AAA").param("to", "AAA").param("date", MONDAY.plusDays(1).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SAME_STATION"));
        as(null, get("/api/v1/trains/search").param("from", "AAA").param("to", "DDD").param("date", MONDAY.minusDays(1).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATE_IN_PAST"));
        as(null, get("/api/v1/trains/search").param("from", "AAA").param("to", "DDD").param("date", MONDAY.plusDays(61).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATE_TOO_FAR"));
        as(null, get("/api/v1/trains/search").param("from", "ZZZ").param("to", "DDD").param("date", MONDAY.plusDays(1).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UNKNOWN_STATION"));
    }

    @Test
    @DisplayName("TC-SRC-05: 'Nearby' widens a search to other stations in the same city")
    void nearbyStations() throws Exception {
        JsonNode exact = body(as(null, get("/api/v1/trains/search").param("from", "AAB").param("to", "DDD").param("date", MONDAY.plusDays(1).toString())));
        assertThat(exact.get("results")).isEmpty();
        JsonNode near = body(as(null, get("/api/v1/trains/search").param("from", "AAB").param("to", "DDD").param("nearby", "true").param("date", MONDAY.plusDays(1).toString())));
        assertThat(near.get("results")).isNotEmpty();
    }

    @Test
    @DisplayName("TC-SRC-06: A train's page, its availability calendar and a station board are public")
    void trainPages() throws Exception {
        as(null, get("/api/v1/trains/12901")).andExpect(status().isOk()).andExpect(jsonPath("$.stops.length()").value(4));
        JsonNode cal = body(as(null, get("/api/v1/trains/12901/availability").param("from", "AAA").param("to", "DDD").param("class", "SL")
                .param("date", MONDAY.plusDays(1).toString()).param("days", "6")));
        assertThat(cal.get("days")).hasSize(6);
        JsonNode board = body(as(null, get("/api/v1/stations/AAA/departures").param("date", MONDAY.toString())));
        assertThat(board.get("departures")).hasSize(2);
        as(null, get("/api/v1/trains/99999")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ booking and ownership

    @Test
    @DisplayName("TC-SEC-06: A guest cannot book, pay, list bookings or open a ticket")
    void guestBlocked() throws Exception {
        as(null, post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(bookingBody(MONDAY.plusDays(1), "SL", "GN", 1)))).andExpect(status().isUnauthorized());
        as(null, post("/api/v1/payments/2123456789/order")).andExpect(status().isUnauthorized());
        as(null, get("/api/v1/bookings")).andExpect(status().isUnauthorized());
        as(null, get("/api/v1/bookings/2123456789/ticket.pdf")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-BK-01: Booking holds confirmed berths, prices them on the server and gives a 10-digit PNR owned by the caller")
    void bookAndHold() throws Exception {
        JsonNode b = book(asha, MONDAY.plusDays(2), "SL", 2);
        assertThat(b.get("pnr").asText()).matches("\\d{10}");
        assertThat(b.get("status").asText()).isEqualTo("PAYMENT_PENDING");
        assertThat(b.get("contactEmail").asText()).isEqualTo("asha@example.com");
        assertThat(b.get("passengers")).allSatisfy(p -> assertThat(p.get("currentStatus").get("status").asText()).isEqualTo("CNF"));
        assertThat(b.get("fare").get("total").asInt()).isEqualTo(b.get("perPassenger").asInt() * 2);
        assertThat(b.get("paymentDueAt").asText()).startsWith(MONDAY.atTime(9, 10).toString());
    }

    @Test
    @DisplayName("TC-BK-02: Missing or invalid traveller details are rejected field by field")
    void bookingValidation() throws Exception {
        Map<String, Object> bad = bookingBody(MONDAY.plusDays(2), "SL", "GN", 1);
        bad.put("passengers", List.of(Map.of("name", "<script>", "age", 3, "gender", "FEMALE")));
        bad.put("mobile", "12345");
        as(asha, post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['passengers[0].name']").exists())
                .andExpect(jsonPath("$.fieldErrors['passengers[0].age']").exists())
                .andExpect(jsonPath("$.fieldErrors.mobile").exists());
        Map<String, Object> seven = bookingBody(MONDAY.plusDays(2), "SL", "GN", 6);
        List<Object> pax = new ArrayList<>((List<?>) seven.get("passengers"));
        pax.add(Map.of("name", "Seventh Person", "age", 40, "gender", "MALE"));
        seven.put("passengers", pax);
        as(asha, post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(seven)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-SEC-07: Another passenger cannot see, pay, cancel or download someone else's booking")
    void ownershipIsolation() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        as(bob, get("/api/v1/bookings/" + pnr)).andExpect(status().isNotFound());
        as(bob, post("/api/v1/payments/" + pnr + "/order")).andExpect(status().isNotFound());
        pay(asha, pnr);
        as(bob, get("/api/v1/bookings/" + pnr + "/ticket.pdf")).andExpect(status().isNotFound());
        as(bob, post("/api/v1/bookings/" + pnr + "/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"passengerIds\":[1]}"))
                .andExpect(status().isNotFound());
        assertThat(body(as(bob, get("/api/v1/bookings")))).isEmpty();
        as(admin, get("/api/v1/bookings/" + pnr)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ payment

    @Test
    @DisplayName("TC-PAY-01: A verified Razorpay payment books the ticket; reporting it again does not record it twice")
    void payIsIdempotent() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "3A", 1).get("pnr").asText();
        JsonNode checkout = body(as(asha, post("/api/v1/payments/" + pnr + "/order")).andExpect(status().isOk()));
        assertThat(checkout.get("keyId").asText()).startsWith("rzp_test_");
        assertThat(checkout.get("testMode").asBoolean()).isTrue();
        assertThat(checkout.get("prefill").get("email").asText()).isEqualTo("asha@example.com");
        JsonNode held = body(as(asha, get("/api/v1/bookings/" + pnr)));
        assertThat(checkout.get("amount").asLong()).isEqualTo(held.get("fare").get("total").asLong() * 100);
        // Opening Checkout again reuses the same order
        assertThat(order(asha, pnr)).isEqualTo(checkout.get("orderId").asText());
        assertThat(razorpay.orderCount()).isEqualTo(1);

        FakeRazorpay.Paid paid = razorpay.pay(checkout.get("orderId").asText(), "upi", "UPI asha@okaxis");
        JsonNode first = body(confirm(asha, pnr, paid).andExpect(status().isOk()));
        assertThat(first.get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(first.get("txnRef").asText()).isEqualTo(paid.paymentId());
        assertThat(first.get("booking").get("status").asText()).isEqualTo("BOOKED");
        assertThat(first.get("booking").get("payments").get(0).get("instrument").asText()).isEqualTo("UPI asha@okaxis");
        JsonNode second = body(confirm(asha, pnr, paid).andExpect(status().isOk()));
        assertThat(second.get("txnRef").asText()).isEqualTo(paid.paymentId());
        assertThat(second.get("booking").get("payments")).hasSize(1);
        as(asha, post("/api/v1/payments/" + pnr + "/order")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PAID"));
    }

    @Test
    @DisplayName("TC-PAY-02: A declined payment is recorded and keeps the hold, so the passenger can pay again")
    void declineAndRetry() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        String orderId = order(asha, pnr);
        FakeRazorpay.Paid declined = razorpay.decline(orderId, "card", "Visa credit card ending 0002", "Your bank declined this payment.");
        JsonNode afterFailure = body(as(asha, post("/api/v1/payments/" + pnr + "/failure").contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentId\":\"" + declined.paymentId() + "\"}")).andExpect(status().isOk()));
        assertThat(afterFailure.get("status").asText()).isEqualTo("PAYMENT_PENDING");
        assertThat(afterFailure.get("payments").get(0).get("status").asText()).isEqualTo("FAILED");
        assertThat(afterFailure.get("payments").get(0).get("failureReason").asText()).isEqualTo("Your bank declined this payment.");

        JsonNode ok = body(confirm(asha, pnr, razorpay.pay(orderId, "card", "Visa credit card ending 1111")).andExpect(status().isOk()));
        assertThat(ok.get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(ok.get("booking").get("payments")).hasSize(2);
        assertThat(ok.get("booking").get("payments").get(1).get("instrument").asText()).isEqualTo("Visa credit card ending 1111");
    }

    @Test
    @DisplayName("TC-PAY-03: An unpaid hold expires after 10 minutes, its berths return, and it can no longer be paid")
    void holdExpiry() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 2).get("pnr").asText();
        order(asha, pnr);
        int before = availableSl();
        clock.advance(Duration.ofMinutes(11));
        assertThat(housekeeping.expireHolds()).isEqualTo(1);
        assertThat(availableSl()).isEqualTo(before + 2);
        as(asha, post("/api/v1/payments/" + pnr + "/order")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HOLD_EXPIRED"));
    }

    @Test
    @DisplayName("TC-PAY-04: Releasing a hold frees the berths at once")
    void abandon() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        int before = availableSl();
        as(asha, post("/api/v1/payments/" + pnr + "/abandon")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
        assertThat(availableSl()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("TC-PAY-05: A forged signature, or a payment from another booking's order, books nothing")
    void forgedPayments() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        String orderId = order(asha, pnr);
        FakeRazorpay.Paid real = razorpay.pay(orderId, "upi", "UPI asha@okaxis");
        confirm(asha, pnr, new FakeRazorpay.Paid(orderId, real.paymentId(), "0".repeat(64)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PAYMENT_UNVERIFIED"));

        String other = book(bob, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        FakeRazorpay.Paid bobs = razorpay.pay(order(bob, other), "upi", "UPI bob@okaxis");
        confirm(asha, pnr, bobs).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PAYMENT_MISMATCH"));
        confirm(asha, pnr, new FakeRazorpay.Paid("order_<script>", "pay_x", "abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        // A payment id Razorpay has never issued is bad input, not a gateway failure
        as(asha, post("/api/v1/payments/" + pnr + "/failure").contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentId\":\"pay_NoSuchPayment1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PAYMENT_MISMATCH"));
        // A body in any format other than JSON is refused cleanly
        as(asha, post("/api/v1/payments/" + pnr + "/confirm").contentType(MediaType.APPLICATION_XML).content("<x/>"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        assertThat(body(as(asha, get("/api/v1/bookings/" + pnr))).get("status").asText()).isEqualTo("PAYMENT_PENDING");
    }

    @Test
    @DisplayName("TC-PAY-06: Paid but the tab was closed: the payment is found and books the ticket instead of the hold expiring")
    void paidButTabClosed() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        razorpay.pay(order(asha, pnr), "upi", "UPI asha@okaxis");
        // Reopening the pay page finds it
        assertThat(body(as(asha, post("/api/v1/payments/" + pnr + "/sync")).andExpect(status().isOk())).get("status").asText())
                .isEqualTo("BOOKED");

        String later = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        razorpay.pay(order(asha, later), "upi", "UPI asha@okaxis");
        // Nobody comes back: the hold check at expiry finds it
        clock.advance(Duration.ofMinutes(11));
        assertThat(housekeeping.expireHolds()).isEqualTo(1);
        JsonNode b = body(as(asha, get("/api/v1/bookings/" + later)));
        assertThat(b.get("status").asText()).isEqualTo("BOOKED");
        assertThat(b.get("payments").get(0).get("status").asText()).isEqualTo("SUCCESS");
        assertThat(razorpay.refunds()).isEmpty();
    }

    @Test
    @DisplayName("TC-PAY-07: Money that arrives after the hold was released is refunded in full automatically")
    void latePaymentRefunded() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        String orderId = order(asha, pnr);
        as(asha, post("/api/v1/payments/" + pnr + "/abandon")).andExpect(status().isOk());
        FakeRazorpay.Paid late = razorpay.pay(orderId, "upi", "UPI asha@okaxis");
        JsonNode r = body(confirm(asha, pnr, late).andExpect(status().isOk()));
        assertThat(r.get("outcome").asText()).isEqualTo("REFUNDED");
        assertThat(r.get("booking").get("status").asText()).isEqualTo("PAYMENT_FAILED");
        assertThat(r.get("booking").get("payments").get(0).get("status").asText()).isEqualTo("REFUNDED");
        assertThat(razorpay.refunds()).singleElement().satisfies(x -> {
            assertThat(x.paymentId()).isEqualTo(late.paymentId());
            assertThat(x.amountPaise()).isEqualTo(r.get("booking").get("fare").get("total").asLong() * 100);
        });
    }

    @Test
    @DisplayName("TC-PAY-08: If Razorpay cannot be asked, a lapsed hold waits up to 30 minutes before its berths are released")
    void gatewayDownAtExpiry() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        order(asha, pnr);
        razorpay.setDown(true);
        clock.advance(Duration.ofMinutes(11));
        assertThat(housekeeping.expireHolds()).isZero();
        assertThat(body(as(asha, get("/api/v1/bookings/" + pnr))).get("status").asText()).isEqualTo("PAYMENT_PENDING");
        clock.advance(Duration.ofMinutes(30));
        assertThat(housekeeping.expireHolds()).isEqualTo(1);
        assertThat(body(as(asha, get("/api/v1/bookings/" + pnr))).get("status").asText()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("TC-PAY-09: An authorised payment is captured before the ticket is booked; without keys, payment is unavailable")
    void captureAndUnavailable() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        FakeRazorpay.Paid authorised = razorpay.authorize(order(asha, pnr));
        assertThat(body(confirm(asha, pnr, authorised).andExpect(status().isOk())).get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(razorpay.fetchPayment(authorised.paymentId()).captured()).isTrue();

        razorpay.setEnabled(false);
        String other = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        as(asha, post("/api/v1/payments/" + other + "/order")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PAYMENTS_UNAVAILABLE"));
    }

    @Test
    @DisplayName("TC-MAIL-01: A paid booking emails the e-ticket as a PDF to the passenger")
    void bookingEmail() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 2).get("pnr").asText();
        String paymentId = pay(asha, pnr).get("txnRef").asText();
        MailCatcherConfig.Mail m = mail.await("asha@example.com", "Booking confirmed: PNR " + pnr);
        assertThat(m.subject()).contains("12901 AAA to DDD");
        assertThat(m.text()).contains("Traveller A, 30: CNF").contains("Traveller B, 31").contains(paymentId);
        assertThat(m.html()).contains("Your ticket is booked").contains(pnr);
        assertThat(m.attachments()).singleElement().satisfies(a -> {
            assertThat(a.filename()).isEqualTo("e-ticket-" + pnr + ".pdf");
            assertThat(new String(a.bytes(), 0, 4)).isEqualTo("%PDF");
        });
    }

    @Test
    @DisplayName("TC-MAIL-02: Cancelling travellers emails the refund amount and the Razorpay refund id")
    void cancellationEmail() throws Exception {
        JsonNode b = paidBooking(MONDAY.plusDays(5), 2);
        String pnr = b.get("pnr").asText();
        JsonNode after = cancel(pnr, b.get("passengers").get(0).get("id").asLong());
        int refund = after.get("refundTotal").asInt();
        String refundId = after.get("cancellations").get(0).get("refundRef").asText();
        MailCatcherConfig.Mail m = mail.await("asha@example.com", "Cancelled: PNR " + pnr);
        assertThat(m.subject()).contains("1 traveller").contains(String.valueOf(refund));
        assertThat(m.text()).contains("You cancelled 1 traveller").contains(refundId).contains("Traveller A, 30: Cancelled");
        assertThat(m.html()).contains("Travellers cancelled");
    }

    @Test
    @DisplayName("TC-MAIL-03: A mail server failure never undoes a booking; a declined payment sends no ticket")
    void mailFailureIsHarmless() throws Exception {
        String declinedPnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        FakeRazorpay.Paid declined = razorpay.decline(order(asha, declinedPnr), "card", "Visa credit card ending 0002", "Declined.");
        as(asha, post("/api/v1/payments/" + declinedPnr + "/failure").contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentId\":\"" + declined.paymentId() + "\"}")).andExpect(status().isOk());
        assertThat(mail.noneWithin("Booking confirmed", 500)).isTrue();

        mail.failNext(true);
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        assertThat(pay(asha, pnr).get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(mail.noneWithin("Booking confirmed", 500)).isTrue();
        assertThat(body(as(asha, get("/api/v1/bookings/" + pnr))).get("status").asText()).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("TC-MAIL-04: A payment found later (closed tab) still emails the ticket")
    void emailAfterReconcile() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        razorpay.pay(order(asha, pnr), "upi", "UPI asha@okaxis");
        clock.advance(Duration.ofMinutes(11));
        housekeeping.expireHolds();
        assertThat(mail.await("asha@example.com", "Booking confirmed: PNR " + pnr).attachments()).hasSize(1);
    }

    private int availableSl() throws Exception {
        JsonNode r = body(as(null, get("/api/v1/trains/search").param("from", "AAA").param("to", "DDD").param("date", MONDAY.plusDays(2).toString())));
        for (JsonNode t : r.get("results")) {
            if (!t.get("number").asText().equals("12901")) continue;
            for (JsonNode c : t.get("classes")) if (c.get("code").asText().equals("SL")) return c.get("count").asInt();
        }
        return -1;
    }

    // ------------------------------------------------------------------ ticket, PNR, history

    @Test
    @DisplayName("TC-BK-03 / TC-HIS-01: The paid ticket downloads as PDF and appears in the owner's history")
    void ticketAndHistory() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        as(asha, get("/api/v1/bookings/" + pnr + "/ticket.pdf")).andExpect(status().isConflict());
        pay(asha, pnr);
        byte[] pdf = as(asha, get("/api/v1/bookings/" + pnr + "/ticket.pdf")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF)).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        JsonNode upcoming = body(as(asha, get("/api/v1/bookings").param("scope", "upcoming")));
        assertThat(upcoming).extracting(n -> n.get("pnr").asText()).containsExactly(pnr);
    }

    @Test
    @DisplayName("TC-PNR-01: Public PNR status shows statuses but never names; unpaid and unknown PNRs are not found")
    void publicPnr() throws Exception {
        String pnr = book(asha, MONDAY.plusDays(2), "SL", 1).get("pnr").asText();
        as(null, get("/api/v1/pnr/" + pnr)).andExpect(status().isNotFound());
        pay(asha, pnr);
        String text = as(null, get("/api/v1/pnr/" + pnr)).andExpect(status().isOk())
                .andExpect(jsonPath("$.passengers[0].currentStatus.status").value("CNF"))
                .andReturn().getResponse().getContentAsString();
        assertThat(text).doesNotContain("Traveller A").doesNotContain("asha@example.com").doesNotContain("9876543210");
        as(null, get("/api/v1/pnr/2999999999")).andExpect(status().isNotFound());
        as(null, get("/api/v1/pnr/123")).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ cancellation

    private JsonNode paidBooking(LocalDate date, int travellers) throws Exception {
        String pnr = book(asha, date, "SL", travellers).get("pnr").asText();
        return pay(asha, pnr).get("booking");
    }

    private JsonNode cancel(String pnr, long... ids) throws Exception {
        return body(as(asha, post("/api/v1/bookings/" + pnr + "/cancel").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("passengerIds", Arrays.stream(ids).boxed().toList())))).andExpect(status().isOk()));
    }

    @Test
    @DisplayName("TC-CAN-02: Refunds follow the rule table: flat charge beyond 48 h, 25% from 12 h, 50% from 4 h, nothing under 4 h")
    void refundTiers() throws Exception {
        JsonNode b = paidBooking(MONDAY.plusDays(3), 1);
        String pnr = b.get("pnr").asText();
        long id = b.get("passengers").get(0).get("id").asLong();
        int fare = b.get("perPassenger").asInt();
        String preview = "/api/v1/bookings/" + pnr + "/cancel/preview";
        String payload = "{\"passengerIds\":[" + id + "]}";

        int flat = body(as(asha, post(preview).contentType(MediaType.APPLICATION_JSON).content(payload))).get("totalDeduction").asInt();
        assertThat(flat).isEqualTo(120);

        LocalDateTime departure = MONDAY.plusDays(3).atTime(8, 0);
        clock.set(departure.minusHours(20));
        assertThat(body(as(asha, post(preview).contentType(MediaType.APPLICATION_JSON).content(payload))).get("totalDeduction").asInt())
                .isEqualTo(Math.max(Math.round(fare * 0.25f), 120));
        clock.set(departure.minusHours(6));
        assertThat(body(as(asha, post(preview).contentType(MediaType.APPLICATION_JSON).content(payload))).get("totalDeduction").asInt())
                .isEqualTo(Math.max(Math.round(fare * 0.5f), 120));
        clock.set(departure.minusHours(2));
        assertThat(body(as(asha, post(preview).contentType(MediaType.APPLICATION_JSON).content(payload))).get("totalRefund").asInt()).isZero();
    }

    @Test
    @DisplayName("TC-CAN-01: Cancelling some travellers refunds them, leaves the rest booked, and refuses a repeat")
    void partialCancellation() throws Exception {
        JsonNode b = paidBooking(MONDAY.plusDays(5), 2);
        String pnr = b.get("pnr").asText();
        long first = b.get("passengers").get(0).get("id").asLong();
        JsonNode after = cancel(pnr, first);
        assertThat(after.get("status").asText()).isEqualTo("PARTIALLY_CANCELLED");
        assertThat(after.get("refundTotal").asInt()).isEqualTo(b.get("perPassenger").asInt() - 120);
        assertThat(after.get("cancellations")).hasSize(1);
        // The refund goes back through Razorpay against the original payment
        String paymentId = b.get("payments").get(0).get("txnRef").asText();
        assertThat(razorpay.refunds()).singleElement().satisfies(r -> {
            assertThat(r.paymentId()).isEqualTo(paymentId);
            assertThat(r.amountPaise()).isEqualTo((b.get("perPassenger").asLong() - 120) * 100);
            assertThat(after.get("cancellations").get(0).get("refundRef").asText()).isEqualTo(r.id());
        });
        as(asha, post("/api/v1/bookings/" + pnr + "/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"passengerIds\":[" + first + "]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_CANCELLED"));
        long second = b.get("passengers").get(1).get("id").asLong();
        assertThat(cancel(pnr, second).get("status").asText()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-CAN-12: If Razorpay cannot start the refund, nothing is cancelled")
    void refundFailureCancelsNothing() throws Exception {
        JsonNode b = paidBooking(MONDAY.plusDays(5), 1);
        String pnr = b.get("pnr").asText();
        razorpay.setRefundsFail(true);
        as(asha, post("/api/v1/bookings/" + pnr + "/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"passengerIds\":[" + b.get("passengers").get(0).get("id").asLong() + "]}"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("REFUND_FAILED"));
        JsonNode still = body(as(asha, get("/api/v1/bookings/" + pnr)));
        assertThat(still.get("status").asText()).isEqualTo("BOOKED");
        assertThat(still.get("refundTotal").asInt()).isZero();
        assertThat(still.get("cancellations")).isEmpty();
    }

    @Test
    @DisplayName("TC-CAN-03: A cancellation hands its berth to the first RAC traveller")
    void cancellationPromotesRac() throws Exception {
        LocalDate date = MONDAY.plusDays(4);
        JsonNode full = paidBooking(date, 6);
        for (int i = 0; i < 8; i++) paidBooking(date, 6);
        paidBooking(date, 4);                                   // 58 general berths taken
        JsonNode rac = paidBooking(date, 1);
        assertThat(rac.get("passengers").get(0).get("currentStatus").get("text").asText()).isEqualTo("RAC 1");

        cancel(full.get("pnr").asText(), full.get("passengers").get(0).get("id").asLong());

        JsonNode promoted = body(as(asha, get("/api/v1/bookings/" + rac.get("pnr").asText())));
        assertThat(promoted.get("passengers").get(0).get("currentStatus").get("status").asText()).isEqualTo("CNF");
        assertThat(promoted.get("passengers").get(0).get("bookingStatus").get("text").asText()).isEqualTo("RAC 1");
    }

    @Test
    @DisplayName("TC-CAN-04: A fully waitlisted ticket is cancelled and refunded automatically once the chart is prepared")
    void waitlistAutoCancelledAfterChart() throws Exception {
        LocalDate date = MONDAY.plusDays(1);
        for (int i = 0; i < 12; i++) paidBooking(date, 6);      // 58 berths and 14 RAC places
        JsonNode wl = paidBooking(date, 1);
        assertThat(wl.get("passengers").get(0).get("currentStatus").get("status").asText()).isEqualTo("WL");
        clock.set(date.atTime(4, 30));                           // chart at 04:00 for an 08:00 departure
        assertThat(housekeeping.cancelWaitlistedAfterChart()).isEqualTo(1);
        JsonNode after = body(as(asha, get("/api/v1/bookings/" + wl.get("pnr").asText())));
        assertThat(after.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(after.get("refundTotal").asInt()).isEqualTo(wl.get("perPassenger").asInt() - 60);
        assertThat(razorpay.refunds()).singleElement().satisfies(r -> {
            assertThat(r.paymentId()).isEqualTo(wl.get("payments").get(0).get("txnRef").asText());
            assertThat(r.amountPaise()).isEqualTo((wl.get("perPassenger").asLong() - 60) * 100);
        });
    }

    // ------------------------------------------------------------------ Tatkal

    @Test
    @DisplayName("TC-TQ-01: Tatkal opens at 10:00 (AC) and 11:00 (non-AC) the day before, for at most 4 travellers")
    void tatkalWindow() throws Exception {
        LocalDate tomorrow = MONDAY.plusDays(1);
        Map<String, Object> body = bookingBody(tomorrow, "SL", "TQ", 1);
        as(asha, post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TATKAL_NOT_OPEN"));
        clock.set(MONDAY.atTime(LocalTime.of(11, 0)));
        JsonNode ok = body(as(asha, post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()));
        assertThat(ok.get("quota").asText()).isEqualTo("TQ");
        assertThat(ok.get("fare").get("tatkal").asInt()).isPositive();
        as(asha, post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(bookingBody(tomorrow, "SL", "TQ", 5))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TOO_MANY_TRAVELLERS"));
    }

    // ------------------------------------------------------------------ admin

    @Test
    @DisplayName("TC-ADM-02: Admin routes refuse passengers and guests")
    void adminRoutesNeedAdmin() throws Exception {
        as(null, get("/api/v1/admin/overview")).andExpect(status().isUnauthorized());
        as(asha, get("/api/v1/admin/overview")).andExpect(status().isForbidden());
        as(admin, get("/api/v1/admin/overview")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-ADM-01: An admin creates a train that appears in the next search; bad routes are rejected")
    void adminCreatesTrain() throws Exception {
        as(admin, post("/api/v1/admin/trains").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(RailFixtures.trainForm("12999", "New Service")))).andExpect(status().isCreated());
        JsonNode r = body(as(null, get("/api/v1/trains/search").param("from", "BBB").param("to", "DDD").param("date", MONDAY.plusDays(1).toString())));
        assertThat(r.get("results")).extracting(n -> n.get("number").asText()).contains("12999");

        Map<String, Object> backwards = new HashMap<>(RailFixtures.trainForm("12998", "Backwards"));
        backwards.put("stops", List.of(
                Map.of("code", "AAA", "arrival", "", "departure", "09:00", "day", 1, "distanceKm", 0),
                Map.of("code", "DDD", "arrival", "08:00", "departure", "", "day", 1, "distanceKm", 300)));
        as(admin, post("/api/v1/admin/trains").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(backwards)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TIME_ORDER"));
        as(admin, post("/api/v1/admin/trains").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(RailFixtures.trainForm("12999", "Again")))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-ADM-03: A train with tickets keeps its route, cannot be deleted, and can be stopped from taking bookings")
    void trainWithBookingsIsProtected() throws Exception {
        paidBooking(MONDAY.plusDays(2), 1);
        as(admin, delete("/api/v1/admin/trains/12901")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRAIN_HAS_BOOKINGS"));
        as(admin, patch("/api/v1/admin/trains/12901/status").contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNoContent());
        JsonNode r = body(as(null, get("/api/v1/trains/search").param("from", "AAA").param("to", "DDD").param("date", MONDAY.plusDays(1).toString())));
        assertThat(r.get("results")).extracting(n -> n.get("number").asText()).doesNotContain("12901");
    }

    @Test
    @DisplayName("TC-ADM-04: The bookings report filters by train and date, and the chart places the traveller in a berth")
    void reportAndChart() throws Exception {
        JsonNode b = paidBooking(MONDAY.plusDays(2), 1);
        JsonNode report = body(as(admin, get("/api/v1/admin/bookings").param("train", "12901").param("date", MONDAY.plusDays(2).toString())));
        assertThat(report.get("total").asInt()).isEqualTo(1);
        assertThat(report.get("items").get(0).get("pnr").asText()).isEqualTo(b.get("pnr").asText());
        JsonNode chart = body(as(admin, get("/api/v1/admin/chart").param("train", "12901").param("date", MONDAY.plusDays(2).toString()).param("class", "SL")));
        assertThat(chart.get("systemTravellers").asInt()).isEqualTo(1);
        String csv = as(admin, get("/api/v1/admin/chart.csv").param("train", "12901").param("date", MONDAY.plusDays(2).toString()).param("class", "SL"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(csv).contains(b.get("pnr").asText()).contains("\"Traveller A\"");
    }

    @Test
    @DisplayName("TC-ADM-05: Changing the rule table changes the next refund")
    void editableRules() throws Exception {
        JsonNode b = paidBooking(MONDAY.plusDays(5), 1);
        JsonNode list = body(as(admin, get("/api/v1/admin/rules")));
        long top = list.get(0).get("id").asLong();
        as(admin, put("/api/v1/admin/rules").contentType(MediaType.APPLICATION_JSON)
                .content("[{\"id\":" + top + ",\"deductionPercent\":10,\"flatMinimum\":false,\"description\":\"Early: 10 percent\"}]"))
                .andExpect(status().isNoContent());
        JsonNode pv = body(as(asha, post("/api/v1/bookings/" + b.get("pnr").asText() + "/cancel/preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"passengerIds\":[" + b.get("passengers").get(0).get("id").asLong() + "]}")));
        assertThat(pv.get("totalDeduction").asInt()).isEqualTo(Math.round(b.get("perPassenger").asInt() * 0.10f));
    }

    // ------------------------------------------------------------------ account

    @Test
    @DisplayName("TC-ACC-01: A passenger edits their profile, changes password and keeps a private list of travellers")
    void account() throws Exception {
        as(asha, put("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Asha K\",\"phone\":\"9123456780\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("9123456780"));
        as(asha, post("/api/v1/me/password").contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"wrong\",\"newPassword\":\"Another#123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        as(asha, post("/api/v1/me/password").contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"Password#1\",\"newPassword\":\"Another#123\"}"))
                .andExpect(status().isNoContent());
        JsonNode t = body(as(asha, post("/api/v1/me/travellers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ravi Kumar\",\"age\":66,\"gender\":\"MALE\"}")).andExpect(status().isCreated()));
        assertThat(body(as(bob, get("/api/v1/me/travellers")))).isEmpty();
        as(bob, delete("/api/v1/me/travellers/" + t.get("id").asLong())).andExpect(status().isNotFound());
        as(asha, delete("/api/v1/me/travellers/" + t.get("id").asLong())).andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------ errors and headers

    @Test
    @DisplayName("TC-OPS-03: Unknown API paths are 401 to guests and 404 JSON when signed in, unknown pages are the 404 page, and pages carry a CSP")
    void notFoundAndHeaders() throws Exception {
        as(null, get("/api/v1/nothing-here")).andExpect(status().isUnauthorized());
        as(asha, get("/api/v1/nothing-here")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        as(null, get("/no-such-page")).andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        as(null, get("/index.html")).andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")));
    }
}
