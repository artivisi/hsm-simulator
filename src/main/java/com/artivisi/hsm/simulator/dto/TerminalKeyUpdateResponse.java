package com.artivisi.hsm.simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for terminal key update request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalKeyUpdateResponse {

    private UUID rotationId;
    private String newKeyId;
    private String keyType;

    /**
     * New key encrypted under current terminal key (for secure delivery)
     */
    private String encryptedNewKey;

    /**
     * New key checksum for verification
     */
    private String newKeyChecksum;

    /**
     * IV used for encryption (prepended to encryptedNewKey in hex)
     */
    private String iv;

    /**
     * Grace period end time (when old key will be revoked)
     */
    private String gracePeriodEndsAt;

    private String message;
}
