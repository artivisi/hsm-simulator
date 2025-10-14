package com.artivisi.hsm.simulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity storing aggregated statistics for ceremony monitoring and reporting.
 */
@Entity
@Table(name = "ceremony_statistics", indexes = {
    @Index(name = "idx_stats_ceremony", columnList = "id_key_ceremony", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CeremonyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_key_ceremony", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_statistics_ceremony"))
    private KeyCeremony keyCeremony;

    @Column(name = "total_custodians", nullable = false)
    @Builder.Default
    private Integer totalCustodians = 0;

    @Column(name = "contributions_received", nullable = false)
    @Builder.Default
    private Integer contributionsReceived = 0;

    @Column(name = "contributions_pending", nullable = false)
    @Builder.Default
    private Integer contributionsPending = 0;

    @Column(name = "shares_generated", nullable = false)
    @Builder.Default
    private Integer sharesGenerated = 0;

    @Column(name = "shares_distributed", nullable = false)
    @Builder.Default
    private Integer sharesDistributed = 0;

    @Column(name = "average_contribution_time_minutes")
    private Integer averageContributionTimeMinutes;

    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column(name = "ceremony_completion_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ceremonyCompletionPercentage = BigDecimal.ZERO;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @LastModifiedDate
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;
}
