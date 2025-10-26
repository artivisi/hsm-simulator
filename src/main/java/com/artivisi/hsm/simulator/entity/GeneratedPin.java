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
 * Entity for tracking generated PINs and their encrypted values
 */
@Entity
@Table(name = "generated_pins")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedPin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number", nullable = false, length = 19)
    private String accountNumber;

    @Column(name = "pin_length", nullable = false)
    private Integer pinLength;

    @Column(name = "pin_format", nullable = false, length = 20)
    private String pinFormat;

    @Column(name = "encrypted_pin_block", nullable = false, columnDefinition = "TEXT")
    private String encryptedPinBlock;

    @Column(name = "pin_verification_value", length = 10)
    private String pinVerificationValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encryption_key_id", nullable = false)
    private MasterKey encryptionKey;

    @Column(name = "clear_pin", length = 12)
    private String clearPin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PinStatus status = PinStatus.ACTIVE;

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "verification_attempts")
    @Builder.Default
    private Integer verificationAttempts = 0;

    public enum PinStatus {
        ACTIVE, BLOCKED, EXPIRED
    }
}
