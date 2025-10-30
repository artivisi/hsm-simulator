package com.artivisi.hsm.simulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing individual participants in a key rotation process.
 * Tracks which terminals/banks have completed their key update.
 */
@Entity
@Table(name = "rotation_participants", indexes = {
    @Index(name = "idx_rotation_participant_rotation", columnList = "id_rotation"),
    @Index(name = "idx_rotation_participant_terminal", columnList = "id_terminal"),
    @Index(name = "idx_rotation_participant_bank", columnList = "id_bank"),
    @Index(name = "idx_rotation_participant_status", columnList = "update_status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rotation", nullable = false, foreignKey = @ForeignKey(name = "fk_rotation_participant_rotation"))
    private KeyRotationHistory rotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_terminal", foreignKey = @ForeignKey(name = "fk_rotation_participant_terminal"))
    private Terminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bank", foreignKey = @ForeignKey(name = "fk_rotation_participant_bank"))
    private Bank bank;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20)
    private ParticipantType participantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_status", nullable = false, length = 20)
    @Builder.Default
    private UpdateStatus updateStatus = UpdateStatus.PENDING;

    @Column(name = "new_key_delivered_at")
    private LocalDateTime newKeyDeliveredAt;

    @Column(name = "update_confirmed_at")
    private LocalDateTime updateConfirmedAt;

    @Column(name = "update_confirmed_by", length = 100)
    private String updateConfirmedBy;

    @Column(name = "delivery_attempts")
    @Builder.Default
    private Integer deliveryAttempts = 0;

    @Column(name = "last_delivery_attempt")
    private LocalDateTime lastDeliveryAttempt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ParticipantType {
        TERMINAL, BANK
    }

    public enum UpdateStatus {
        PENDING,           // Waiting for key delivery
        DELIVERED,         // New key sent to participant
        CONFIRMED,         // Participant confirmed installation
        FAILED,            // Update failed
        SKIPPED            // Skipped (e.g., terminal inactive)
    }
}
