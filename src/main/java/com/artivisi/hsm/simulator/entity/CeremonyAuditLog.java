package com.artivisi.hsm.simulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity providing comprehensive audit trail for all ceremony activities.
 */
@Entity
@Table(name = "ceremony_audit_logs", indexes = {
    @Index(name = "idx_audit_ceremony", columnList = "id_key_ceremony"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_event_category", columnList = "event_category"),
    @Index(name = "idx_audit_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_actor", columnList = "actor_id"),
    @Index(name = "idx_audit_event_status", columnList = "event_status"),
    @Index(name = "idx_audit_event_severity", columnList = "event_severity")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CeremonyAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_key_ceremony", foreignKey = @ForeignKey(name = "fk_audit_ceremony"))
    private KeyCeremony keyCeremony;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_category", nullable = false, length = 50)
    private EventCategory eventCategory;

    @Column(name = "event_description", nullable = false, columnDefinition = "TEXT")
    private String eventDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 50)
    private ActorType actorType;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "target_entity_type", length = 50)
    private String targetEntityType;

    @Column(name = "target_entity_id", length = 100)
    private String targetEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false, length = 20)
    private EventStatus eventStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_severity", nullable = false, length = 20)
    @Builder.Default
    private EventSeverity eventSeverity = EventSeverity.INFO;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_metadata", columnDefinition = "jsonb")
    private Map<String, Object> eventMetadata;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EventCategory {
        CEREMONY, CONTRIBUTION, KEY_GENERATION, DISTRIBUTION, SECURITY, SYSTEM
    }

    public enum ActorType {
        ADMINISTRATOR, CUSTODIAN, SYSTEM, API
    }

    public enum EventStatus {
        SUCCESS, FAILURE, WARNING, INFO
    }

    public enum EventSeverity {
        DEBUG, INFO, WARNING, ERROR, CRITICAL
    }
}
