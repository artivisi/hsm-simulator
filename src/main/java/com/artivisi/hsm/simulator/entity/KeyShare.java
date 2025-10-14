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
 * Entity representing Shamir's Secret Sharing key shares for master key recovery.
 */
@Entity
@Table(name = "key_shares", 
    indexes = {
        @Index(name = "idx_key_shares_master_key", columnList = "id_master_key"),
        @Index(name = "idx_key_shares_custodian", columnList = "id_ceremony_custodian"),
        @Index(name = "idx_key_shares_used", columnList = "used_in_restoration")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_master_key_share_index", columnNames = {"id_master_key", "share_index"}),
        @UniqueConstraint(name = "uk_master_key_custodian", columnNames = {"id_master_key", "id_ceremony_custodian"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "share_id", nullable = false, unique = true, length = 50)
    private String shareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_master_key", nullable = false, foreignKey = @ForeignKey(name = "fk_key_share_master_key"))
    private MasterKey masterKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ceremony_custodian", nullable = false, foreignKey = @ForeignKey(name = "fk_key_share_ceremony_custodian"))
    private CeremonyCustodian ceremonyCustodian;

    @Column(name = "share_index", nullable = false)
    private Integer shareIndex;

    @Column(name = "share_data_encrypted", nullable = false, columnDefinition = "bytea")
    private byte[] shareDataEncrypted;

    @Column(name = "share_verification_hash", nullable = false)
    private String shareVerificationHash;

    @Column(name = "polynomial_degree", nullable = false)
    private Integer polynomialDegree;

    @Column(name = "prime_modulus", columnDefinition = "TEXT")
    private String primeModulus;

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "distributed_at")
    private LocalDateTime distributedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_method", length = 50)
    @Builder.Default
    private DistributionMethod distributionMethod = DistributionMethod.EMAIL;

    @Column(name = "used_in_restoration")
    @Builder.Default
    private Boolean usedInRestoration = false;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public enum DistributionMethod {
        EMAIL, PHYSICAL, API, MANUAL
    }
}
