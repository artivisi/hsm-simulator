package com.artivisi.hsm.simulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity storing custodian passphrase contributions with security metadata.
 */
@Entity
@Table(name = "passphrase_contributions", indexes = {
    @Index(name = "idx_contributions_ceremony_custodian", columnList = "id_ceremony_custodian"),
    @Index(name = "idx_contributions_timestamp", columnList = "contributed_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassphraseContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contribution_id", nullable = false, unique = true, length = 50)
    private String contributionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ceremony_custodian", nullable = false, foreignKey = @ForeignKey(name = "fk_contribution_ceremony_custodian"))
    private CeremonyCustodian ceremonyCustodian;

    @Column(name = "passphrase_hash", nullable = false)
    private String passphraseHash;

    @Column(name = "passphrase_entropy_score", nullable = false, precision = 3, scale = 1)
    private BigDecimal passphraseEntropyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "passphrase_strength", nullable = false, length = 20)
    private PassphraseStrength passphraseStrength;

    @Column(name = "passphrase_length", nullable = false)
    private Integer passphraseLength;

    @Column(name = "contribution_fingerprint", nullable = false)
    private String contributionFingerprint;

    @CreatedDate
    @Column(name = "contributed_at", nullable = false, updatable = false)
    private LocalDateTime contributedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    public enum PassphraseStrength {
        WEAK, FAIR, GOOD, STRONG, VERY_STRONG
    }
}
