package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.KeyRotationHistory;
import com.artivisi.hsm.simulator.entity.MasterKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KeyRotationHistoryRepository extends JpaRepository<KeyRotationHistory, UUID> {

    Optional<KeyRotationHistory> findByRotationId(String rotationId);

    List<KeyRotationHistory> findByOldKey(MasterKey oldKey);

    List<KeyRotationHistory> findByNewKey(MasterKey newKey);

    List<KeyRotationHistory> findByRotationStatus(KeyRotationHistory.RotationStatus rotationStatus);

    List<KeyRotationHistory> findByRotationType(KeyRotationHistory.RotationType rotationType);

    @Query("SELECT k FROM KeyRotationHistory k WHERE k.oldKey.id = :keyId OR k.newKey.id = :keyId")
    List<KeyRotationHistory> findRotationHistoryByKey(@Param("keyId") UUID keyId);

    @Query("SELECT k FROM KeyRotationHistory k WHERE k.rotationStatus = :status " +
           "AND k.rotationStartedAt < :before")
    List<KeyRotationHistory> findByStatusAndStartedBefore(
        @Param("status") KeyRotationHistory.RotationStatus status,
        @Param("before") LocalDateTime before);

    @Query("SELECT k FROM KeyRotationHistory k WHERE k.rotationType = :type " +
           "AND k.rotationStartedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY k.rotationStartedAt DESC")
    List<KeyRotationHistory> findRotationsByTypeAndDateRange(
        @Param("type") KeyRotationHistory.RotationType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT k FROM KeyRotationHistory k WHERE k.rollbackRequired = true " +
           "AND k.rollbackCompletedAt IS NULL")
    List<KeyRotationHistory> findPendingRollbacks();

    boolean existsByRotationId(String rotationId);

    @Query("SELECT COUNT(k) FROM KeyRotationHistory k WHERE k.rotationStatus = :status")
    long countByStatus(@Param("status") KeyRotationHistory.RotationStatus status);

    @Query("SELECT COUNT(k) FROM KeyRotationHistory k WHERE k.rotationType = :type " +
           "AND k.rotationStartedAt BETWEEN :startDate AND :endDate")
    long countRotationsByTypeAndDateRange(
        @Param("type") KeyRotationHistory.RotationType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
}
