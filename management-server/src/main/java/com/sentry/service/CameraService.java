package com.sentry.service;

import com.sentry.dto.CameraRequest;
import com.sentry.entity.Camera;
import com.sentry.repository.CameraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for camera management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CameraService {

    private final CameraRepository cameraRepository;

    /**
     * Create a new camera.
     */
    @Transactional
    public Camera createCamera(CameraRequest request) {
        if (cameraRepository.existsByName(request.getName())) {
            throw new RuntimeException("Camera with name '" + request.getName() + "' already exists");
        }

        Camera camera = Camera.builder()
                .name(request.getName())
                .location(request.getLocation())
                .streamUrl(request.getStreamUrl())
                .status(request.getStatus() != null ? request.getStatus() : Camera.CameraStatus.INACTIVE)
                .build();

        Camera savedCamera = cameraRepository.save(camera);
        log.info("Camera created: {} ({})", savedCamera.getName(), savedCamera.getId());
        
        return savedCamera;
    }

    /**
     * Get all cameras.
     */
    public List<Camera> getAllCameras() {
        return cameraRepository.findAll();
    }

    /**
     * Get camera by ID.
     */
    public Optional<Camera> getCamera(Long id) {
        return cameraRepository.findById(id);
    }

    /**
     * Get cameras by status.
     */
    public List<Camera> getCamerasByStatus(Camera.CameraStatus status) {
        return cameraRepository.findByStatus(status);
    }

    /**
     * Update a camera.
     */
    @Transactional
    public Optional<Camera> updateCamera(Long id, CameraRequest request) {
        return cameraRepository.findById(id)
                .map(camera -> {
                    if (request.getName() != null) {
                        camera.setName(request.getName());
                    }
                    if (request.getLocation() != null) {
                        camera.setLocation(request.getLocation());
                    }
                    if (request.getStreamUrl() != null) {
                        camera.setStreamUrl(request.getStreamUrl());
                    }
                    if (request.getStatus() != null) {
                        camera.setStatus(request.getStatus());
                    }
                    return cameraRepository.save(camera);
                });
    }

    /**
     * Delete a camera.
     */
    @Transactional
    public boolean deleteCamera(Long id) {
        if (cameraRepository.existsById(id)) {
            cameraRepository.deleteById(id);
            log.info("Camera deleted: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Update camera status.
     */
    @Transactional
    public Optional<Camera> updateStatus(Long id, Camera.CameraStatus status) {
        return cameraRepository.findById(id)
                .map(camera -> {
                    camera.setStatus(status);
                    return cameraRepository.save(camera);
                });
    }
}
