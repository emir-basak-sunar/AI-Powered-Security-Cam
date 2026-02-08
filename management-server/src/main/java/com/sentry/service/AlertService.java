package com.sentry.service;

import com.sentry.dto.AlertPayload;
import com.sentry.entity.Alert;
import com.sentry.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for alert management and WebSocket broadcasting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new alert from AI Engine payload.
     * Automatically broadcasts to WebSocket subscribers.
     */
    @Transactional
    public Alert createAlert(AlertPayload payload) {
        Alert alert = Alert.builder()
                .cameraId(payload.getCameraId())
                .alertType(payload.getAlertType())
                .message(payload.getDescription())
                .imageData(payload.getImageBase64())
                .timestamp(payload.getTimestamp())
                .acknowledged(false)
                .build();

        Alert savedAlert = alertRepository.save(alert);
        
        log.info("Alert created: {} from camera {}", savedAlert.getId(), savedAlert.getCameraId());

        // Broadcast to WebSocket subscribers
        broadcastAlert(savedAlert);

        return savedAlert;
    }

    /**
     * Broadcast alert to WebSocket topic.
     */
    private void broadcastAlert(Alert alert) {
        try {
            messagingTemplate.convertAndSend("/topic/live-alerts", alert);
            log.debug("Alert {} broadcast to /topic/live-alerts", alert.getId());
        } catch (Exception e) {
            log.error("Failed to broadcast alert: {}", e.getMessage());
        }
    }

    /**
     * Get paginated list of alerts.
     */
    public Page<Alert> getAlerts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return alertRepository.findLatestAlerts(pageable);
    }

    /**
     * Get alert by ID.
     */
    public Optional<Alert> getAlert(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * Get alerts by camera ID.
     */
    public List<Alert> getAlertsByCameraId(String cameraId) {
        return alertRepository.findByCameraId(cameraId);
    }

    /**
     * Get unacknowledged alerts.
     */
    public Page<Alert> getUnacknowledgedAlerts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return alertRepository.findByAcknowledged(false, pageable);
    }

    /**
     * Acknowledge an alert.
     */
    @Transactional
    public Optional<Alert> acknowledgeAlert(Long id, String acknowledgedBy) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setAcknowledged(true);
                    alert.setAcknowledgedBy(acknowledgedBy);
                    alert.setAcknowledgedAt(LocalDateTime.now());
                    return alertRepository.save(alert);
                });
    }

    /**
     * Delete an alert.
     */
    @Transactional
    public boolean deleteAlert(Long id) {
        if (alertRepository.existsById(id)) {
            alertRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Get alert statistics.
     */
    public AlertStats getStats() {
        long total = alertRepository.count();
        long unacknowledged = alertRepository.countByAcknowledged(false);
        return new AlertStats(total, unacknowledged);
    }

    public record AlertStats(long total, long unacknowledged) {}
}
