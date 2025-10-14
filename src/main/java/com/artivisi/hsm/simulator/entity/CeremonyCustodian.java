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
 * Entity linking custodians to ceremonies with their specific roles and contribution status.
 */
@Entity
@Table(name = "ceremony_custodians", 
    indexes = {
        @Index(name = "idx_ceremony_custodians_ceremony", columnList = "id_key_ceremony"),
        @Index(name = "idx_ceremony_custodians_custodian", columnList = "id_key_custodian"),
        @Index(name = "idx_ceremony_custodians_token", columnList = "contribution_token"),
        @Index(name = "idx_ceremony_custodians_status", columnList = "contribution_status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ceremony_custodian", columnNames = {"id_key_ceremony", "id_key_custodian"}),
        @UniqueConstraint(name = "uk_ceremony_order", columnNames = {"id_key_ceremony", "custodian_order"}),
        @UniqueConstraint(name = "uk_ceremony_label", columnNames = {"id_key_ceremony", "custodian_label"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CeremonyCustodian {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_key_ceremony", nullable = false, foreignKey = @ForeignKey(name = "fk_ceremony_custodian_ceremony"))
    private KeyCeremony keyCeremony;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_key_custodian", nullable = false, foreignKey = @ForeignKey(name = "fk_ceremony_custodian_custodian"))
    private KeyCustodian keyCustodian;

    @Column(name = "custodian_order", nullable = false)
    private Integer custodianOrder;

    @Column(name = "custodian_label", nullable = false, length = 10)
    private String custodianLabel;

    @Column(name = "contribution_token", nullable = false, unique = true)
    private String contributionToken;

    @Column(name = "contribution_link", columnDefinition = "TEXT")
    private String contributionLink;

    @Column(name = "invitation_sent_at")
    private LocalDateTime invitationSentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_status", nullable = false, length = 50)
    @Builder.Default
    private ContributionStatus contributionStatus = ContributionStatus.PENDING;

    @Column(name = "contributed_at")
    private LocalDateTime contributedAt;

    @Column(name = "share_sent_at")
    private LocalDateTime shareSentAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ContributionStatus {
        PENDING, CONTRIBUTED, EXPIRED
    }
}
