package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.dto.*;
import com.artivisi.hsm.simulator.service.KeyRotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for key rotation operations.
 * Supports ATM terminal key change requests with pending state tracking.
 */
@RestController
@RequestMapping("/api/hsm")
@Slf4j
@RequiredArgsConstructor
public class KeyRotationController {

    private final KeyRotationService keyRotationService;

    /**
     * POST /api/hsm/key/rotate
     * Initiate key rotation for a master key.
     * Creates new key and tracks all participants that need to update.
     *
     * Request body:
     * {
     *   "keyId": "uuid-of-key-to-rotate",
     *   "rotationType": "SCHEDULED",
     *   "reason": "Regular rotation schedule",
     *   "gracePeriodHours": 24,
     *   "autoComplete": true
     * }
     *
     * Response:
     * {
     *   "rotationId": "uuid",
     *   "rotationIdString": "ROT-TMK-ABC12345",
     *   "oldKeyId": "TMK-ISS001-XYZ",
     *   "newKeyId": "TMK-ISS001-ABC",
     *   "rotationType": "SCHEDULED",
     *   "rotationStatus": "IN_PROGRESS",
     *   "rotationStartedAt": "2025-10-31T10:00:00",
     *   "totalParticipants": 5,
     *   "pendingParticipants": 5,
     *   "confirmedParticipants": 0,
     *   "failedParticipants": 0,
     *   "message": "Rotation in progress. 5 of 5 participants pending."
     * }
     */
    @PostMapping("/key/rotate")
    public ResponseEntity<?> initiateRotation(
            @RequestBody KeyRotationRequest request,
            Principal principal
    ) {
        log.info("API: Initiating key rotation for key: {}, user: {}",
                request.getKeyId(), principal != null ? principal.getName() : "system");

        try {
            String initiatedBy = principal != null ? principal.getName() : "system";
            KeyRotationResponse response = keyRotationService.initiateRotation(request, initiatedBy);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initiating key rotation", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/terminal/{terminalId}/get-updated-key
     * ATM/Terminal requests new key during rotation.
     * Returns new key encrypted under current terminal key for secure delivery.
     *
     * Request body:
     * {
     *   "terminalId": "TRM-ISS001-ATM-001",
     *   "rotationId": "uuid-optional",
     *   "currentKeyChecksum": "A8FC6D4EEB350415"
     * }
     *
     * Response:
     * {
     *   "rotationId": "uuid",
     *   "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
     *   "keyType": "TPK",
     *   "encryptedNewKey": "hex-encoded-encrypted-key-with-iv",
     *   "newKeyChecksum": "B7A53E2FC461526",
     *   "gracePeriodEndsAt": "2025-11-01T10:00:00",
     *   "message": "New key delivered successfully. Please install and confirm."
     * }
     */
    @PostMapping("/terminal/{terminalId}/get-updated-key")
    public ResponseEntity<?> getUpdatedKeyForTerminal(
            @PathVariable String terminalId,
            @RequestBody(required = false) TerminalKeyUpdateRequest request
    ) {
        log.info("API: Terminal {} requesting updated key", terminalId);

        try {
            // If no request body, create default request
            if (request == null) {
                request = TerminalKeyUpdateRequest.builder()
                        .terminalId(terminalId)
                        .build();
            } else {
                request.setTerminalId(terminalId);
            }

            TerminalKeyUpdateResponse response = keyRotationService.getKeyUpdateForTerminal(request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting updated key for terminal: " + terminalId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/terminal/{terminalId}/confirm-key-update
     * Terminal confirms successful key installation.
     * Updates rotation participant status and checks if all participants completed.
     *
     * Request body:
     * {
     *   "rotationId": "uuid",
     *   "confirmedBy": "TERMINAL_APP"
     * }
     *
     * Response:
     * {
     *   "rotationId": "uuid",
     *   "rotationIdString": "ROT-TPK-ABC12345",
     *   "oldKeyId": "TPK-TRM-ISS001-ATM-001-OLD",
     *   "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
     *   "rotationType": "SCHEDULED",
     *   "rotationStatus": "IN_PROGRESS",
     *   "totalParticipants": 1,
     *   "pendingParticipants": 0,
     *   "confirmedParticipants": 1,
     *   "failedParticipants": 0,
     *   "message": "All participants confirmed. Rotation ready to complete."
     * }
     */
    @PostMapping("/terminal/{terminalId}/confirm-key-update")
    public ResponseEntity<?> confirmKeyUpdate(
            @PathVariable String terminalId,
            @RequestBody Map<String, String> request
    ) {
        log.info("API: Terminal {} confirming key update", terminalId);

        try {
            UUID rotationId = UUID.fromString(request.get("rotationId"));
            String confirmedBy = request.getOrDefault("confirmedBy", terminalId);

            KeyRotationResponse response = keyRotationService.confirmKeyUpdate(
                    rotationId, terminalId, confirmedBy);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error confirming key update for terminal: " + terminalId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/hsm/rotation/{rotationId}/status
     * Get current status of key rotation with participant details.
     *
     * Response:
     * {
     *   "rotationId": "uuid",
     *   "rotationIdString": "ROT-TMK-ABC12345",
     *   "oldKeyId": "TMK-ISS001-OLD",
     *   "newKeyId": "TMK-ISS001-NEW",
     *   "rotationType": "SCHEDULED",
     *   "rotationStatus": "IN_PROGRESS",
     *   "rotationStartedAt": "2025-10-31T10:00:00",
     *   "totalParticipants": 5,
     *   "pendingParticipants": 2,
     *   "confirmedParticipants": 3,
     *   "failedParticipants": 0,
     *   "message": "Rotation in progress. 2 of 5 participants pending."
     * }
     */
    @GetMapping("/rotation/{rotationId}/status")
    public ResponseEntity<?> getRotationStatus(@PathVariable UUID rotationId) {
        log.info("API: Getting rotation status for: {}", rotationId);

        try {
            KeyRotationResponse response = keyRotationService.getRotationStatus(rotationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting rotation status", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/rotation/{rotationId}/complete
     * Manually complete rotation and revoke old key.
     * Typically called after grace period or when all participants confirmed.
     *
     * Request body (optional):
     * {
     *   "completedBy": "admin"
     * }
     *
     * Response:
     * {
     *   "rotationId": "uuid",
     *   "rotationIdString": "ROT-TPK-ABC12345",
     *   "oldKeyId": "TPK-TRM-ISS001-ATM-001-OLD",
     *   "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
     *   "rotationType": "SCHEDULED",
     *   "rotationStatus": "COMPLETED",
     *   "rotationStartedAt": "2025-10-31T10:00:00",
     *   "totalParticipants": 1,
     *   "pendingParticipants": 0,
     *   "confirmedParticipants": 1,
     *   "failedParticipants": 0,
     *   "message": "Rotation completed successfully. Old key has been rotated."
     * }
     */
    @PostMapping("/rotation/{rotationId}/complete")
    public ResponseEntity<?> completeRotation(
            @PathVariable UUID rotationId,
            @RequestBody(required = false) Map<String, String> request,
            Principal principal
    ) {
        log.info("API: Completing rotation: {}", rotationId);

        try {
            String completedBy;
            if (request != null && request.containsKey("completedBy")) {
                completedBy = request.get("completedBy");
            } else if (principal != null) {
                completedBy = principal.getName();
            } else {
                completedBy = "system";
            }

            KeyRotationResponse response = keyRotationService.completeRotation(rotationId, completedBy);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error completing rotation", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/rotation/{rotationId}/rollback
     * Rollback rotation - revoke new key, keep old key active.
     * Use when rotation fails or needs to be aborted.
     *
     * Request body:
     * {
     *   "reason": "Terminal installation failed",
     *   "rolledBackBy": "admin"
     * }
     *
     * Response:
     * {
     *   "rotationId": "uuid",
     *   "rotationIdString": "ROT-TPK-ABC12345",
     *   "oldKeyId": "TPK-TRM-ISS001-ATM-001-OLD",
     *   "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
     *   "rotationType": "SCHEDULED",
     *   "rotationStatus": "ROLLED_BACK",
     *   "totalParticipants": 1,
     *   "pendingParticipants": 0,
     *   "confirmedParticipants": 0,
     *   "failedParticipants": 1,
     *   "message": "Rotation rolled back. Old key remains active, new key revoked."
     * }
     */
    @PostMapping("/rotation/{rotationId}/rollback")
    public ResponseEntity<?> rollbackRotation(
            @PathVariable UUID rotationId,
            @RequestBody Map<String, String> request,
            Principal principal
    ) {
        log.info("API: Rolling back rotation: {}", rotationId);

        try {
            String reason = request.getOrDefault("reason", "Manual rollback");
            String rolledBackBy;
            if (request.containsKey("rolledBackBy")) {
                rolledBackBy = request.get("rolledBackBy");
            } else if (principal != null) {
                rolledBackBy = principal.getName();
            } else {
                rolledBackBy = "system";
            }

            KeyRotationResponse response = keyRotationService.rollbackRotation(
                    rotationId, reason, rolledBackBy);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rolling back rotation", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
