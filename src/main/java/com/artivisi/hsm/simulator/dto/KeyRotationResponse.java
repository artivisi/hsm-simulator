package com.artivisi.hsm.simulator.dto;

import com.artivisi.hsm.simulator.entity.KeyRotationHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for key rotation operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyRotationResponse {

    private UUID rotationId;
    private String rotationIdString;
    private String oldKeyId;
    private String newKeyId;
    private KeyRotationHistory.RotationType rotationType;
    private KeyRotationHistory.RotationStatus rotationStatus;
    private LocalDateTime rotationStartedAt;
    private Integer totalParticipants;
    private Integer pendingParticipants;
    private Integer confirmedParticipants;
    private Integer failedParticipants;
    private String message;
}
