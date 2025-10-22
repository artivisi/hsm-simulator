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
 * Entity representing terminals (ATM, POS, etc.) for terminal key management.
 */
@Entity
@Table(name = "terminals", indexes = {
    @Index(name = "idx_terminals_bank", columnList = "id_bank"),
    @Index(name = "idx_terminals_status", columnList = "status"),
    @Index(name = "idx_terminals_type", columnList = "terminal_type")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Terminal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "terminal_id", nullable = false, unique = true, length = 50)
    private String terminalId;

    @Column(name = "terminal_name", nullable = false)
    private String terminalName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bank", nullable = false, foreignKey = @ForeignKey(name = "fk_terminal_bank"))
    private Bank bank;

    @Enumerated(EnumType.STRING)
    @Column(name = "terminal_type", nullable = false, length = 50)
    private TerminalType terminalType;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TerminalStatus status = TerminalStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum TerminalType {
        ATM, POS, MPOS, VIRTUAL, E_COMMERCE
    }

    public enum TerminalStatus {
        ACTIVE, INACTIVE, SUSPENDED, MAINTENANCE
    }
}
