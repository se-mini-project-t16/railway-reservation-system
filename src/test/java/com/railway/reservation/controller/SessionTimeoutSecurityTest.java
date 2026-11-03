package com.railway.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.railway.reservation.repository.CancellationRuleRepository;
import com.railway.reservation.repository.StationRepository;
import com.railway.reservation.support.RailFixtures;
import org.springframework.jdbc.core.JdbcTemplate;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.BookingRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** TC-SEC-05: session ends after 15 minutes of inactivity via sliding JWT renewal. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("TC-SEC-05 session inactivity timeout [RRS-SR-005]")
class SessionTimeoutSecurityTest {

    private static final String ADMIN_TRAINS_URL = "/api/v1/admin/trains";
    private static final String HEALTH_URL = "/api/v1/health";
    private static final String RENEWED_TOKEN_HEADER = "X-Auth-Token";
    private static final String TAMPERED_TOKEN = "eyJhbGciOiJIUzI1NiJ9.tampered.token";
    private static final long FIFTEEN_MINUTES_MS = 900_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User adminUser;
    private SecretKey key;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CancellationRuleRepository ruleRepository;

    @Autowired
    private StationRepository stationRepository;

    @BeforeEach
    void setUp() {
        RailFixtures.reset(jdbcTemplate, ruleRepository);
        RailFixtures.stations(stationRepository);

        adminUser = userRepository.save(new User(
                "Admin User",
                "admin@railway.com",
                "9999999999",
                passwordEncoder.encode("AdminPass123!"),
                Role.ROLE_ADMIN
        ));
        key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Signs an admin token issued and expiring at the given offsets from now. */
    private String adminTokenWithOffsets(long issuedOffsetMs, long expiresOffsetMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(adminUser.getEmail())
                .claim("userId", adminUser.getId())
                .claim("role", adminUser.getRole().name())
                .issuedAt(new Date(now + issuedOffsetMs))
                .expiration(new Date(now + expiresOffsetMs))
                .signWith(key)
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @Test
    @DisplayName("TC-SEC-05: Token idle past 15 minutes -> 401 Session Expired with inactivity notice")
    void expiredTokenReturnsSessionExpired() throws Exception {
        String idleToken = adminTokenWithOffsets(-(FIFTEEN_MINUTES_MS + 60_000), -60_000);

        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + idleToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateDto())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Session Expired"))
                .andExpect(jsonPath("$.message").value("Session expired due to inactivity. Please sign in again."))
                .andExpect(header().doesNotExist(RENEWED_TOKEN_HEADER));
    }

    @Test
    @DisplayName("TC-SEC-05: Activity renews the session; renewed token has later expiry, same identity, and works on admin routes")
    void activeRequestRenewsToken() throws Exception {
        // Session already 10 minutes in, 5 minutes left
        String originalToken = adminTokenWithOffsets(-600_000, 300_000);
        Date originalExpiry = parse(originalToken).getExpiration();

        String renewedToken = mockMvc.perform(get(HEALTH_URL)
                        .header("Authorization", "Bearer " + originalToken))
                .andExpect(status().isOk())
                .andExpect(header().exists(RENEWED_TOKEN_HEADER))
                .andReturn().getResponse().getHeader(RENEWED_TOKEN_HEADER);

        Claims renewed = parse(renewedToken);
        assertThat(renewed.getExpiration()).isAfter(originalExpiry);
        assertThat(renewed.getSubject()).isEqualTo(adminUser.getEmail());
        assertThat(renewed.get("role", String.class)).isEqualTo("ROLE_ADMIN");
        assertThat(renewed.get("userId", Number.class).longValue()).isEqualTo(adminUser.getId());

        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + renewedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateDto())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-SEC-05: Renewed token grants exactly a fresh 15-minute inactivity window")
    void renewedTokenLastsFifteenMinutes() throws Exception {
        String renewedToken = mockMvc.perform(get(HEALTH_URL)
                        .header("Authorization", "Bearer " + jwtService.generateToken(adminUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(RENEWED_TOKEN_HEADER);

        Claims renewed = parse(renewedToken);
        long lifetimeMs = renewed.getExpiration().getTime() - renewed.getIssuedAt().getTime();
        assertThat(lifetimeMs).isCloseTo(FIFTEEN_MINUTES_MS, within(1_000L));
    }

    @Test
    @DisplayName("TC-SEC-05: Missing or tampered token -> no renewal and a plain 401 (not a timeout)")
    void missingOrTamperedTokenIsNotRenewed() throws Exception {
        String json = objectMapper.writeValueAsString(sampleCreateDto());

        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(RENEWED_TOKEN_HEADER));

        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(header().doesNotExist(RENEWED_TOKEN_HEADER));

        mockMvc.perform(post(ADMIN_TRAINS_URL)
                        .header("Authorization", "Bearer " + TAMPERED_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(header().doesNotExist(RENEWED_TOKEN_HEADER));
    }

    @Test
    @DisplayName("TC-SEC-05: JwtService.renewToken keeps identity claims and moves expiry forward")
    void renewTokenKeepsClaims() {
        Claims original = parse(adminTokenWithOffsets(-600_000, 300_000));

        Claims renewed = jwtService.parseAndValidateToken(jwtService.renewToken(original));

        assertThat(renewed.getSubject()).isEqualTo(original.getSubject());
        assertThat(renewed.get("role", String.class)).isEqualTo(original.get("role", String.class));
        assertThat(renewed.get("userId", Number.class).longValue())
                .isEqualTo(original.get("userId", Number.class).longValue());
        assertThat(renewed.getExpiration()).isAfter(original.getExpiration());
    }

    private java.util.Map<String, Object> sampleCreateDto() {
        return RailFixtures.trainForm("54321", "Sample Express");
    }
}
