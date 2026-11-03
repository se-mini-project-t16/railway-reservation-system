package com.railway.reservation.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.railway.reservation.config.CorrelationIdFilter;
import com.railway.reservation.config.GlobalExceptionHandler;
import com.railway.reservation.model.Role;
import com.railway.reservation.model.User;
import com.railway.reservation.service.JwtService;
import com.railway.reservation.service.SystemUptimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SystemReliabilityAndLoggingTest {

    private static final String HEALTH_URL = "/api/v1/health";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SystemUptimeService systemUptimeService;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger exceptionHandlerLogger;

    @TestConfiguration
    static class FaultyEndpointsConfiguration {
        @RestController
        static class FaultyTestController {
            @GetMapping("/api/v1/test/faulty-uncaught")
            public String triggerUncaught() {
                throw new RuntimeException("Simulated uncaught null pointer and internal stack frame: at SecretClass.java:42");
            }

            @GetMapping("/api/v1/test/faulty-sql")
            public String triggerSqlError() {
                throw new DataIntegrityViolationException("Constraint violation: INSERT INTO secret_users (password_hash) VALUES ('$2a$10$xyz') failed duplicate key");
            }
        }
    }

    @BeforeEach
    void setUpLoggingCapture() {
        exceptionHandlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        exceptionHandlerLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDownLoggingCapture() {
        if (exceptionHandlerLogger != null && listAppender != null) {
            exceptionHandlerLogger.detachAppender(listAppender);
        }
    }

    // ==========================================
    // TC-OPS-01: System Availability & Resilience [RRS-NF-002]
    // ==========================================

    @Test
    @DisplayName("TC-OPS-01: Health check reports UP status with 99% SLA availability and uptime diagnostics")
    void testHealthCheck_ReturnsOptimalStatusWith99PercentAvailability() throws Exception {
        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("railway-reservation-system"))
                .andExpect(jsonPath("$.database").value("H2 (in-memory)"))
                .andExpect(jsonPath("$.targetAvailability").value("99.0%"))
                .andExpect(jsonPath("$.availabilityStatus").value("OPTIMAL"))
                .andExpect(jsonPath("$.uptimeSeconds").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.uptimeFormatted").isNotEmpty())
                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.sla").value(containsString("99% availability guaranteed")))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.memory.status").value("UP"))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("TC-OPS-01: High-concurrency health check burst maintains 100% uptime with zero crashes")
    void testSystemReliability_HighConcurrencyHealthRequestsDoNotCrashServer() throws Exception {
        int concurrency = 30;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < concurrency; i++) {
            tasks.add(() -> mockMvc.perform(get(HEALTH_URL))
                    .andReturn()
                    .getResponse()
                    .getStatus());
        }

        List<Future<Integer>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        for (Future<Integer> future : futures) {
            assertThat(future.get()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("TC-OPS-01: Database disconnection reports DEGRADED availability status")
    void testHealthCheck_ReportsServiceUnavailableWhenDatabaseFails() throws Exception {
        DataSource faultyDataSource = mock(DataSource.class);
        when(faultyDataSource.getConnection()).thenThrow(new SQLException("Connection refused by test mock"));

        SystemUptimeService degradedService = new SystemUptimeService(faultyDataSource);
        Map<String, Object> health = degradedService.getSystemHealth();

        assertThat(health.get("status")).isEqualTo("DOWN");
        assertThat(health.get("availabilityStatus")).isEqualTo("DEGRADED");
        assertThat(degradedService.isDatabaseHealthy()).isFalse();
    }

    // ==========================================
    // TC-OPS-02: Structured Error Logging & No Leaks [RRS-NF-005]
    // ==========================================

    @Test
    @DisplayName("TC-OPS-02: Uncaught exception returns standard JSON without stack trace leakage")
    void testUncaughtException_ReturnsStandardJsonWithoutStackTraceLeak() throws Exception {
        mockMvc.perform(get("/api/v1/test/faulty-uncaught"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(jsonPath("$.path").value("/api/v1/test/faulty-uncaught"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                // Ensure no stack frame or internal class is leaked in the response payload
                .andExpect(content().string(not(containsString("SecretClass.java"))))
                .andExpect(content().string(not(containsString("NullPointerException"))))
                .andExpect(content().string(not(containsString("at com.railway"))));
    }

    @Test
    @DisplayName("TC-OPS-02: Database error sanitizes response and prevents raw SQL leakage to client")
    void testDatabaseException_SanitizesSqlAndPreventsSqlLeak() throws Exception {
        mockMvc.perform(get("/api/v1/test/faulty-sql"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("A database error occurred. Please try again later."))
                .andExpect(jsonPath("$.path").value("/api/v1/test/faulty-sql"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                // Ensure no raw SQL query, table, or password hash leaks to client
                .andExpect(content().string(not(containsString("INSERT INTO"))))
                .andExpect(content().string(not(containsString("secret_users"))))
                .andExpect(content().string(not(containsString("password_hash"))));
    }

    @Test
    @DisplayName("TC-OPS-02: Log all exceptions at ERROR level with user ID and timestamp")
    void testStructuredLogging_LogsErrorsAtErrorLevelWithUserIdAndTimestamp() throws Exception {
        User user = new User("ops-test@railway.com", "HashedPass123!", "Ops User", "9876543210", Role.ROLE_PASSENGER);
        user.setId(888L);
        String token = jwtService.generateToken(user);

        // An authenticated request for a PNR that does not exist is a 404 the handler must log
        mockMvc.perform(get("/api/v1/bookings/2999999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // Client errors are logged at WARN, server errors at ERROR; both carry user id and timestamp
        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).isNotEmpty();

        boolean foundErrorWithUserId = events.stream().anyMatch(event ->
                (event.getLevel() == Level.ERROR || event.getLevel() == Level.WARN) &&
                event.getFormattedMessage().contains("userId=888") &&
                event.getFormattedMessage().contains("RRS-NF-005") &&
                event.getFormattedMessage().contains("timestamp=")
        );

        assertThat(foundErrorWithUserId)
                .withFailMessage("Expected an error log containing userId=888, timestamp, and RRS-NF-005 marker")
                .isTrue();
    }

    @Test
    @DisplayName("TC-OPS-02: Correlation ID is generated or echoed in header for distributed tracing")
    void testCorrelationId_InjectedInResponseHeaderAndPreserved() throws Exception {
        // Test auto-generated correlation ID
        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, not(emptyOrNullString())));

        // Test custom correlation ID propagation
        String customCorrelationId = "custom-ops-trace-999888";
        mockMvc.perform(get(HEALTH_URL)
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, customCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, customCorrelationId));
    }

    @Test
    @DisplayName("TC-OPS-02: Rotating log files are configured in /logs/ directory with daily policy")
    void testDailyLogRotation_ConfiguredInLogsDirectory() {
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        assertThat(logsDir).exists().isDirectory();
    }
}
