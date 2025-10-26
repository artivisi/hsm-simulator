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
 * Entity representing generated master keys with metadata and encryption details.
 */
@Entity
@Table(name = "master_keys", indexes = {
    @Index(name = "idx_master_keys_ceremony", columnList = "id_key_ceremony"),
    @Index(name = "idx_master_keys_status", columnList = "status"),
    @Index(name = "idx_master_keys_fingerprint", columnList = "key_fingerprint")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "master_key_id", nullable = false, unique = true, length = 50)
    private String masterKeyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_key_ceremony", nullable = true, foreignKey = @ForeignKey(name = "fk_master_key_ceremony"))
    private KeyCeremony keyCeremony;

    @Column(name = "parent_key_id")
    private UUID parentKeyId;

    @Column(name = "rotated_from_key_id")
    private UUID rotatedFromKeyId;

    @Column(name = "key_type", nullable = false, length = 50)
    @Builder.Default
    private String keyType = "HSM_MASTER_KEY";

    @Column(nullable = false, length = 50)
    private String algorithm;

    @Column(name = "key_size", nullable = false)
    private Integer keySize;

    @Column(name = "key_data_encrypted", nullable = false, columnDefinition = "bytea")
    private byte[] keyDataEncrypted;

    @Column(name = "key_fingerprint", nullable = false)
    private String keyFingerprint;

    @Column(name = "key_checksum", nullable = false)
    private String keyChecksum;

    @Column(name = "combined_entropy_hash", nullable = true)
    private String combinedEntropyHash;

    @Column(name = "generation_method", nullable = false, length = 50)
    @Builder.Default
    private String generationMethod = "PBKDF2";

    @Column(name = "kdf_iterations", nullable = false)
    @Builder.Default
    private Integer kdfIterations = 100000;

    @Column(name = "kdf_salt", nullable = false)
    private String kdfSalt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private KeyStatus status = KeyStatus.ACTIVE;

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    public enum KeyStatus {
        ACTIVE, ROTATED, REVOKED, EXPIRED
    }
}
