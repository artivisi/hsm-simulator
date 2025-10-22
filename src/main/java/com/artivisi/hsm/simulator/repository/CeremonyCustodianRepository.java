package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.CeremonyCustodian;
import com.artivisi.hsm.simulator.entity.KeyCeremony;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CeremonyCustodianRepository extends JpaRepository<CeremonyCustodian, UUID> {

    Optional<CeremonyCustodian> findByContributionToken(String contributionToken);

    List<CeremonyCustodian> findByKeyCeremony(KeyCeremony keyCeremony);

    List<CeremonyCustodian> findByKeyCeremonyAndContributionStatus(KeyCeremony keyCeremony, CeremonyCustodian.ContributionStatus contributionStatus);

    long countByKeyCeremonyAndContributionStatus(KeyCeremony keyCeremony, CeremonyCustodian.ContributionStatus contributionStatus);

    @Query("SELECT cc FROM CeremonyCustodian cc WHERE cc.keyCeremony.id = ?1")
    List<CeremonyCustodian> findByKeyCeremonyId(UUID keyCeremonyId);

    @Query("SELECT cc FROM CeremonyCustodian cc WHERE cc.keyCeremony.id = ?1 AND cc.contributionStatus = ?2")
    List<CeremonyCustodian> findByKeyCeremonyIdAndContributionStatus(UUID keyCeremonyId, CeremonyCustodian.ContributionStatus contributionStatus);

    @Query("SELECT COUNT(cc) FROM CeremonyCustodian cc WHERE cc.keyCeremony.id = ?1 AND cc.contributionStatus = 'CONTRIBUTED'")
    long countContributionsByCeremony(UUID keyCeremonyId);

    @Query("SELECT cc FROM CeremonyCustodian cc WHERE cc.keyCeremony.id = ?1 AND cc.keyCustodian.id = ?2")
    Optional<CeremonyCustodian> findByKeyCeremonyIdAndKeyCustodianId(UUID keyCeremonyId, UUID keyCustodianId);

    boolean existsByContributionToken(String contributionToken);
}