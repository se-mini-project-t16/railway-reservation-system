package com.railway.reservation.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.reservation.service.payment.PaymentGateway.GatewayOrder;
import com.railway.reservation.service.payment.PaymentGateway.GatewayPayment;
import com.railway.reservation.service.payment.PaymentGateway.GatewayRefund;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The real Razorpay client, talking to a local server that answers the way Razorpay's API does. */
class RazorpayGatewayTest {

    record Seen(String method, String path, String auth, String body) {}

    private final ObjectMapper json = new ObjectMapper();
    private final List<Seen> seen = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private RazorpayGateway gateway;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String path = exchange.getRequestURI().getRawPath()
                    + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery());
            seen.add(new Seen(exchange.getRequestMethod(), path, exchange.getRequestHeaders().getFirst("Authorization"), body));
            int status = 200;
            String reply;
            if (path.equals("/v1/orders")) {
                reply = "{\"id\":\"order_N1\",\"entity\":\"order\",\"amount\":154500,\"currency\":\"INR\",\"status\":\"created\"}";
            } else if (path.startsWith("/v1/payments/pay_CARD1")) {
                reply = "{\"id\":\"pay_CARD1\",\"order_id\":\"order_N1\",\"amount\":154500,\"status\":\"captured\",\"method\":\"card\","
                        + "\"card\":{\"last4\":\"1111\",\"network\":\"Visa\",\"type\":\"credit\"}}";
            } else if (path.equals("/v1/orders/order_N1/payments")) {
                reply = "{\"entity\":\"collection\",\"count\":2,\"items\":["
                        + "{\"id\":\"pay_F1\",\"order_id\":\"order_N1\",\"amount\":154500,\"status\":\"failed\",\"method\":\"upi\","
                        + "\"vpa\":\"failure@razorpay\",\"error_description\":\"Payment failed\"},"
                        + "{\"id\":\"pay_U1\",\"order_id\":\"order_N1\",\"amount\":154500,\"status\":\"captured\",\"method\":\"upi\","
                        + "\"vpa\":\"success@razorpay\"}]}";
            } else if (path.equals("/v1/payments/pay_U1/refund")) {
                reply = "{\"id\":\"rfnd_R1\",\"entity\":\"refund\",\"amount\":50000,\"payment_id\":\"pay_U1\",\"status\":\"processed\"}";
            } else if (path.equals("/v1/payments/pay_U1/capture")) {
                reply = "{\"id\":\"pay_U1\",\"order_id\":\"order_N1\",\"amount\":154500,\"status\":\"captured\",\"method\":\"upi\",\"vpa\":\"success@razorpay\"}";
            } else {
                status = 400;
                reply = "{\"error\":{\"code\":\"BAD_REQUEST_ERROR\",\"description\":\"The id provided does not exist\"}}";
            }
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        gateway = new RazorpayGateway("rzp_test_Key123", "secret456",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    @DisplayName("TC-PAY-12: Orders are created in paise with the key as Basic auth, and read back")
    void createsOrder() throws Exception {
        GatewayOrder o = gateway.createOrder(154500, "2123456789", Map.of("pnr", "2123456789"));
        assertThat(o.id()).isEqualTo("order_N1");
        assertThat(o.amountPaise()).isEqualTo(154500);
        Seen req = seen.getFirst();
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.auth()).isEqualTo("Basic " + Base64.getEncoder().encodeToString("rzp_test_Key123:secret456".getBytes(StandardCharsets.UTF_8)));
        JsonNode body = json.readTree(req.body());
        assertThat(body.get("amount").asLong()).isEqualTo(154500);
        assertThat(body.get("currency").asText()).isEqualTo("INR");
        assertThat(body.get("receipt").asText()).isEqualTo("2123456789");
        assertThat(body.get("notes").get("pnr").asText()).isEqualTo("2123456789");
    }

    @Test
    @DisplayName("TC-PAY-13: Payments are read with masked card details; an order's attempts are listed")
    void readsPayments() {
        GatewayPayment card = gateway.fetchPayment("pay_CARD1");
        assertThat(seen.getFirst().path()).isEqualTo("/v1/payments/pay_CARD1?expand%5B%5D=card");
        assertThat(card.captured()).isTrue();
        assertThat(card.instrument()).isEqualTo("Visa credit card ending 1111");

        List<GatewayPayment> attempts = gateway.paymentsForOrder("order_N1");
        assertThat(attempts).extracting(GatewayPayment::status).containsExactly("failed", "captured");
        assertThat(attempts.get(0).errorDescription()).isEqualTo("Payment failed");
        assertThat(attempts.get(1).instrument()).isEqualTo("UPI success@razorpay");
    }

    @Test
    @DisplayName("TC-PAY-14: Refunds and captures are sent for the exact amount")
    void refundsAndCaptures() throws Exception {
        GatewayRefund r = gateway.refund("pay_U1", 50000, Map.of("pnr", "2123456789"));
        assertThat(r.id()).isEqualTo("rfnd_R1");
        assertThat(r.status()).isEqualTo("processed");
        JsonNode body = json.readTree(seen.getLast().body());
        assertThat(body.get("amount").asLong()).isEqualTo(50000);
        assertThat(body.get("speed").asText()).isEqualTo("normal");

        assertThat(gateway.capture("pay_U1", 154500).captured()).isTrue();
        assertThat(json.readTree(seen.getLast().body()).get("amount").asLong()).isEqualTo(154500);
    }

    @Test
    @DisplayName("TC-PAY-15: Razorpay errors and bad ids become a gateway failure; no keys means payment is off")
    void errorsAndDisabled() {
        assertThatThrownBy(() -> gateway.fetchPayment("pay_MISSING"))
                .isInstanceOf(GatewayException.class).hasMessage("The id provided does not exist");
        assertThatThrownBy(() -> gateway.fetchPayment("../orders")).isInstanceOf(GatewayException.class);
        assertThat(gateway.enabled()).isTrue();
        assertThat(gateway.testMode()).isTrue();

        RazorpayGateway off = new RazorpayGateway("", "", "http://127.0.0.1:1/v1");
        assertThat(off.enabled()).isFalse();
        assertThat(off.signatureValid("order_a", "pay_b", "c")).isFalse();
        assertThatThrownBy(() -> off.createOrder(100, "r", Map.of())).isInstanceOf(GatewayException.class);
    }

    @Test
    @DisplayName("TC-PAY-16: Every payment method is described without exposing full numbers")
    void instruments() throws Exception {
        assertThat(RazorpayGateway.instrument(json.readTree("{\"bank\":\"HDFC\"}"), "netbanking")).isEqualTo("Net banking, HDFC");
        assertThat(RazorpayGateway.instrument(json.readTree("{\"wallet\":\"paytm\"}"), "wallet")).isEqualTo("Wallet, paytm");
        assertThat(RazorpayGateway.instrument(json.readTree("{\"upi\":{\"vpa\":\"a@okaxis\"}}"), "upi")).isEqualTo("UPI a@okaxis");
        assertThat(RazorpayGateway.instrument(json.readTree("{\"card\":{\"last4\":\"4242\",\"network\":\"RuPay\",\"type\":\"debit\"}}"), "card"))
                .isEqualTo("RuPay debit card ending 4242");
        assertThat(RazorpayGateway.instrument(json.readTree("{}"), "")).isEqualTo("Online payment");
    }
}
