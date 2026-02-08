package com.sentry.dto;

import com.sentry.entity.Camera;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for camera management requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CameraRequest {

    @NotBlank(message = "Camera name is required")
    private String name;

    private String location;

    private String streamUrl;

    private Camera.CameraStatus status;
}
