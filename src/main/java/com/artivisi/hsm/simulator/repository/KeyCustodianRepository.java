package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.KeyCustodian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KeyCustodianRepository extends JpaRepository<KeyCustodian, UUID> {

    Optional<KeyCustodian> findByCustodianId(String custodianId);

    Optional<KeyCustodian> findByEmail(String email);

    List<KeyCustodian> findByStatus(KeyCustodian.CustodianStatus status);

    @Query("SELECT COUNT(k) FROM KeyCustodian k WHERE k.status = 'ACTIVE'")
    long countActiveCustodians();

    boolean existsByEmail(String email);

    boolean existsByCustodianId(String custodianId);
}