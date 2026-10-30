package com.railway.reservation.controller;

import com.railway.reservation.service.SystemUptimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health and Availability Controller (RRS-NF-002, TC-OPS-01, SAD Section 4.4).
 * Publishes operational health status, database connectivity, and uptime statistics.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final SystemUptimeService uptimeService;

    public HealthController(SystemUptimeService uptimeService) {
        this.uptimeService = uptimeService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = uptimeService.getSystemHealth();
        boolean isUp = "UP".equals(health.get("status"));
        return ResponseEntity.status(isUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(health);
    }
}
