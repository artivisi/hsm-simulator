package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.*;
import com.artivisi.hsm.simulator.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyCeremonyService {

    private final MasterKeyRepository masterKeyRepository;
    private final KeyCustodianRepository keyCustodianRepository;
    private final KeyCeremonyRepository keyCeremonyRepository;
    private final CeremonyCustodianRepository ceremonyCustodianRepository;

    /**
     * Check if there's an existing active master key in the database
     */
    public boolean hasActiveMasterKey() {
        return masterKeyRepository.findActiveMasterKey().isPresent();
    }

    /**
     * Get the current active master key if exists
     */
    public MasterKey getActiveMasterKey() {
        return masterKeyRepository.findActiveMasterKey().orElse(null);
    }

    /**
     * Check if key ceremony should be initiated
     */
    public boolean shouldInitiateKeyCeremony() {
        // Check if there are enough active custodians
        long activeCustodians = keyCustodianRepository.countActiveCustodians();
        boolean hasActiveMasterKey = hasActiveMasterKey();

        log.info("Checking key ceremony initiation requirements: {} active custodians, has active master key: {}",
                activeCustodians, hasActiveMasterKey);

        return activeCustodians >= 3 && !hasActiveMasterKey;
    }

    /**
     * Initiate a new key ceremony process
     */
    @Transactional
    public KeyCeremony initiateKeyCeremony(String ceremonyName, String purpose, String createdBy) {
        log.info("Initiating key ceremony: {} by {}", ceremonyName, createdBy);

        // Get active custodians
        List<KeyCustodian> activeCustodians = keyCustodianRepository.findByStatus(KeyCustodian.CustodianStatus.ACTIVE);

        if (activeCustodians.size() < 3) {
            throw new IllegalStateException("Need at least 3 active custodians to initiate key ceremony. Found: " + activeCustodians.size());
        }

        // Create ceremony
        String ceremonyId = generateCeremonyId();
        KeyCeremony ceremony = KeyCeremony.builder()
                .ceremonyId(ceremonyId)
                .ceremonyName(ceremonyName)
                .purpose(purpose)
                .ceremonyType(KeyCeremony.CeremonyType.INITIALIZATION)
                .status(KeyCeremony.CeremonyStatus.PENDING)
                .numberOfCustodians(3)
                .threshold(2)
                .algorithm("AES-256")
                .keySize(256)
                .contributionDeadline(LocalDateTime.now().plusHours(24))
                .createdBy(createdBy)
                .build();

        ceremony = keyCeremonyRepository.save(ceremony);
        log.info("Created key ceremony with ID: {}", ceremonyId);

        // Assign custodians to ceremony
        assignCustodiansToCeremony(ceremony, activeCustodians.subList(0, 3));

        // Update ceremony status
        ceremony.setStatus(KeyCeremony.CeremonyStatus.AWAITING_CONTRIBUTIONS);
        keyCeremonyRepository.save(ceremony);

        log.info("Key ceremony {} initiated successfully with 3 custodians", ceremonyId);
        return ceremony;
    }

    /**
     * Assign custodians to a ceremony and generate contribution tokens
     */
    @Transactional
    private void assignCustodiansToCeremony(KeyCeremony ceremony, List<KeyCustodian> custodians) {
        for (int i = 0; i < custodians.size(); i++) {
            KeyCustodian custodian = custodians.get(i);
            String token = generateContributionToken(ceremony.getCeremonyId(), (char) ('A' + i));
            String contributionLink = generateContributionLink(token);

            CeremonyCustodian ceremonyCustodian = CeremonyCustodian.builder()
                    .keyCeremony(ceremony)
                    .keyCustodian(custodian)
                    .custodianOrder(i + 1)
                    .custodianLabel(String.valueOf((char) ('A' + i)))
                    .contributionToken(token)
                    .contributionLink(contributionLink)
                    .invitationSentAt(LocalDateTime.now())
                    .contributionStatus(CeremonyCustodian.ContributionStatus.PENDING)
                    .build();

            ceremonyCustodianRepository.save(ceremonyCustodian);

            // Log token information (since we don't have email system yet)
            log.info("CONTRIBUTION LINK FOR CUSTODIAN {} ({})",
                    custodian.getFullName(),
                    custodian.getEmail());
            log.info("Contribution Link: {}", contributionLink);
            log.info("Contribution Token: {}", token);
            log.info("Custodian Label: {}", String.valueOf((char) ('A' + i)));
            log.info("--------------------------------------------------------");
        }
    }

    /**
     * Generate unique ceremony ID
     */
    private String generateCeremonyId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return "CER-" + timestamp;
    }

    /**
     * Generate contribution token for custodian
     */
    private String generateContributionToken(String ceremonyId, char custodianLabel) {
        return UUID.randomUUID().toString() + "-" + ceremonyId + "-" + custodianLabel;
    }

    /**
     * Generate contribution link URL
     */
    private String generateContributionLink(String token) {
        return "http://localhost:8080/key-ceremony/contribute/" + token;
    }

    /**
     * Get ceremony details with custodian information
     */
    public KeyCeremony getCeremonyWithDetails(String ceremonyId) {
        return keyCeremonyRepository.findByCeremonyId(ceremonyId)
                .orElseThrow(() -> new IllegalArgumentException("Ceremony not found: " + ceremonyId));
    }

    /**
     * Get all active ceremonies
     */
    public List<KeyCeremony> getActiveCeremonies() {
        return keyCeremonyRepository.findActiveCeremonies();
    }

    /**
     * Get ceremony with custodian contributions
     */
    @Transactional(readOnly = true)
    public KeyCeremony getCeremonyWithContributions(String ceremonyId) {
        KeyCeremony ceremony = getCeremonyWithDetails(ceremonyId);
        // Load custodians if needed
        return ceremony;
    }

    /**
     * Get all custodians for a ceremony with their contribution status
     */
    public List<CeremonyCustodian> getCeremonyCustodians(UUID ceremonyId) {
        return ceremonyCustodianRepository.findByKeyCeremonyId(ceremonyId);
    }

    /**
     * Check if ceremony can be completed (all contributions received)
     */
    public boolean canCompleteCeremony(UUID ceremonyId) {
        long contributedCount = ceremonyCustodianRepository.countContributionsByCeremony(ceremonyId);
        return contributedCount >= 2; // Threshold is 2-of-3
    }

    /**
     * Get contribution count for a ceremony
     */
    public long getContributionCount(UUID ceremonyId) {
        return ceremonyCustodianRepository.countContributionsByCeremony(ceremonyId);
    }

    /**
     * Update ceremony status
     */
    @Transactional
    public void updateCeremonyStatus(KeyCeremony ceremony) {
        keyCeremonyRepository.save(ceremony);
    }
}