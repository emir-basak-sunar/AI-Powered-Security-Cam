package com.sentry.repository;

import com.sentry.entity.Camera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Camera entity operations.
 */
@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {

    Optional<Camera> findByName(String name);

    List<Camera> findByStatus(Camera.CameraStatus status);

    boolean existsByName(String name);
}
