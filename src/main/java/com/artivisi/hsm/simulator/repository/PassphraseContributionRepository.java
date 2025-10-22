package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.PassphraseContribution;
import com.artivisi.hsm.simulator.entity.CeremonyCustodian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PassphraseContributionRepository extends JpaRepository<PassphraseContribution, UUID> {

    Optional<PassphraseContribution> findByContributionId(String contributionId);

    Optional<PassphraseContribution> findByCeremonyCustodian(CeremonyCustodian ceremonyCustodian);

    Optional<PassphraseContribution> findByCeremonyCustodianId(UUID ceremonyCustodianId);

    List<PassphraseContribution> findByPassphraseStrength(String passphraseStrength);

    @Query("SELECT p FROM PassphraseContribution p WHERE p.passphraseEntropyScore >= :minScore")
    List<PassphraseContribution> findByMinEntropyScore(@Param("minScore") Double minScore);

    @Query("SELECT p FROM PassphraseContribution p WHERE p.contributedAt BETWEEN :startDate AND :endDate")
    List<PassphraseContribution> findByContributionDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(p.passphraseEntropyScore) FROM PassphraseContribution p")
    Double getAverageEntropyScore();

    @Query("SELECT p.passphraseStrength, COUNT(p) FROM PassphraseContribution p GROUP BY p.passphraseStrength")
    List<Object[]> getStrengthDistribution();

    boolean existsByContributionId(String contributionId);
}
