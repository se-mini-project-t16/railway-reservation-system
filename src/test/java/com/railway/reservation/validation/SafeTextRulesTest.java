package com.railway.reservation.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SafeTextRules: SQL injection and XSS rejection [RRS-SR-003]")
class SafeTextRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert(1)",
            "%3Cscript%3E",
            "&lt;script&gt;",
            "svg onload=alert(1)",
            "admin' OR '1'='1",
            "' OR 1=1 --",
            "1 OR 1=1",
            "admin'--",
            "x'; DROP TABLE users;--",
            "Robert'); DROP TABLE Students;--",
            "UNION SELECT * FROM users",
            "1; SELECT * FROM users",
            "sleep(5)",
            "a\u0000b"
    })
    void rejectsMaliciousInput(String payload) {
        assertThat(SafeTextRules.isSafe(payload)).as("payload should be rejected: %s", payload).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "O'Brien",
            "D'Souza",
            "Anne-Marie",
            "Conor O'Brien",
            "Rajdhani Express",
            "Karnataka Express",
            "Bengaluru City Junction",
            "KSR Bengaluru (SBC)",
            "Dr. B. R. Ambedkar Nagar",
            "Sri Ganganagar",
            "Bangalore and Mysore Express",
            "3A",
            "SL",
            "12627"
    })
    void allowsNormalInput(String value) {
        assertThat(SafeTextRules.isSafe(value)).as("value should be allowed: %s", value).isTrue();
    }

    @Test
    void treatsNullAsSafeSoNotBlankCanReportMissingValues() {
        assertThat(SafeTextRules.isSafe(null)).isTrue();
    }

    @Test
    void requireSafeThrowsWithoutEchoingThePayload() {
        assertThatThrownBy(() -> SafeTextRules.requireSafe("Source station", "<script>alert(1)</script>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source station")
                .hasMessageNotContaining("<script>");
    }
}
