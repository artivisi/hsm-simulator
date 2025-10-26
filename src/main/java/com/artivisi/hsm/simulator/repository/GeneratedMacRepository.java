package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.GeneratedMac;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedMacRepository extends JpaRepository<GeneratedMac, UUID> {

    Page<GeneratedMac> findAll(Pageable pageable);

    long countByStatus(GeneratedMac.MacStatus status);

    Optional<GeneratedMac> findByMessageAndMacKey_Id(String message, UUID macKeyId);
}
