package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.GeneratedPin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedPinRepository extends JpaRepository<GeneratedPin, UUID> {

    Optional<GeneratedPin> findByAccountNumber(String accountNumber);

    Page<GeneratedPin> findByStatus(GeneratedPin.PinStatus status, Pageable pageable);

    long countByStatus(GeneratedPin.PinStatus status);

    boolean existsByAccountNumber(String accountNumber);
}
