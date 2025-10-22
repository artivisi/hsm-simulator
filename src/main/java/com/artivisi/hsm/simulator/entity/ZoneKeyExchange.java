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
 * Entity representing zone key exchanges between banks for ZMK, ZPK, and ZSK.
 */
@Entity
@Table(name = "zone_key_exchanges", indexes = {
    @Index(name = "idx_zone_exchanges_source", columnList = "id_source_bank"),
    @Index(name = "idx_zone_exchanges_destination", columnList = "id_destination_bank"),
    @Index(name = "idx_zone_exchanges_zmk", columnList = "id_zmk"),
    @Index(name = "idx_zone_exchanges_status", columnList = "exchange_status"),
    @Index(name = "idx_zone_exchanges_initiated_at", columnList = "initiated_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneKeyExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "exchange_id", nullable = false, unique = true, length = 50)
    private String exchangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_source_bank", nullable = false, foreignKey = @ForeignKey(name = "fk_zone_exchange_source_bank"))
    private Bank sourceBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_destination_bank", nullable = false, foreignKey = @ForeignKey(name = "fk_zone_exchange_destination_bank"))
    private Bank destinationBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_zmk", nullable = false, foreignKey = @ForeignKey(name = "fk_zone_exchange_zmk"))
    private MasterKey zmk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_exchanged_key", nullable = false, foreignKey = @ForeignKey(name = "fk_zone_exchange_key"))
    private MasterKey exchangedKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_type", nullable = false, length = 50)
    private ExchangeType exchangeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_transport_method", nullable = false, length = 50)
    private TransportMethod keyTransportMethod;

    @Column(name = "transport_key_fingerprint")
    private String transportKeyFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_status", nullable = false, length = 50)
    @Builder.Default
    private ExchangeStatus exchangeStatus = ExchangeStatus.INITIATED;

    @CreatedDate
    @Column(name = "initiated_at", nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "initiated_by", nullable = false, length = 100)
    private String initiatedBy;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    public enum ExchangeType {
        INITIAL, RENEWAL, EMERGENCY, ROTATION
    }

    public enum TransportMethod {
        ENCRYPTED_UNDER_ZMK, MANUAL, COURIER, SECURE_CHANNEL
    }

    public enum ExchangeStatus {
        INITIATED, IN_TRANSIT, ACKNOWLEDGED, ACTIVATED, REJECTED, EXPIRED
    }
}
