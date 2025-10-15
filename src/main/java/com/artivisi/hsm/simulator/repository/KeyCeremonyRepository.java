package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.KeyCeremony;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KeyCeremonyRepository extends JpaRepository<KeyCeremony, UUID> {

    Optional<KeyCeremony> findByCeremonyId(String ceremonyId);

    List<KeyCeremony> findByStatus(KeyCeremony.CeremonyStatus status);

    List<KeyCeremony> findByCeremonyType(KeyCeremony.CeremonyType ceremonyType);

    @Query("SELECT k FROM KeyCeremony k WHERE k.status IN ('PENDING', 'AWAITING_CONTRIBUTIONS', 'PARTIAL_CONTRIBUTIONS') ORDER BY k.startedAt DESC")
    List<KeyCeremony> findActiveCeremonies();

    @Query("SELECT k FROM KeyCeremony k WHERE k.ceremonyType = 'INITIALIZATION' AND k.status = 'COMPLETED' ORDER BY k.completedAt DESC")
    List<KeyCeremony> findCompletedInitializationCeremonies();

    boolean existsByCeremonyId(String ceremonyId);

    @Query("SELECT COUNT(k) FROM KeyCeremony k WHERE k.status = 'COMPLETED' AND k.ceremonyType = 'INITIALIZATION'")
    long countCompletedInitializationCeremonies();
}