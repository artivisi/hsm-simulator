package com.artivisi.hsm.simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for terminal requesting key update during rotation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalKeyUpdateRequest {

    /**
     * Terminal ID requesting the update
     */
    private String terminalId;

    /**
     * Rotation ID (optional - if not provided, returns latest pending rotation)
     */
    private UUID rotationId;

    /**
     * Current key checksum for verification
     */
    private String currentKeyChecksum;
}
