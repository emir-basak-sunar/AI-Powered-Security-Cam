package com.sentry.repository;

import com.sentry.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Alert entity operations.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByCameraId(String cameraId);

    List<Alert> findByAlertType(Alert.AlertType alertType);

    Page<Alert> findByAcknowledged(boolean acknowledged, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("since") LocalDateTime since);

    @Query("SELECT a FROM Alert a ORDER BY a.createdAt DESC")
    Page<Alert> findLatestAlerts(Pageable pageable);

    long countByAcknowledged(boolean acknowledged);

    long countByCameraId(String cameraId);
}
