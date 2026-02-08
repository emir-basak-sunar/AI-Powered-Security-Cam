package com.sentry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Camera entity for managing security cameras.
 */
@Entity
@Table(name = "cameras")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Camera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(name = "stream_url", length = 500)
    private String streamUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CameraStatus status = CameraStatus.INACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CameraStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        ERROR
    }
}
