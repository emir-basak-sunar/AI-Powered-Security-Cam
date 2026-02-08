package com.sentry.controller;

import com.sentry.dto.CameraRequest;
import com.sentry.entity.Camera;
import com.sentry.service.CameraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for camera management endpoints.
 */
@RestController
@RequestMapping("/api/v1/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;

    /**
     * Create a new camera.
     */
    @PostMapping
    public ResponseEntity<Camera> createCamera(@Valid @RequestBody CameraRequest request) {
        Camera camera = cameraService.createCamera(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(camera);
    }

    /**
     * Get all cameras.
     */
    @GetMapping
    public ResponseEntity<List<Camera>> getAllCameras() {
        return ResponseEntity.ok(cameraService.getAllCameras());
    }

    /**
     * Get camera by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Camera> getCamera(@PathVariable Long id) {
        return cameraService.getCamera(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get cameras by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Camera>> getCamerasByStatus(@PathVariable Camera.CameraStatus status) {
        return ResponseEntity.ok(cameraService.getCamerasByStatus(status));
    }

    /**
     * Update a camera.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Camera> updateCamera(
            @PathVariable Long id,
            @Valid @RequestBody CameraRequest request
    ) {
        return cameraService.updateCamera(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update camera status.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Camera> updateCameraStatus(
            @PathVariable Long id,
            @RequestParam Camera.CameraStatus status
    ) {
        return cameraService.updateStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a camera.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCamera(@PathVariable Long id) {
        if (cameraService.deleteCamera(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
