package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.CeremonyStatistics;
import com.artivisi.hsm.simulator.entity.KeyCeremony;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CeremonyStatisticsRepository extends JpaRepository<CeremonyStatistics, UUID> {

    Optional<CeremonyStatistics> findByKeyCeremony(KeyCeremony keyCeremony);

    Optional<CeremonyStatistics> findByKeyCeremonyId(UUID keyCeremonyId);

    @Query("SELECT AVG(c.ceremonyCompletionPercentage) FROM CeremonyStatistics c")
    BigDecimal getAverageCompletionPercentage();

    @Query("SELECT AVG(c.averageContributionTimeMinutes) FROM CeremonyStatistics c " +
           "WHERE c.averageContributionTimeMinutes IS NOT NULL")
    Double getAverageContributionTime();

    @Query("SELECT AVG(c.totalDurationMinutes) FROM CeremonyStatistics c " +
           "WHERE c.totalDurationMinutes IS NOT NULL")
    Double getAverageCeremonyDuration();

    @Query("SELECT c FROM CeremonyStatistics c WHERE c.ceremonyCompletionPercentage >= :minPercentage")
    CeremonyStatistics findByMinCompletionPercentage(@Param("minPercentage") BigDecimal minPercentage);

    @Query("SELECT SUM(c.contributionsReceived) FROM CeremonyStatistics c")
    Long getTotalContributions();

    @Query("SELECT SUM(c.sharesGenerated) FROM CeremonyStatistics c")
    Long getTotalSharesGenerated();
}
