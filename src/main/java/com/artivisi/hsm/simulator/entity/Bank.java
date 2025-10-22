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
 * Entity representing banks/organizations in the four-party card processing model.
 * Supports ISSUER, ACQUIRER, SWITCH, and PROCESSOR types.
 */
@Entity
@Table(name = "banks", indexes = {
    @Index(name = "idx_banks_type", columnList = "bank_type"),
    @Index(name = "idx_banks_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bank_code", nullable = false, unique = true, length = 20)
    private String bankCode;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_type", nullable = false, length = 50)
    private BankType bankType;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BankStatus status = BankStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum BankType {
        ISSUER, ACQUIRER, SWITCH, PROCESSOR
    }

    public enum BankStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
