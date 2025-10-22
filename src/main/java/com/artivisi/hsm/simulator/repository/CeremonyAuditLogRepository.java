package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.CeremonyAuditLog;
import com.artivisi.hsm.simulator.entity.KeyCeremony;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CeremonyAuditLogRepository extends JpaRepository<CeremonyAuditLog, UUID> {

    List<CeremonyAuditLog> findByKeyCeremony(KeyCeremony keyCeremony);

    List<CeremonyAuditLog> findByKeyCeremonyId(UUID keyCeremonyId);

    List<CeremonyAuditLog> findByEventType(String eventType);

    List<CeremonyAuditLog> findByEventCategory(String eventCategory);

    List<CeremonyAuditLog> findByEventStatus(String eventStatus);

    List<CeremonyAuditLog> findByEventSeverity(String eventSeverity);

    List<CeremonyAuditLog> findByActorId(String actorId);

    @Query("SELECT c FROM CeremonyAuditLog c WHERE c.keyCeremony.id = :ceremonyId " +
           "AND c.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.createdAt DESC")
    List<CeremonyAuditLog> findByCeremonyAndDateRange(
        @Param("ceremonyId") UUID ceremonyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM CeremonyAuditLog c WHERE c.eventSeverity IN :severities " +
           "ORDER BY c.createdAt DESC")
    List<CeremonyAuditLog> findBySeverities(@Param("severities") List<String> severities);

    @Query("SELECT c FROM CeremonyAuditLog c WHERE c.keyCeremony.id = :ceremonyId " +
           "AND c.eventCategory = :category " +
           "ORDER BY c.createdAt DESC")
    List<CeremonyAuditLog> findByCeremonyAndCategory(
        @Param("ceremonyId") UUID ceremonyId,
        @Param("category") String category);

    @Query("SELECT COUNT(c) FROM CeremonyAuditLog c WHERE c.keyCeremony.id = :ceremonyId " +
           "AND c.eventStatus = :status")
    long countByCeremonyAndStatus(
        @Param("ceremonyId") UUID ceremonyId,
        @Param("status") String status);

    @Query("SELECT c.eventCategory, COUNT(c) FROM CeremonyAuditLog c " +
           "WHERE c.keyCeremony.id = :ceremonyId GROUP BY c.eventCategory")
    List<Object[]> getCategoryDistributionByCeremony(@Param("ceremonyId") UUID ceremonyId);
}
