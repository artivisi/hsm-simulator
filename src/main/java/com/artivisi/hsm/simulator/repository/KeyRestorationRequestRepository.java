package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.KeyRestorationRequest;
import com.artivisi.hsm.simulator.entity.KeyCeremony;
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
public interface KeyRestorationRequestRepository extends JpaRepository<KeyRestorationRequest, UUID> {

    Optional<KeyRestorationRequest> findByRestorationId(String restorationId);

    List<KeyRestorationRequest> findByKeyCeremonyOriginal(KeyCeremony keyCeremonyOriginal);

    List<KeyRestorationRequest> findByKeyCeremonyOriginalId(UUID keyCeremonyOriginalId);

    List<KeyRestorationRequest> findByMasterKey(MasterKey masterKey);

    List<KeyRestorationRequest> findByMasterKeyId(UUID masterKeyId);

    List<KeyRestorationRequest> findByStatus(String status);

    List<KeyRestorationRequest> findByRequestedBy(String requestedBy);

    @Query("SELECT k FROM KeyRestorationRequest k WHERE k.status = :status " +
           "AND k.requestedAt < :before")
    List<KeyRestorationRequest> findByStatusAndRequestedBefore(
        @Param("status") String status,
        @Param("before") LocalDateTime before);

    @Query("SELECT k FROM KeyRestorationRequest k WHERE k.status IN :statuses " +
           "ORDER BY k.requestedAt DESC")
    List<KeyRestorationRequest> findByStatusIn(@Param("statuses") List<String> statuses);

    @Query("SELECT k FROM KeyRestorationRequest k WHERE k.approved = true " +
           "AND k.status != 'COMPLETED'")
    List<KeyRestorationRequest> findApprovedPendingCompletion();

    @Query("SELECT k FROM KeyRestorationRequest k WHERE k.sharesSubmitted < k.sharesRequired " +
           "AND k.status = 'IN_PROGRESS'")
    List<KeyRestorationRequest> findIncompleteSubmissions();

    @Query("SELECT COUNT(k) FROM KeyRestorationRequest k WHERE k.status = :status")
    long countByStatus(@Param("status") String status);

    boolean existsByRestorationId(String restorationId);

    @Query("SELECT k.status, COUNT(k) FROM KeyRestorationRequest k GROUP BY k.status")
    List<Object[]> getStatusDistribution();
}
