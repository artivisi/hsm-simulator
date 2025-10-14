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
 * Entity tracking share submissions during restoration ceremonies.
 */
@Entity
@Table(name = "restoration_share_submissions", 
    indexes = {
        @Index(name = "idx_restoration_submissions", columnList = "id_key_restoration_request"),
        @Index(name = "idx_restoration_submissions_share", columnList = "id_key_share"),
        @Index(name = "idx_restoration_submissions_custodian", columnList = "id_ceremony_custodian")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_restoration_share", columnNames = {"id_key_restoration_request", "id_key_share"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestorationShareSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_key_restoration_request", nullable = false, foreignKey = @ForeignKey(name = "fk_submission_restoration"))
    private KeyRestorationRequest keyRestorationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_key_share", nullable = false, foreignKey = @ForeignKey(name = "fk_submission_share"))
    private KeyShare keyShare;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ceremony_custodian", nullable = false, foreignKey = @ForeignKey(name = "fk_submission_custodian"))
    private CeremonyCustodian ceremonyCustodian;

    @CreatedDate
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED
    }
}
