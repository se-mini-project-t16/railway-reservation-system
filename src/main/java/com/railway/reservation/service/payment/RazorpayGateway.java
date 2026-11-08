package com.railway.reservation.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Razorpay over its REST API (Orders, Payments, Refunds). Keys come from the git-ignored secrets file
 * (railway-secrets.properties); with no keys the gateway reports itself disabled and the pay page says payments are
 * unavailable. Test keys ({@code rzp_test_}) use Razorpay's test mode, where no real money moves.
 */
@Component
public class RazorpayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private final String keyId;
    private final String keySecret;
    private final RestClient http;
    private final ObjectMapper json = new ObjectMapper();

    public RazorpayGateway(@Value("${app.razorpay.key-id:}") String keyId,
                           @Value("${app.razorpay.key-secret:}") String keySecret,
                           @Value("${app.razorpay.api-base:https://api.razorpay.com/v1}") String apiBase) {
        boolean configured = keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
        this.keyId = configured ? keyId.trim() : null;
        this.keySecret = configured ? keySecret.trim() : null;
        if (!configured) {
            this.http = null;
            log.warn("Online payment is off: app.razorpay.key-id and app.razorpay.key-secret are not set");
            return;
        }
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(10));
        timeouts.setReadTimeout(Duration.ofSeconds(20));
        String basic = Base64.getEncoder().encodeToString((this.keyId + ":" + this.keySecret).getBytes(StandardCharsets.UTF_8));
        this.http = RestClient.builder()
                .baseUrl(apiBase)
                .requestFactory(timeouts)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("Razorpay payments on ({} mode)", testMode() ? "test" : "live");
    }

    @Override
    public boolean enabled() {
        return http != null;
    }

    @Override
    public String keyId() {
        return keyId;
    }

    @Override
    public boolean testMode() {
        return keyId != null && keyId.startsWith("rzp_test_");
    }

    @Override
    public GatewayOrder createOrder(long amountPaise, String receipt, Map<String, String> notes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountPaise);
        body.put("currency", "INR");
        body.put("receipt", receipt);
        body.put("notes", notes);
        JsonNode o = post("/orders", body);
        return new GatewayOrder(o.path("id").asText(), o.path("amount").asLong(), o.path("status").asText());
    }

    @Override
    public GatewayPayment fetchPayment(String paymentId) {
        return payment(get("/payments/" + safeId(paymentId) + "?expand[]=card"));
    }

    @Override
    public List<GatewayPayment> paymentsForOrder(String orderId) {
        JsonNode items = get("/orders/" + safeId(orderId) + "/payments").path("items");
        List<GatewayPayment> out = new ArrayList<>();
        items.forEach(p -> out.add(payment(p)));
        return out;
    }

    @Override
    public GatewayPayment capture(String paymentId, long amountPaise) {
        return payment(post("/payments/" + safeId(paymentId) + "/capture", Map.of("amount", amountPaise, "currency", "INR")));
    }

    @Override
    public GatewayRefund refund(String paymentId, long amountPaise, Map<String, String> notes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountPaise);
        body.put("speed", "normal");
        body.put("notes", notes);
        JsonNode r = post("/payments/" + safeId(paymentId) + "/refund", body);
        return new GatewayRefund(r.path("id").asText(), r.path("amount").asLong(), r.path("status").asText());
    }

    @Override
    public boolean signatureValid(String orderId, String paymentId, String signature) {
        return keySecret != null && RazorpaySignature.matches(keySecret, orderId, paymentId, signature);
    }

    // ------------------------------------------------------------------ mapping

    static GatewayPayment payment(JsonNode p) {
        String method = p.path("method").asText("");
        return new GatewayPayment(p.path("id").asText(), p.path("order_id").asText(null), p.path("amount").asLong(),
                p.path("status").asText(), method, instrument(p, method), p.path("error_description").asText(null));
    }

    /** A masked, human description of what was used to pay. Never a full card number. */
    static String instrument(JsonNode p, String method) {
        String text = switch (method) {
            case "card" -> {
                JsonNode card = p.path("card");
                String network = card.path("network").asText("");
                String last4 = card.path("last4").asText("");
                String type = card.path("type").asText("");
                String kind = type.isBlank() ? "card" : type + " card";
                yield (network.isBlank() || network.equalsIgnoreCase("unknown") ? "" : network + " ") + kind
                        + (last4.isBlank() ? "" : " ending " + last4);
            }
            case "upi" -> {
                String vpa = p.path("vpa").asText(p.path("upi").path("vpa").asText(""));
                yield vpa.isBlank() ? "UPI" : "UPI " + vpa;
            }
            case "netbanking" -> "Net banking, " + p.path("bank").asText("bank");
            case "wallet" -> "Wallet, " + p.path("wallet").asText("wallet");
            case "emi" -> "Card EMI";
            case "paylater" -> "Pay later";
            default -> method.isBlank() ? "Online payment" : method.substring(0, 1).toUpperCase(Locale.ROOT) + method.substring(1);
        };
        text = text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
        return text.length() > 60 ? text.substring(0, 60) : text;
    }

    // ------------------------------------------------------------------ HTTP

    private JsonNode get(String path) {
        return call(() -> http.get().uri(path).retrieve().body(String.class), "GET " + path);
    }

    private JsonNode post(String path, Object body) {
        String payload;
        try {
            payload = json.writeValueAsString(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new GatewayException("Could not prepare the Razorpay request", e);
        }
        return call(() -> http.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(payload)
                .retrieve().body(String.class), "POST " + path);
    }

    private JsonNode call(java.util.function.Supplier<String> request, String what) {
        if (http == null) throw new GatewayException("Online payment is not configured");
        try {
            return json.readTree(request.get());
        } catch (RestClientResponseException e) {
            String reason = errorDescription(e.getResponseBodyAsString());
            int status = e.getStatusCode().value();
            if (status == 400 || status == 404) {
                // Razorpay looked and said no (an unknown id, usually from a bad request): not a fault here
                log.warn("Razorpay {} refused: {} {}", what, status, reason);
            } else {
                log.error("Razorpay {} failed: {} {}", what, status, reason);
            }
            throw new GatewayException(reason, status, e);
        } catch (RestClientException e) {
            log.error("Razorpay {} failed: {}", what, e.getMessage());
            throw new GatewayException("Razorpay could not be reached", e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new GatewayException("Razorpay sent a response that could not be read", e);
        }
    }

    private String errorDescription(String body) {
        try {
            String d = json.readTree(body).path("error").path("description").asText("");
            return d.isBlank() ? "Razorpay refused the request" : d;
        } catch (Exception e) {
            return "Razorpay refused the request";
        }
    }

    /** Ids are only ever Razorpay's own (order_..., pay_...), never free text in a URL. */
    private static String safeId(String id) {
        if (id == null || !id.matches("^[A-Za-z0-9_]{1,40}$")) throw new GatewayException("Invalid Razorpay id", 400, null);
        return id;
    }
}
