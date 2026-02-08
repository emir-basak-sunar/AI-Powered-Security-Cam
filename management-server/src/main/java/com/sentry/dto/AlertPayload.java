package com.sentry.dto;

import com.sentry.entity.Alert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving alert payloads from AI Engine.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertPayload {

    @NotBlank(message = "Camera ID is required")
    private String cameraId;

    @NotNull(message = "Alert type is required")
    private Alert.AlertType alertType;

    private String description;

    private String imageBase64;

    private Long timestamp;
}
