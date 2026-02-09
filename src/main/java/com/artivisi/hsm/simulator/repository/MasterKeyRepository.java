package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterKeyRepository extends JpaRepository<MasterKey, UUID> {

    Optional<MasterKey> findByMasterKeyId(String masterKeyId);

    Optional<MasterKey> findByKeyFingerprint(String keyFingerprint);

    Optional<MasterKey> findByKeyCeremonyId(UUID keyCeremonyId);

    List<MasterKey> findByStatus(MasterKey.KeyStatus status);

    List<MasterKey> findByStatusAndKeyTypeAndIdTerminal(MasterKey.KeyStatus status, KeyType keyType, UUID terminalId);

    Optional<MasterKey> findFirstByStatusAndKeyTypeAndIdBankOrderByGeneratedAtDesc(MasterKey.KeyStatus status, KeyType keyType, UUID idBank);

    @Query("SELECT m FROM MasterKey m WHERE m.status = 'ACTIVE' ORDER BY m.generatedAt DESC")
    Optional<MasterKey> findActiveMasterKey();

    boolean existsByMasterKeyId(String masterKeyId);

    boolean existsByKeyFingerprint(String keyFingerprint);

    @Query("SELECT COUNT(m) FROM MasterKey m WHERE m.status = 'ACTIVE'")
    long countActiveMasterKeys();

    // Count methods for statistics
    long countByStatus(MasterKey.KeyStatus status);

    long countByGenerationMethod(String generationMethod);
}