package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.config.CryptoConstants;
import com.artivisi.hsm.simulator.dto.*;
import com.artivisi.hsm.simulator.entity.*;
import com.artivisi.hsm.simulator.repository.*;
import com.artivisi.hsm.simulator.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing cryptographic key rotation with participant tracking.
 * Supports pending state until all participants have updated their keys.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeyRotationService {

    private final MasterKeyRepository masterKeyRepository;
    private final KeyRotationHistoryRepository rotationHistoryRepository;
    private final RotationParticipantRepository participantRepository;
    private final TerminalRepository terminalRepository;
    private final BankRepository bankRepository;
    private final KeyOperationService keyOperationService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Initiate key rotation for a master key.
     * Creates new key and rotation tracking record with PENDING status.
     */
    @Transactional
    public KeyRotationResponse initiateRotation(KeyRotationRequest request, String initiatedBy) {
        log.info("Initiating key rotation for key: {}, type: {}, reason: {}",
                request.getKeyId(), request.getRotationType(), request.getReason());

        // Get old key
        MasterKey oldKey = masterKeyRepository.findById(request.getKeyId())
                .orElseThrow(() -> new IllegalArgumentException("Key not found: " + request.getKeyId()));

        if (oldKey.getStatus() != MasterKey.KeyStatus.ACTIVE) {
            throw new IllegalStateException("Can only rotate ACTIVE keys. Current status: " + oldKey.getStatus());
        }

        // Generate new key based on key type
        MasterKey newKey = generateReplacementKey(oldKey);

        // Create rotation history record
        String rotationIdString = generateRotationId(oldKey.getKeyType());
        KeyRotationHistory rotation = KeyRotationHistory.builder()
                .rotationId(rotationIdString)
                .oldKey(oldKey)
                .newKey(newKey)
                .rotationType(request.getRotationType())
                .rotationReason(request.getReason())
                .rotationInitiatedBy(initiatedBy)
                .rotationStatus(KeyRotationHistory.RotationStatus.IN_PROGRESS)
                .rotationStartedAt(LocalDateTime.now())
                .build();

        rotation = rotationHistoryRepository.save(rotation);

        // Identify and register participants
        List<RotationParticipant> participants = identifyParticipants(oldKey, rotation);
        participantRepository.saveAll(participants);

        rotation.setAffectedTerminalsCount((int) participants.stream()
                .filter(p -> p.getParticipantType() == RotationParticipant.ParticipantType.TERMINAL)
                .count());
        rotation.setAffectedBanksCount((int) participants.stream()
                .filter(p -> p.getParticipantType() == RotationParticipant.ParticipantType.BANK)
                .count());

        rotation = rotationHistoryRepository.save(rotation);

        // Link new key to old key for rotation tracking
        newKey.setRotatedFromKeyId(oldKey.getId());
        masterKeyRepository.save(newKey);

        log.info("Rotation initiated successfully. Rotation ID: {}, Participants: {}",
                rotationIdString, participants.size());

        return buildRotationResponse(rotation);
    }

    /**
     * Terminal requests new key during rotation.
     * Returns encrypted new key for secure delivery.
     */
    @Transactional
    public TerminalKeyUpdateResponse getKeyUpdateForTerminal(TerminalKeyUpdateRequest request) {
        log.info("Terminal {} requesting key update for rotation: {}",
                request.getTerminalId(), request.getRotationId());

        Terminal terminal = terminalRepository.findByTerminalId(request.getTerminalId())
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found: " + request.getTerminalId()));

        // Find rotation for this terminal
        KeyRotationHistory rotation;
        if (request.getRotationId() != null) {
            rotation = rotationHistoryRepository.findById(request.getRotationId())
                    .orElseThrow(() -> new IllegalArgumentException("Rotation not found: " + request.getRotationId()));
        } else {
            // Find latest IN_PROGRESS rotation for this terminal
            rotation = findLatestPendingRotationForTerminal(terminal);
        }

        if (rotation.getRotationStatus() != KeyRotationHistory.RotationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Rotation is not in progress: " + rotation.getRotationStatus());
        }

        // Find participant record
        RotationParticipant participant = participantRepository.findByRotation(rotation).stream()
                .filter(p -> p.getTerminal() != null && p.getTerminal().getId().equals(terminal.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Terminal not involved in this rotation: " + request.getTerminalId()));

        if (participant.getUpdateStatus() == RotationParticipant.UpdateStatus.CONFIRMED) {
            throw new IllegalStateException("Terminal already updated: " + request.getTerminalId());
        }

        // Verify current key checksum if provided
        if (request.getCurrentKeyChecksum() != null) {
            String expectedChecksum = rotation.getOldKey().getKeyChecksum().substring(0, 16);
            if (!expectedChecksum.equalsIgnoreCase(request.getCurrentKeyChecksum())) {
                log.warn("Key checksum mismatch for terminal {}: expected {}, got {}",
                        request.getTerminalId(), expectedChecksum, request.getCurrentKeyChecksum());
                participant.setFailureReason("Key checksum mismatch");
                participant.setUpdateStatus(RotationParticipant.UpdateStatus.FAILED);
                participantRepository.save(participant);
                throw new IllegalArgumentException("Current key checksum does not match");
            }
        }

        // Encrypt new key under current (old) terminal key for secure delivery
        String encryptedNewKey = encryptKeyForDelivery(
                rotation.getNewKey().getKeyData(),
                rotation.getOldKey().getKeyData()
        );

        // Update participant status
        participant.setUpdateStatus(RotationParticipant.UpdateStatus.DELIVERED);
        participant.setNewKeyDeliveredAt(LocalDateTime.now());
        participant.setDeliveryAttempts(participant.getDeliveryAttempts() + 1);
        participant.setLastDeliveryAttempt(LocalDateTime.now());
        participantRepository.save(participant);

        log.info("New key delivered to terminal: {}", request.getTerminalId());

        return TerminalKeyUpdateResponse.builder()
                .rotationId(rotation.getId())
                .newKeyId(rotation.getNewKey().getMasterKeyId())
                .keyType(rotation.getNewKey().getKeyType().toString())
                .encryptedNewKey(encryptedNewKey)
                .newKeyChecksum(rotation.getNewKey().getKeyChecksum().substring(0, 16))
                .gracePeriodEndsAt(calculateGracePeriodEnd(rotation).toString())
                .message("New key delivered successfully. Please install and confirm.")
                .build();
    }

    /**
     * Terminal confirms successful key installation.
     */
    @Transactional
    public KeyRotationResponse confirmKeyUpdate(UUID rotationId, String terminalId, String confirmedBy) {
        log.info("Terminal {} confirming key update for rotation: {}", terminalId, rotationId);

        KeyRotationHistory rotation = rotationHistoryRepository.findById(rotationId)
                .orElseThrow(() -> new IllegalArgumentException("Rotation not found: " + rotationId));

        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found: " + terminalId));

        // Find participant record
        RotationParticipant participant = participantRepository.findByRotation(rotation).stream()
                .filter(p -> p.getTerminal() != null && p.getTerminal().getId().equals(terminal.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Terminal not involved in this rotation: " + terminalId));

        if (participant.getUpdateStatus() == RotationParticipant.UpdateStatus.CONFIRMED) {
            log.warn("Terminal {} already confirmed update for rotation {}", terminalId, rotationId);
            return buildRotationResponse(rotation);
        }

        // Mark as confirmed
        participant.setUpdateStatus(RotationParticipant.UpdateStatus.CONFIRMED);
        participant.setUpdateConfirmedAt(LocalDateTime.now());
        participant.setUpdateConfirmedBy(confirmedBy);
        participantRepository.save(participant);

        log.info("Terminal {} confirmed key update successfully", terminalId);

        // Check if all participants have confirmed
        checkAndCompleteRotation(rotation);

        return buildRotationResponse(rotation);
    }

    /**
     * Get rotation status with participant details.
     */
    @Transactional(readOnly = true)
    public KeyRotationResponse getRotationStatus(UUID rotationId) {
        KeyRotationHistory rotation = rotationHistoryRepository.findById(rotationId)
                .orElseThrow(() -> new IllegalArgumentException("Rotation not found: " + rotationId));

        return buildRotationResponse(rotation);
    }

    /**
     * Manually complete rotation (revoke old key).
     * Typically called after grace period or when all participants confirmed.
     */
    @Transactional
    public KeyRotationResponse completeRotation(UUID rotationId, String completedBy) {
        log.info("Manually completing rotation: {} by: {}", rotationId, completedBy);

        KeyRotationHistory rotation = rotationHistoryRepository.findById(rotationId)
                .orElseThrow(() -> new IllegalArgumentException("Rotation not found: " + rotationId));

        if (rotation.getRotationStatus() == KeyRotationHistory.RotationStatus.COMPLETED) {
            throw new IllegalStateException("Rotation already completed");
        }

        // Check pending participants
        long pendingCount = participantRepository.countByRotationAndUpdateStatus(
                rotation, RotationParticipant.UpdateStatus.PENDING);

        if (pendingCount > 0) {
            log.warn("Completing rotation with {} pending participants", pendingCount);
        }

        // Mark old key as ROTATED
        MasterKey oldKey = rotation.getOldKey();
        oldKey.setStatus(MasterKey.KeyStatus.ROTATED);
        oldKey.setRevokedAt(LocalDateTime.now());
        oldKey.setRevocationReason("Rotated to " + rotation.getNewKey().getMasterKeyId());
        masterKeyRepository.save(oldKey);

        // Update rotation status
        rotation.setRotationStatus(KeyRotationHistory.RotationStatus.COMPLETED);
        rotation.setRotationCompletedAt(LocalDateTime.now());
        rotation.setRotationApprovedBy(completedBy);
        rotationHistoryRepository.save(rotation);

        log.info("Rotation completed successfully. Old key {} marked as ROTATED", oldKey.getMasterKeyId());

        return buildRotationResponse(rotation);
    }

    /**
     * Rollback rotation (revert to old key, revoke new key).
     */
    @Transactional
    public KeyRotationResponse rollbackRotation(UUID rotationId, String reason, String rolledBackBy) {
        log.info("Rolling back rotation: {}, reason: {}", rotationId, reason);

        KeyRotationHistory rotation = rotationHistoryRepository.findById(rotationId)
                .orElseThrow(() -> new IllegalArgumentException("Rotation not found: " + rotationId));

        if (rotation.getRotationStatus() == KeyRotationHistory.RotationStatus.COMPLETED) {
            throw new IllegalStateException("Cannot rollback completed rotation");
        }

        // Revoke new key
        MasterKey newKey = rotation.getNewKey();
        newKey.setStatus(MasterKey.KeyStatus.REVOKED);
        newKey.setRevokedAt(LocalDateTime.now());
        newKey.setRevocationReason("Rotation rollback: " + reason);
        masterKeyRepository.save(newKey);

        // Ensure old key is ACTIVE
        MasterKey oldKey = rotation.getOldKey();
        oldKey.setStatus(MasterKey.KeyStatus.ACTIVE);
        masterKeyRepository.save(oldKey);

        // Update rotation status
        rotation.setRotationStatus(KeyRotationHistory.RotationStatus.ROLLED_BACK);
        rotation.setRollbackRequired(true);
        rotation.setRollbackCompletedAt(LocalDateTime.now());
        rotation.setNotes("Rollback reason: " + reason + " | Rolled back by: " + rolledBackBy);
        rotationHistoryRepository.save(rotation);

        log.info("Rotation rolled back successfully");

        return buildRotationResponse(rotation);
    }

    // ===== Helper Methods =====

    /**
     * Generate replacement key based on old key type.
     */
    private MasterKey generateReplacementKey(MasterKey oldKey) {
        switch (oldKey.getKeyType()) {
            case TMK:
                if (oldKey.getIdBank() == null) {
                    throw new IllegalStateException("Old TMK has no bank association");
                }
                return keyOperationService.generateTMK(
                        oldKey.getIdBank(),
                        oldKey.getKeySize(),
                        "Rotation replacement for " + oldKey.getMasterKeyId()
                );

            case TPK:
                if (oldKey.getParentKeyId() == null || oldKey.getIdTerminal() == null) {
                    throw new IllegalStateException("Old TPK missing parent or terminal");
                }
                return keyOperationService.generateTPK(
                        oldKey.getParentKeyId(),
                        oldKey.getIdTerminal(),
                        "Rotation replacement for " + oldKey.getMasterKeyId()
                );

            case TSK:
                if (oldKey.getParentKeyId() == null || oldKey.getIdTerminal() == null) {
                    throw new IllegalStateException("Old TSK missing parent or terminal");
                }
                return keyOperationService.generateTSK(
                        oldKey.getParentKeyId(),
                        oldKey.getIdTerminal(),
                        "Rotation replacement for " + oldKey.getMasterKeyId()
                );

            case ZMK:
                if (oldKey.getIdBank() == null) {
                    throw new IllegalStateException("Old ZMK has no bank association");
                }
                return keyOperationService.generateZMK(
                        oldKey.getIdBank(),
                        oldKey.getKeySize(),
                        "Rotation replacement for " + oldKey.getMasterKeyId()
                );

            default:
                throw new IllegalArgumentException("Key rotation not supported for type: " + oldKey.getKeyType());
        }
    }

    /**
     * Identify participants that need to be notified/updated for this rotation.
     */
    private List<RotationParticipant> identifyParticipants(MasterKey oldKey, KeyRotationHistory rotation) {
        switch (oldKey.getKeyType()) {
            case TMK:
                // All terminals using this TMK need TPK/TSK updates
                return terminalRepository.findByBankId(oldKey.getIdBank()).stream()
                        .filter(t -> t.getStatus() == Terminal.TerminalStatus.ACTIVE)
                        .map(terminal -> RotationParticipant.builder()
                                .rotation(rotation)
                                .terminal(terminal)
                                .participantType(RotationParticipant.ParticipantType.TERMINAL)
                                .updateStatus(RotationParticipant.UpdateStatus.PENDING)
                                .build())
                        .toList();

            case TPK:
            case TSK:
                // Single terminal affected
                Terminal terminal = terminalRepository.findById(oldKey.getIdTerminal())
                        .orElseThrow(() -> new IllegalStateException("Terminal not found for key"));
                return List.of(RotationParticipant.builder()
                        .rotation(rotation)
                        .terminal(terminal)
                        .participantType(RotationParticipant.ParticipantType.TERMINAL)
                        .updateStatus(RotationParticipant.UpdateStatus.PENDING)
                        .build());

            case ZMK:
                // Bank-to-bank key - affected bank needs notification
                Bank bank = bankRepository.findById(oldKey.getIdBank())
                        .orElseThrow(() -> new IllegalStateException("Bank not found for key"));
                return List.of(RotationParticipant.builder()
                        .rotation(rotation)
                        .bank(bank)
                        .participantType(RotationParticipant.ParticipantType.BANK)
                        .updateStatus(RotationParticipant.UpdateStatus.PENDING)
                        .build());

            default:
                return List.of();
        }
    }

    /**
     * Encrypt new key under old key for secure delivery to terminal.
     */
    private String encryptKeyForDelivery(byte[] newKeyData, byte[] oldKeyData) {
        try {
            // Derive operational key from old master key for encryption
            String context = "KEY_DELIVERY:ROTATION";
            byte[] encryptionKey = CryptoUtils.deriveKeyFromParent(oldKeyData, context, 128);

            // Generate random IV
            byte[] iv = new byte[CryptoConstants.CBC_IV_BYTES];
            secureRandom.nextBytes(iv);

            // Encrypt new key
            Cipher cipher = Cipher.getInstance(CryptoConstants.PIN_CIPHER);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, CryptoConstants.MASTER_KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(newKeyData);

            // Prepend IV
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return CryptoUtils.bytesToHex(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt key for delivery", e);
        }
    }

    /**
     * Check if all participants have confirmed and auto-complete if enabled.
     */
    private void checkAndCompleteRotation(KeyRotationHistory rotation) {
        long pendingCount = participantRepository.countByRotationAndUpdateStatus(
                rotation, RotationParticipant.UpdateStatus.PENDING);
        long deliveredCount = participantRepository.countByRotationAndUpdateStatus(
                rotation, RotationParticipant.UpdateStatus.DELIVERED);

        if (pendingCount == 0 && deliveredCount == 0) {
            // All participants confirmed, auto-complete rotation
            log.info("All participants confirmed for rotation {}. Auto-completing...", rotation.getId());

            MasterKey oldKey = rotation.getOldKey();
            oldKey.setStatus(MasterKey.KeyStatus.ROTATED);
            oldKey.setRevokedAt(LocalDateTime.now());
            oldKey.setRevocationReason("Rotated to " + rotation.getNewKey().getMasterKeyId());
            masterKeyRepository.save(oldKey);

            rotation.setRotationStatus(KeyRotationHistory.RotationStatus.COMPLETED);
            rotation.setRotationCompletedAt(LocalDateTime.now());
            rotationHistoryRepository.save(rotation);

            log.info("Rotation auto-completed successfully");
        }
    }

    /**
     * Find latest pending rotation for a terminal.
     */
    private KeyRotationHistory findLatestPendingRotationForTerminal(Terminal terminal) {
        List<RotationParticipant> participants = participantRepository.findByTerminal_Id(terminal.getId());

        return participants.stream()
                .filter(p -> p.getRotation().getRotationStatus() == KeyRotationHistory.RotationStatus.IN_PROGRESS)
                .filter(p -> p.getUpdateStatus() == RotationParticipant.UpdateStatus.PENDING ||
                             p.getUpdateStatus() == RotationParticipant.UpdateStatus.DELIVERED)
                .map(RotationParticipant::getRotation)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pending rotation found for terminal: " + terminal.getTerminalId()));
    }

    /**
     * Calculate grace period end time.
     */
    private LocalDateTime calculateGracePeriodEnd(KeyRotationHistory rotation) {
        // Default 24 hours from now
        return LocalDateTime.now().plusHours(24);
    }

    /**
     * Generate rotation ID string.
     */
    private String generateRotationId(KeyType keyType) {
        return String.format("ROT-%s-%s",
                keyType.toString(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    /**
     * Build rotation response DTO with current status.
     */
    private KeyRotationResponse buildRotationResponse(KeyRotationHistory rotation) {
        long total = participantRepository.findByRotation(rotation).size();
        long pending = participantRepository.countByRotationAndUpdateStatus(
                rotation, RotationParticipant.UpdateStatus.PENDING);
        long confirmed = participantRepository.countByRotationAndUpdateStatus(
                rotation, RotationParticipant.UpdateStatus.CONFIRMED);
        long failed = participantRepository.countByRotationAndUpdateStatus(
                rotation, RotationParticipant.UpdateStatus.FAILED);

        return KeyRotationResponse.builder()
                .rotationId(rotation.getId())
                .rotationIdString(rotation.getRotationId())
                .oldKeyId(rotation.getOldKey().getMasterKeyId())
                .newKeyId(rotation.getNewKey().getMasterKeyId())
                .rotationType(rotation.getRotationType())
                .rotationStatus(rotation.getRotationStatus())
                .rotationStartedAt(rotation.getRotationStartedAt())
                .totalParticipants((int) total)
                .pendingParticipants((int) pending)
                .confirmedParticipants((int) confirmed)
                .failedParticipants((int) failed)
                .message(buildStatusMessage(rotation.getRotationStatus(), pending, total))
                .build();
    }

    /**
     * Build human-readable status message.
     */
    private String buildStatusMessage(KeyRotationHistory.RotationStatus status, long pending, long total) {
        switch (status) {
            case IN_PROGRESS:
                if (pending == 0) {
                    return "All participants confirmed. Rotation ready to complete.";
                } else {
                    return String.format("Rotation in progress. %d of %d participants pending.", pending, total);
                }
            case COMPLETED:
                return "Rotation completed successfully. Old key has been rotated.";
            case FAILED:
                return "Rotation failed. Old key remains active.";
            case ROLLED_BACK:
                return "Rotation rolled back. Old key remains active, new key revoked.";
            case CANCELLED:
                return "Rotation cancelled.";
            default:
                return "Unknown status";
        }
    }
}
