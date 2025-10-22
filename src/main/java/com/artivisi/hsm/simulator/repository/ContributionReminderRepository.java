package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.ContributionReminder;
import com.artivisi.hsm.simulator.entity.CeremonyCustodian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContributionReminderRepository extends JpaRepository<ContributionReminder, UUID> {

    List<ContributionReminder> findByCeremonyCustodian(CeremonyCustodian ceremonyCustodian);

    List<ContributionReminder> findByCeremonyCustodianId(UUID ceremonyCustodianId);

    List<ContributionReminder> findByReminderType(String reminderType);

    List<ContributionReminder> findByEmailStatus(String emailStatus);

    @Query("SELECT c FROM ContributionReminder c WHERE c.ceremonyCustodian.id = :custodianId " +
           "ORDER BY c.sentAt DESC")
    List<ContributionReminder> findByCustodianOrderByDate(@Param("custodianId") UUID custodianId);

    @Query("SELECT c FROM ContributionReminder c WHERE c.emailStatus = :status " +
           "AND c.deliveryConfirmedAt IS NULL")
    List<ContributionReminder> findPendingConfirmation(@Param("status") String status);

    @Query("SELECT c FROM ContributionReminder c WHERE c.sentAt BETWEEN :startDate AND :endDate")
    List<ContributionReminder> findBySentDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(c) FROM ContributionReminder c WHERE c.ceremonyCustodian.id = :custodianId")
    long countByCustodian(@Param("custodianId") UUID custodianId);

    @Query("SELECT c.reminderType, COUNT(c) FROM ContributionReminder c GROUP BY c.reminderType")
    List<Object[]> getReminderTypeDistribution();

    @Query("SELECT c.emailStatus, COUNT(c) FROM ContributionReminder c GROUP BY c.emailStatus")
    List<Object[]> getEmailStatusDistribution();
}
