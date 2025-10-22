package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.RestorationShareSubmission;
import com.artivisi.hsm.simulator.entity.KeyRestorationRequest;
import com.artivisi.hsm.simulator.entity.KeyShare;
import com.artivisi.hsm.simulator.entity.CeremonyCustodian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestorationShareSubmissionRepository extends JpaRepository<RestorationShareSubmission, UUID> {

    List<RestorationShareSubmission> findByKeyRestorationRequest(KeyRestorationRequest keyRestorationRequest);

    List<RestorationShareSubmission> findByKeyRestorationRequestId(UUID keyRestorationRequestId);

    Optional<RestorationShareSubmission> findByKeyShare(KeyShare keyShare);

    Optional<RestorationShareSubmission> findByKeyShareId(UUID keyShareId);

    List<RestorationShareSubmission> findByCeremonyCustodian(CeremonyCustodian ceremonyCustodian);

    List<RestorationShareSubmission> findByCeremonyCustodianId(UUID ceremonyCustodianId);

    List<RestorationShareSubmission> findByVerificationStatus(String verificationStatus);

    @Query("SELECT r FROM RestorationShareSubmission r WHERE r.keyRestorationRequest.id = :requestId " +
           "AND r.verificationStatus = :status")
    List<RestorationShareSubmission> findByRequestAndStatus(
        @Param("requestId") UUID requestId,
        @Param("status") String status);

    @Query("SELECT r FROM RestorationShareSubmission r WHERE r.verificationStatus = 'PENDING' " +
           "AND r.verifiedAt IS NULL")
    List<RestorationShareSubmission> findPendingVerification();

    @Query("SELECT COUNT(r) FROM RestorationShareSubmission r " +
           "WHERE r.keyRestorationRequest.id = :requestId AND r.verificationStatus = 'VERIFIED'")
    long countVerifiedSubmissionsByRequest(@Param("requestId") UUID requestId);

    @Query("SELECT COUNT(r) FROM RestorationShareSubmission r " +
           "WHERE r.keyRestorationRequest.id = :requestId")
    long countSubmissionsByRequest(@Param("requestId") UUID requestId);

    @Query("SELECT r.verificationStatus, COUNT(r) FROM RestorationShareSubmission r " +
           "GROUP BY r.verificationStatus")
    List<Object[]> getVerificationStatusDistribution();

    boolean existsByKeyRestorationRequestAndKeyShare(
        KeyRestorationRequest keyRestorationRequest,
        KeyShare keyShare);
}
