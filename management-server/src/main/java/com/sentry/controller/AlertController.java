package com.sentry.controller;

import com.sentry.dto.AlertPayload;
import com.sentry.entity.Alert;
import com.sentry.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for alert management endpoints.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;

    /**
     * Receive alert from AI Engine.
     * Authenticated via API key.
     */
    @PostMapping
    public ResponseEntity<Alert> createAlert(@Valid @RequestBody AlertPayload payload) {
        log.info("Received alert from camera {}: {}", payload.getCameraId(), payload.getAlertType());
        Alert alert = alertService.createAlert(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(alert);
    }

    /**
     * Get paginated list of alerts.
     */
    @GetMapping
    public ResponseEntity<Page<Alert>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(alertService.getAlerts(page, size));
    }

    /**
     * Get alert by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlert(@PathVariable Long id) {
        return alertService.getAlert(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get alerts by camera ID.
     */
    @GetMapping("/camera/{cameraId}")
    public ResponseEntity<?> getAlertsByCamera(@PathVariable String cameraId) {
        return ResponseEntity.ok(alertService.getAlertsByCameraId(cameraId));
    }

    /**
     * Get unacknowledged alerts.
     */
    @GetMapping("/unacknowledged")
    public ResponseEntity<Page<Alert>> getUnacknowledgedAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(alertService.getUnacknowledgedAlerts(page, size));
    }

    /**
     * Acknowledge an alert.
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();
        return alertService.acknowledgeAlert(id, username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete an alert.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        if (alertService.deleteAlert(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get alert statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        AlertService.AlertStats stats = alertService.getStats();
        return ResponseEntity.ok(Map.of(
                "total", stats.total(),
                "unacknowledged", stats.unacknowledged()
        ));
    }
}
