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
 * Entity representing key ceremonies that track the key generation lifecycle.
 * A key ceremony is the process of securely generating cryptographic keys with multiple custodians.
 */
@Entity
@Table(name = "key_ceremonies", indexes = {
    @Index(name = "idx_ceremony_status", columnList = "status"),
    @Index(name = "idx_ceremony_type", columnList = "ceremony_type"),
    @Index(name = "idx_ceremony_created_at", columnList = "started_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyCeremony {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ceremony_id", nullable = false, unique = true, length = 50)
    private String ceremonyId;

    @Column(name = "ceremony_name", nullable = false)
    private String ceremonyName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "ceremony_type", nullable = false, length = 50)
    private CeremonyType ceremonyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private CeremonyStatus status = CeremonyStatus.PENDING;

    @Column(name = "number_of_custodians", nullable = false)
    @Builder.Default
    private Integer numberOfCustodians = 3;

    @Column(nullable = false)
    @Builder.Default
    private Integer threshold = 2;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String algorithm = "AES-256";

    @Column(name = "key_size", nullable = false)
    @Builder.Default
    private Integer keySize = 256;

    @Column(name = "contribution_deadline")
    private LocalDateTime contributionDeadline;

    @CreatedDate
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    public enum CeremonyType {
        INITIALIZATION, RESTORATION, KEY_ROTATION
    }

    public enum CeremonyStatus {
        PENDING, 
        AWAITING_CONTRIBUTIONS, 
        PARTIAL_CONTRIBUTIONS, 
        GENERATING_KEY, 
        COMPLETED, 
        CANCELLED, 
        EXPIRED
    }
}
