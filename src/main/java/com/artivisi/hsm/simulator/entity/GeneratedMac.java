package com.artivisi.hsm.simulator.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a generated MAC (Message Authentication Code)
 */
@Entity
@Table(name = "generated_macs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GeneratedMac {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "message_length", nullable = false)
    private Integer messageLength;

    @Column(name = "mac_value", nullable = false, length = 64)
    private String macValue;

    @Column(name = "mac_algorithm", nullable = false, length = 50)
    private String macAlgorithm;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mac_key", nullable = false)
    private MasterKey macKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MacStatus status = MacStatus.ACTIVE;

    @Column(name = "verification_attempts")
    @Builder.Default
    private Integer verificationAttempts = 0;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    public enum MacStatus {
        ACTIVE,
        EXPIRED,
        REVOKED
    }
}
