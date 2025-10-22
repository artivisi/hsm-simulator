package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.KeyShare;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.entity.CeremonyCustodian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KeyShareRepository extends JpaRepository<KeyShare, UUID> {

    Optional<KeyShare> findByShareId(String shareId);

    List<KeyShare> findByMasterKey(MasterKey masterKey);

    List<KeyShare> findByMasterKeyId(UUID masterKeyId);

    List<KeyShare> findByCeremonyCustodian(CeremonyCustodian ceremonyCustodian);

    List<KeyShare> findByCeremonyCustodianId(UUID ceremonyCustodianId);

    Optional<KeyShare> findByMasterKeyAndShareIndex(MasterKey masterKey, Integer shareIndex);

    List<KeyShare> findByUsedInRestoration(Boolean usedInRestoration);

    @Query("SELECT k FROM KeyShare k WHERE k.masterKey.id = :masterKeyId AND k.usedInRestoration = false")
    List<KeyShare> findUnusedSharesByMasterKey(@Param("masterKeyId") UUID masterKeyId);

    @Query("SELECT k FROM KeyShare k WHERE k.distributedAt IS NULL")
    List<KeyShare> findPendingDistribution();

    @Query("SELECT COUNT(k) FROM KeyShare k WHERE k.masterKey.id = :masterKeyId")
    long countSharesByMasterKey(@Param("masterKeyId") UUID masterKeyId);

    boolean existsByShareId(String shareId);
}
