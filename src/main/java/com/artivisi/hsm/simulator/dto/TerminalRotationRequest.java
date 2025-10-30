package com.artivisi.hsm.simulator.dto;

import com.artivisi.hsm.simulator.entity.KeyRotationHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for terminal-initiated key rotation.
 * Terminal requests rotation of its own keys (TPK/TSK) as part of scheduled maintenance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalRotationRequest {

    /**
     * Terminal ID requesting rotation
     */
    private String terminalId;

    /**
     * Type of key to rotate (TPK or TSK)
     */
    private String keyType;

    /**
     * Reason for rotation (typically SCHEDULED for terminal-initiated)
     */
    @Builder.Default
    private KeyRotationHistory.RotationType rotationType = KeyRotationHistory.RotationType.SCHEDULED;

    /**
     * Optional description
     */
    private String description;

    /**
     * Grace period in hours before old key is revoked (default: 24 hours)
     */
    @Builder.Default
    private Integer gracePeriodHours = 24;
}
