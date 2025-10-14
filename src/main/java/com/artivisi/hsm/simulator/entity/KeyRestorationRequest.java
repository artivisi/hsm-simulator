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
 * Entity tracking key restoration ceremony requests.
 */
@Entity
@Table(name = "key_restoration_requests", indexes = {
    @Index(name = "idx_restoration_original_ceremony", columnList = "id_key_ceremony_original"),
    @Index(name = "idx_restoration_master_key", columnList = "id_master_key"),
    @Index(name = "idx_restoration_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyRestorationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "restoration_id", nullable = false, unique = true, length = 50)
    private String restorationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_key_ceremony_original", nullable = false, foreignKey = @ForeignKey(name = "fk_restoration_original_ceremony"))
    private KeyCeremony keyCeremonyOriginal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_master_key", nullable = false, foreignKey = @ForeignKey(name = "fk_restoration_master_key"))
    private MasterKey masterKey;

    @Column(name = "restoration_reason", nullable = false, columnDefinition = "TEXT")
    private String restorationReason;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @CreatedDate
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RestorationStatus status = RestorationStatus.PENDING;

    @Column(name = "shares_required", nullable = false)
    private Integer sharesRequired;

    @Column(name = "shares_submitted", nullable = false)
    @Builder.Default
    private Integer sharesSubmitted = 0;

    @Column(name = "restoration_completed_at")
    private LocalDateTime restorationCompletedAt;

    @Column(name = "restored_key_verified")
    @Builder.Default
    private Boolean restoredKeyVerified = false;

    public enum RestorationStatus {
        PENDING, APPROVED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED
    }
}
