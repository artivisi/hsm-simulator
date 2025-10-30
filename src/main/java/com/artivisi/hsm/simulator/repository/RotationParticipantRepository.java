package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.KeyRotationHistory;
import com.artivisi.hsm.simulator.entity.RotationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RotationParticipantRepository extends JpaRepository<RotationParticipant, UUID> {

    List<RotationParticipant> findByRotation(KeyRotationHistory rotation);

    List<RotationParticipant> findByRotationAndUpdateStatus(
        KeyRotationHistory rotation,
        RotationParticipant.UpdateStatus status
    );

    long countByRotationAndUpdateStatus(
        KeyRotationHistory rotation,
        RotationParticipant.UpdateStatus status
    );

    List<RotationParticipant> findByTerminal_Id(UUID terminalId);
}
