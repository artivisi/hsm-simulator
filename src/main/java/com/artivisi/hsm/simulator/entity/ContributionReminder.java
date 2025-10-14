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
 * Entity tracking reminder emails sent to custodians.
 */
@Entity
@Table(name = "contribution_reminders", indexes = {
    @Index(name = "idx_reminders_ceremony_custodian", columnList = "id_ceremony_custodian"),
    @Index(name = "idx_reminders_sent_at", columnList = "sent_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ceremony_custodian", nullable = false, foreignKey = @ForeignKey(name = "fk_reminder_ceremony_custodian"))
    private CeremonyCustodian ceremonyCustodian;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 50)
    private ReminderType reminderType;

    @CreatedDate
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "sent_by", length = 100)
    private String sentBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    @Builder.Default
    private EmailStatus emailStatus = EmailStatus.SENT;

    @Column(name = "delivery_confirmed_at")
    private LocalDateTime deliveryConfirmedAt;

    public enum ReminderType {
        INITIAL, FIRST_REMINDER, SECOND_REMINDER, URGENT, DEADLINE_APPROACHING
    }

    public enum EmailStatus {
        SENT, DELIVERED, BOUNCED, FAILED, OPENED
    }
}
