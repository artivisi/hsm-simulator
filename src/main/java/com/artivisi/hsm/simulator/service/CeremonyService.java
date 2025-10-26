package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.*;
import com.artivisi.hsm.simulator.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing key ceremony lifecycle including creation, contribution tracking,
 * and master key generation orchestration.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CeremonyService {

    private final KeyCeremonyRepository ceremonyRepository;
    private final CeremonyCustodianRepository ceremonyCustodianRepository;
    private final KeyCustodianRepository custodianRepository;
    private final PassphraseContributionRepository contributionRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final KeyShareRepository shareRepository;
    private final CeremonyAuditLogRepository auditLogRepository;

    private final PassphraseService passphraseService;
    private final KeyGenerationService keyGenerationService;
    private final EmailService emailService;

    /**
     * Creates a new key ceremony with selected custodians.
     */
    @Transactional
    public KeyCeremony createCeremony(CeremonyCreationRequest request) {
        log.info("Creating new ceremony: {}", request.getCeremonyName());

        // Validate custodians
        if (request.getCustodianIds().size() != request.getNumberOfCustodians()) {
            throw new IllegalArgumentException("Number of selected custodians does not match expected count");
        }

        List<KeyCustodian> custodians = custodianRepository.findAllById(request.getCustodianIds());
        if (custodians.size() != request.getCustodianIds().size()) {
            throw new IllegalArgumentException("One or more custodians not found");
        }

        // Check all custodians are active
        long inactiveCustodians = custodians.stream()
                .filter(c -> c.getStatus() != KeyCustodian.CustodianStatus.ACTIVE)
                .count();
        if (inactiveCustodians > 0) {
            throw new IllegalArgumentException("All custodians must be in ACTIVE status");
        }

        // Generate unique ceremony ID
        String ceremonyId = generateCeremonyId();

        // Create ceremony
        KeyCeremony ceremony = KeyCeremony.builder()
                .ceremonyId(ceremonyId)
                .ceremonyName(request.getCeremonyName())
                .purpose(request.getPurpose())
                .ceremonyType(request.getCeremonyType())
                .status(KeyCeremony.CeremonyStatus.PENDING)
                .numberOfCustodians(request.getNumberOfCustodians())
                .threshold(request.getThreshold())
                .algorithm(request.getAlgorithm())
                .keySize(request.getKeySize())
                .contributionDeadline(request.getContributionDeadline())
                .createdBy(request.getCreatedBy())
                .build();

        ceremony = ceremonyRepository.save(ceremony);

        // Create ceremony-custodian associations with unique tokens
        int order = 1;
        for (KeyCustodian custodian : custodians) {
            String token = UUID.randomUUID().toString();
            String contributionLink = generateContributionLink(token);

            CeremonyCustodian ceremonyCustodian = CeremonyCustodian.builder()
                    .keyCeremony(ceremony)
                    .keyCustodian(custodian)
                    .custodianOrder(order)
                    .custodianLabel("Custodian " + (char) ('A' + order - 1))
                    .contributionToken(token)
                    .contributionLink(contributionLink)
                    .contributionStatus(CeremonyCustodian.ContributionStatus.PENDING)
                    .build();

            ceremonyCustodian = ceremonyCustodianRepository.save(ceremonyCustodian);

            // Send invitation email to custodian
            sendInvitationEmail(ceremonyCustodian, ceremony);

            order++;
        }

        // Update ceremony status to AWAITING_CONTRIBUTIONS
        ceremony.setStatus(KeyCeremony.CeremonyStatus.AWAITING_CONTRIBUTIONS);
        ceremonyRepository.save(ceremony);

        // Create audit log
        createAuditLog(ceremony, "CEREMONY_CREATED", "Ceremony created with " + custodians.size() + " custodians", request.getCreatedBy());

        log.info("Ceremony created successfully: {} ({})", ceremonyId, ceremony.getId());
        return ceremony;
    }

    /**
     * Submits a custodian's passphrase contribution.
     */
    @Transactional
    public PassphraseContribution submitContribution(String token, ContributionSubmissionRequest request) {
        log.info("Processing contribution for token: {}", token.substring(0, 8) + "...");

        // Find ceremony custodian by token
        CeremonyCustodian ceremonyCustodian = ceremonyCustodianRepository.findByContributionToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired contribution token"));

        // Validate ceremony status
        KeyCeremony ceremony = ceremonyCustodian.getKeyCeremony();
        if (ceremony.getStatus() != KeyCeremony.CeremonyStatus.AWAITING_CONTRIBUTIONS &&
            ceremony.getStatus() != KeyCeremony.CeremonyStatus.PARTIAL_CONTRIBUTIONS) {
            throw new IllegalStateException("Ceremony is not accepting contributions");
        }

        // Check if already contributed
        if (ceremonyCustodian.getContributionStatus() == CeremonyCustodian.ContributionStatus.CONTRIBUTED) {
            throw new IllegalStateException("Contribution already submitted");
        }

        // Check deadline
        if (ceremony.getContributionDeadline() != null &&
            LocalDateTime.now().isAfter(ceremony.getContributionDeadline())) {
            throw new IllegalStateException("Contribution deadline has passed");
        }

        // Validate passphrase strength
        PassphraseService.PassphraseValidationResult validation =
                passphraseService.validatePassphrase(request.getPassphrase());

        if (!validation.isValid()) {
            throw new IllegalArgumentException("Passphrase validation failed: " + validation.getErrorMessage());
        }

        // Hash passphrase
        String passphraseHash = passphraseService.hashPassphrase(request.getPassphrase());
        String contributionFingerprint = passphraseService.generateContributionFingerprint(passphraseHash);

        // Create contribution record
        PassphraseContribution contribution = PassphraseContribution.builder()
                .contributionId(generateContributionId(ceremony, ceremonyCustodian))
                .ceremonyCustodian(ceremonyCustodian)
                .passphraseHash(passphraseHash)
                .passphraseEntropyScore(validation.getEntropyScore())
                .passphraseStrength(validation.getStrength())
                .passphraseLength(validation.getLength())
                .contributionFingerprint(contributionFingerprint)
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();

        contribution = contributionRepository.save(contribution);

        // Update ceremony custodian status
        ceremonyCustodian.setContributionStatus(CeremonyCustodian.ContributionStatus.CONTRIBUTED);
        ceremonyCustodian.setContributedAt(LocalDateTime.now());
        ceremonyCustodianRepository.save(ceremonyCustodian);

        // Check if threshold met
        long contributionsCount = ceremonyCustodianRepository.countByKeyCeremonyAndContributionStatus(
                ceremony, CeremonyCustodian.ContributionStatus.CONTRIBUTED);

        if (contributionsCount == 1) {
            ceremony.setStatus(KeyCeremony.CeremonyStatus.PARTIAL_CONTRIBUTIONS);
            ceremonyRepository.save(ceremony);
        }

        // Create audit log
        createAuditLog(ceremony, "CONTRIBUTION_RECEIVED",
                "Contribution received from " + ceremonyCustodian.getCustodianLabel() +
                " (strength: " + validation.getStrength() + ", entropy: " + validation.getEntropyScore() + ")",
                ceremonyCustodian.getKeyCustodian().getFullName());

        log.info("Contribution accepted. Total contributions: {}/{}", contributionsCount, ceremony.getThreshold());

        return contribution;
    }

    /**
     * Generates the master key once threshold is met.
     */
    @Transactional
    public MasterKey generateMasterKey(UUID ceremonyId, String initiatedBy) {
        log.info("Generating master key for ceremony: {}", ceremonyId);

        KeyCeremony ceremony = ceremonyRepository.findById(ceremonyId)
                .orElseThrow(() -> new IllegalArgumentException("Ceremony not found"));

        // Validate status
        if (ceremony.getStatus() != KeyCeremony.CeremonyStatus.AWAITING_CONTRIBUTIONS &&
            ceremony.getStatus() != KeyCeremony.CeremonyStatus.PARTIAL_CONTRIBUTIONS) {
            throw new IllegalStateException("Ceremony is not in a valid state for key generation");
        }

        // Check threshold
        long contributionsCount = ceremonyCustodianRepository.countByKeyCeremonyAndContributionStatus(
                ceremony, CeremonyCustodian.ContributionStatus.CONTRIBUTED);

        if (contributionsCount < ceremony.getThreshold()) {
            throw new IllegalStateException(
                    String.format("Insufficient contributions: need %d, have %d", ceremony.getThreshold(), contributionsCount));
        }

        // Update status
        ceremony.setStatus(KeyCeremony.CeremonyStatus.GENERATING_KEY);
        ceremonyRepository.save(ceremony);

        try {
            // Get all passphrase contributions
            List<CeremonyCustodian> ceremonyCustodians = ceremonyCustodianRepository
                    .findByKeyCeremonyAndContributionStatus(ceremony, CeremonyCustodian.ContributionStatus.CONTRIBUTED);

            List<String> passphraseHashes = ceremonyCustodians.stream()
                    .map(cc -> contributionRepository.findByCeremonyCustodian(cc))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(PassphraseContribution::getPassphraseHash)
                    .collect(Collectors.toList());

            // Generate salt
            byte[] salt = keyGenerationService.generateSalt();

            // Derive master key
            KeyGenerationService.MasterKeyResult keyResult = keyGenerationService.deriveMasterKey(
                    passphraseHashes, salt, ceremony.getKeySize() == 256 ? 100000 : 50000);

            // Create MasterKey entity
            String masterKeyId = generateMasterKeyId(ceremony);
            MasterKey masterKey = MasterKey.builder()
                    .masterKeyId(masterKeyId)
                    .keyCeremony(ceremony)
                    .keyType("HSM_MASTER_KEY")
                    .algorithm(ceremony.getAlgorithm())
                    .keySize(ceremony.getKeySize())
                    .keyDataEncrypted(keyResult.getKeyData()) // In production, encrypt this
                    .keyFingerprint(keyResult.getFingerprint())
                    .keyChecksum(keyResult.getChecksum())
                    .combinedEntropyHash(keyResult.getCombinedEntropyHash())
                    .generationMethod("PBKDF2")
                    .kdfIterations(100000)
                    .kdfSalt(bytesToHex(salt))
                    .status(MasterKey.KeyStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now())
                    .build();

            masterKey = masterKeyRepository.save(masterKey);

            // Generate Shamir shares
            List<KeyGenerationService.ShamirShare> shamirShares = keyGenerationService.createShamirShares(
                    keyResult.getKeyData(),
                    ceremony.getNumberOfCustodians(),
                    ceremony.getThreshold()
            );

            // Create KeyShare entities for each custodian
            int shareIndex = 0;
            for (CeremonyCustodian ceremonyCustodian : ceremonyCustodianRepository
                    .findByKeyCeremony(ceremony)) {

                KeyGenerationService.ShamirShare shamirShare = shamirShares.get(shareIndex);

                // Generate encryption key for this custodian's share
                byte[] encryptionKey = deriveShareEncryptionKey(ceremonyCustodian, ceremony);

                // Encrypt the share
                byte[] encryptedShareData = keyGenerationService.encryptShare(shamirShare, encryptionKey);

                // Calculate verification hash
                String verificationHash = keyGenerationService.generateShareVerificationHash(encryptedShareData);

                String shareId = generateShareId(ceremony, ceremonyCustodian);

                KeyShare keyShare = KeyShare.builder()
                        .shareId(shareId)
                        .masterKey(masterKey)
                        .ceremonyCustodian(ceremonyCustodian)
                        .shareIndex(shamirShare.getShareIndex())
                        .shareDataEncrypted(encryptedShareData)
                        .shareVerificationHash(verificationHash)
                        .polynomialDegree(ceremony.getThreshold() - 1)
                        .primeModulus(shamirShare.getPrime().toString(16))
                        .distributionMethod(KeyShare.DistributionMethod.EMAIL)
                        .usedInRestoration(false)
                        .build();

                shareRepository.save(keyShare);
                shareIndex++;
            }

            // Update ceremony status
            ceremony.setStatus(KeyCeremony.CeremonyStatus.COMPLETED);
            ceremony.setCompletedAt(LocalDateTime.now());
            ceremony.setLastModifiedBy(initiatedBy);
            ceremonyRepository.save(ceremony);

            // Create audit log
            createAuditLog(ceremony, "KEY_GENERATED",
                    String.format("Master key generated successfully. Fingerprint: %s", keyResult.getFingerprint()),
                    initiatedBy);

            log.info("Master key generated successfully: {}", masterKeyId);
            return masterKey;

        } catch (Exception e) {
            log.error("Error generating master key", e);
            ceremony.setStatus(KeyCeremony.CeremonyStatus.PARTIAL_CONTRIBUTIONS);
            ceremonyRepository.save(ceremony);
            throw new RuntimeException("Failed to generate master key: " + e.getMessage(), e);
        }
    }

    /**
     * Gets ceremony status and contribution progress.
     */
    public CeremonyStatusResponse getCeremonyStatus(UUID ceremonyId) {
        KeyCeremony ceremony = ceremonyRepository.findById(ceremonyId)
                .orElseThrow(() -> new IllegalArgumentException("Ceremony not found"));

        List<CeremonyCustodian> ceremonyCustodians = ceremonyCustodianRepository.findByKeyCeremony(ceremony);

        long contributedCount = ceremonyCustodians.stream()
                .filter(cc -> cc.getContributionStatus() == CeremonyCustodian.ContributionStatus.CONTRIBUTED)
                .count();

        boolean thresholdMet = contributedCount >= ceremony.getThreshold();

        List<CustodianStatusInfo> custodianStatuses = ceremonyCustodians.stream()
                .map(cc -> {
                    PassphraseContribution contribution = contributionRepository.findByCeremonyCustodian(cc).orElse(null);
                    return CustodianStatusInfo.builder()
                            .custodianName(cc.getKeyCustodian().getFullName())
                            .custodianLabel(cc.getCustodianLabel())
                            .contributionStatus(cc.getContributionStatus())
                            .contributedAt(cc.getContributedAt())
                            .contributionToken(cc.getContributionToken())
                            .contributionLink(cc.getContributionLink())
                            .passphraseStrength(contribution != null ? contribution.getPassphraseStrength() : null)
                            .entropyScore(contribution != null ? contribution.getPassphraseEntropyScore() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return CeremonyStatusResponse.builder()
                .ceremony(ceremony)
                .contributedCount(contributedCount)
                .requiredCount(ceremony.getThreshold())
                .totalCustodians(ceremony.getNumberOfCustodians())
                .thresholdMet(thresholdMet)
                .custodianStatuses(custodianStatuses)
                .build();
    }

    /**
     * Verifies a contribution token and returns ceremony info.
     */
    public ContributionTokenInfo verifyContributionToken(String token) {
        CeremonyCustodian ceremonyCustodian = ceremonyCustodianRepository.findByContributionToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired contribution token"));

        KeyCeremony ceremony = ceremonyCustodian.getKeyCeremony();

        // Check if already contributed
        if (ceremonyCustodian.getContributionStatus() == CeremonyCustodian.ContributionStatus.CONTRIBUTED) {
            return ContributionTokenInfo.builder()
                    .valid(false)
                    .errorMessage("You have already submitted your contribution")
                    .build();
        }

        // Check ceremony status
        if (ceremony.getStatus() != KeyCeremony.CeremonyStatus.AWAITING_CONTRIBUTIONS &&
            ceremony.getStatus() != KeyCeremony.CeremonyStatus.PARTIAL_CONTRIBUTIONS) {
            return ContributionTokenInfo.builder()
                    .valid(false)
                    .errorMessage("This ceremony is no longer accepting contributions")
                    .build();
        }

        // Check deadline
        if (ceremony.getContributionDeadline() != null &&
            LocalDateTime.now().isAfter(ceremony.getContributionDeadline())) {
            return ContributionTokenInfo.builder()
                    .valid(false)
                    .errorMessage("The contribution deadline has passed")
                    .build();
        }

        return ContributionTokenInfo.builder()
                .valid(true)
                .ceremonyName(ceremony.getCeremonyName())
                .ceremonyPurpose(ceremony.getPurpose())
                .custodianName(ceremonyCustodian.getKeyCustodian().getFullName())
                .custodianLabel(ceremonyCustodian.getCustodianLabel())
                .deadline(ceremony.getContributionDeadline())
                .build();
    }

    // ===== Private Helper Methods =====

    private void createAuditLog(KeyCeremony ceremony, String eventType, String eventDescription, String performedBy) {
        CeremonyAuditLog auditLog = CeremonyAuditLog.builder()
                .keyCeremony(ceremony)
                .eventType(eventType)
                .eventCategory(CeremonyAuditLog.EventCategory.CEREMONY)
                .eventDescription(eventDescription)
                .actorType(CeremonyAuditLog.ActorType.ADMINISTRATOR)
                .actorName(performedBy)
                .eventStatus(CeremonyAuditLog.EventStatus.SUCCESS)
                .eventSeverity(CeremonyAuditLog.EventSeverity.INFO)
                .build();
        auditLogRepository.save(auditLog);
    }

    private String generateCeremonyId() {
        return "CER-" + LocalDateTime.now().getYear() + "-" +
               String.format("%06d", new Random().nextInt(1000000));
    }

    private String generateContributionId(KeyCeremony ceremony, CeremonyCustodian ceremonyCustodian) {
        return "CONT-" + LocalDateTime.now().getYear() + "-" +
               ceremony.getCeremonyId().substring(ceremony.getCeremonyId().lastIndexOf('-') + 1) + "-" +
               ceremonyCustodian.getCustodianLabel().substring(ceremonyCustodian.getCustodianLabel().length() - 1);
    }

    private String generateMasterKeyId(KeyCeremony ceremony) {
        return "MK-" + LocalDateTime.now().getYear() + "-" +
               String.format("%03d", new Random().nextInt(1000));
    }

    private String generateShareId(KeyCeremony ceremony, CeremonyCustodian ceremonyCustodian) {
        return "KS-" + LocalDateTime.now().getYear() + "-" +
               ceremony.getCeremonyId().substring(ceremony.getCeremonyId().lastIndexOf('-') + 1) + "-" +
               ceremonyCustodian.getCustodianLabel().substring(ceremonyCustodian.getCustodianLabel().length() - 1);
    }

    private String generateContributionLink(String token) {
        // In production, use actual domain
        return "https://hsm.local/hsm/contribute/" + token;
    }

    private byte[] deriveShareEncryptionKey(CeremonyCustodian ceremonyCustodian, KeyCeremony ceremony) {
        // Derive a unique encryption key for each custodian's share
        // Using ceremony ID + custodian email as seed
        String seed = ceremony.getCeremonyId() + ":" + ceremonyCustodian.getKeyCustodian().getEmail();
        byte[] salt = keyGenerationService.generateSalt();

        try {
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(seed.toCharArray(), salt, 10000, 256);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive share encryption key", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Sends invitation email to custodian with contribution link
     */
    private void sendInvitationEmail(CeremonyCustodian ceremonyCustodian, KeyCeremony ceremony) {
        String custodianName = ceremonyCustodian.getKeyCustodian().getFullName();
        String custodianEmail = ceremonyCustodian.getKeyCustodian().getEmail();
        String custodianLabel = ceremonyCustodian.getCustodianLabel();
        String contributionLink = ceremonyCustodian.getContributionLink();

        log.info("Sending invitation email to {} ({})", custodianName, custodianEmail);

        String htmlContent = buildInvitationEmailHtml(
                custodianName,
                custodianLabel,
                ceremony.getCeremonyName(),
                ceremony.getPurpose(),
                contributionLink,
                ceremony.getContributionDeadline(),
                ceremony.getThreshold(),
                ceremony.getNumberOfCustodians()
        );

        try {
            emailService.sendHtmlEmail(
                    custodianEmail,
                    "HSM Key Ceremony Invitation - " + ceremony.getCeremonyName(),
                    htmlContent
            );

            // Update invitation sent timestamp
            ceremonyCustodian.setInvitationSentAt(LocalDateTime.now());
            ceremonyCustodianRepository.save(ceremonyCustodian);

            if (emailService.isEmailEnabled()) {
                log.info("Invitation email sent successfully to {}", custodianEmail);
            } else {
                log.info("Email sending disabled - simulated invitation to {}", custodianEmail);
            }

        } catch (Exception e) {
            log.error("Failed to send invitation email to {}: {}", custodianEmail, e.getMessage(), e);
            // Don't fail ceremony creation if email fails
        }
    }

    /**
     * Builds HTML content for invitation email
     */
    private String buildInvitationEmailHtml(String custodianName, String custodianLabel,
                                            String ceremonyName, String purpose,
                                            String contributionLink, LocalDateTime deadline,
                                            Integer threshold, Integer totalCustodians) {
        String deadlineStr = deadline != null
                ? deadline.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm"))
                : "No deadline specified";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { background: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; }
                        .info-box { background: white; padding: 20px; border-left: 4px solid #667eea; margin: 20px 0; border-radius: 4px; }
                        .button { display: inline-block; background: #667eea; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; margin: 20px 0; font-weight: bold; }
                        .button:hover { background: #5568d3; }
                        .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .footer { text-align: center; padding: 20px; color: #6b7280; font-size: 12px; }
                        .detail-row { margin: 8px 0; }
                        .detail-label { font-weight: bold; color: #4b5563; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0;">🔐 HSM Key Ceremony Invitation</h1>
                        </div>

                        <div class="content">
                            <p>Dear <strong>%s</strong> (%s),</p>

                            <p>You have been selected as a key custodian for an important HSM (Hardware Security Module) key ceremony.</p>

                            <div class="info-box">
                                <h3 style="margin-top: 0; color: #667eea;">Ceremony Information</h3>
                                <div class="detail-row">
                                    <span class="detail-label">Ceremony Name:</span> %s
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Purpose:</span> %s
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Threshold:</span> %d of %d custodians required
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Deadline:</span> %s
                                </div>
                            </div>

                            <div class="warning">
                                <strong>⚠️ Important:</strong> Your participation is critical for the security of this ceremony. Please create a strong, memorable passphrase and store it securely.
                            </div>

                            <p style="text-align: center;">
                                <a href="%s" class="button">Submit Your Contribution</a>
                            </p>

                            <p style="font-size: 12px; color: #6b7280;">
                                If the button doesn't work, copy and paste this link into your browser:<br>
                                <a href="%s" style="color: #667eea; word-break: break-all;">%s</a>
                            </p>

                            <h3>What You Need to Do:</h3>
                            <ol>
                                <li>Click the link above to access the contribution form</li>
                                <li>Create a strong passphrase (minimum 12 characters, 20+ recommended)</li>
                                <li>Confirm your passphrase and submit</li>
                                <li>Store your passphrase securely - you'll need it for future recovery operations</li>
                            </ol>

                            <div class="info-box">
                                <h3 style="margin-top: 0; color: #667eea;">Security Notes</h3>
                                <ul style="margin: 0;">
                                    <li>Do NOT share your passphrase with anyone</li>
                                    <li>Use a password manager to store it securely</li>
                                    <li>Your passphrase is NOT stored - only a cryptographic hash</li>
                                    <li>You will receive your encrypted key share after the ceremony completes</li>
                                </ul>
                            </div>
                        </div>

                        <div class="footer">
                            <p>This is an automated message from the HSM Simulator.<br>
                            For questions, please contact your system administrator.</p>
                            <p style="margin-top: 15px;">
                                <strong>ArtiVisi Intermedia</strong><br>
                                HSM Key Ceremony Management System
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                custodianName, custodianLabel,
                ceremonyName, purpose,
                threshold, totalCustodians,
                deadlineStr,
                contributionLink,
                contributionLink, contributionLink
        );
    }

    // ===== Request/Response Classes =====

    @lombok.Data
    @lombok.Builder
    public static class CeremonyCreationRequest {
        private String ceremonyName;
        private String purpose;
        private KeyCeremony.CeremonyType ceremonyType;
        private List<UUID> custodianIds;
        private Integer numberOfCustodians;
        private Integer threshold;
        private String algorithm;
        private Integer keySize;
        private LocalDateTime contributionDeadline;
        private String createdBy;
    }

    @lombok.Data
    @lombok.Builder
    public static class ContributionSubmissionRequest {
        private String passphrase;
        private String ipAddress;
        private String userAgent;
    }

    @lombok.Data
    @lombok.Builder
    public static class CeremonyStatusResponse {
        private KeyCeremony ceremony;
        private long contributedCount;
        private int requiredCount;
        private int totalCustodians;
        private boolean thresholdMet;
        private List<CustodianStatusInfo> custodianStatuses;
    }

    @lombok.Data
    @lombok.Builder
    public static class CustodianStatusInfo {
        private String custodianName;
        private String custodianLabel;
        private CeremonyCustodian.ContributionStatus contributionStatus;
        private LocalDateTime contributedAt;
        private String contributionToken;
        private String contributionLink;
        private PassphraseContribution.PassphraseStrength passphraseStrength;
        private BigDecimal entropyScore;
    }

    @lombok.Data
    @lombok.Builder
    public static class ContributionTokenInfo {
        private boolean valid;
        private String errorMessage;
        private String ceremonyName;
        private String ceremonyPurpose;
        private String custodianName;
        private String custodianLabel;
        private LocalDateTime deadline;
    }
}
