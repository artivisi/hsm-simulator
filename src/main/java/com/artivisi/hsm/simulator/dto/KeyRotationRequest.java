package com.artivisi.hsm.simulator.dto;

import com.artivisi.hsm.simulator.entity.KeyRotationHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for initiating key rotation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyRotationRequest {

    /**
     * ID of the key to be rotated (old key)
     */
    private UUID keyId;

    /**
     * Type of rotation
     */
    private KeyRotationHistory.RotationType rotationType;

    /**
     * Reason for rotation
     */
    private String reason;

    /**
     * Grace period in hours before old key is revoked (default: 24 hours)
     */
    @Builder.Default
    private Integer gracePeriodHours = 24;

    /**
     * Whether to auto-complete rotation when all participants confirm (default: true)
     */
    @Builder.Default
    private Boolean autoComplete = true;
}
