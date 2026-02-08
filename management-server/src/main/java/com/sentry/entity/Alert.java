package com.sentry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Alert entity for persisting security alerts from AI engine.
 */
@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false, length = 100)
    private String cameraId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "image_data", columnDefinition = "TEXT")
    private String imageData;

    @Column(name = "timestamp")
    private Long timestamp;

    @Column(name = "acknowledged")
    private boolean acknowledged = false;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
    }

    public enum AlertType {
        VISUAL,
        AUDIO
    }
}
