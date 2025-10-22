package com.artivisi.hsm.simulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing the complete history of key rotations for compliance and audit.
 */
@Entity
@Table(name = "key_rotation_history", indexes = {
    @Index(name = "idx_rotation_history_old_key", columnList = "id_old_key"),
    @Index(name = "idx_rotation_history_new_key", columnList = "id_new_key"),
    @Index(name = "idx_rotation_history_status", columnList = "rotation_status"),
    @Index(name = "idx_rotation_history_started_at", columnList = "rotation_started_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyRotationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rotation_id", nullable = false, unique = true, length = 50)
    private String rotationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_old_key", nullable = false, foreignKey = @ForeignKey(name = "fk_rotation_old_key"))
    private MasterKey oldKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_new_key", nullable = false, foreignKey = @ForeignKey(name = "fk_rotation_new_key"))
    private MasterKey newKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "rotation_type", nullable = false, length = 50)
    private RotationType rotationType;

    @Column(name = "rotation_reason", nullable = false, columnDefinition = "TEXT")
    private String rotationReason;

    @Column(name = "rotation_initiated_by", nullable = false, length = 100)
    private String rotationInitiatedBy;

    @Column(name = "rotation_approved_by", length = 100)
    private String rotationApprovedBy;

    @CreatedDate
    @Column(name = "rotation_started_at", nullable = false, updatable = false)
    private LocalDateTime rotationStartedAt;

    @Column(name = "rotation_completed_at")
    private LocalDateTime rotationCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rotation_status", nullable = false, length = 50)
    @Builder.Default
    private RotationStatus rotationStatus = RotationStatus.IN_PROGRESS;

    @Column(name = "affected_terminals_count")
    @Builder.Default
    private Integer affectedTerminalsCount = 0;

    @Column(name = "affected_banks_count")
    @Builder.Default
    private Integer affectedBanksCount = 0;

    @Column(name = "rollback_required")
    @Builder.Default
    private Boolean rollbackRequired = false;

    @Column(name = "rollback_completed_at")
    private LocalDateTime rollbackCompletedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum RotationType {
        SCHEDULED, EMERGENCY, COMPLIANCE, COMPROMISE, EXPIRATION
    }

    public enum RotationStatus {
        IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK, CANCELLED
    }
}
