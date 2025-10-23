package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.CeremonyCustodian;
import com.artivisi.hsm.simulator.entity.KeyCeremony;
import com.artivisi.hsm.simulator.entity.KeyShare;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.CeremonyCustodianRepository;
import com.artivisi.hsm.simulator.repository.KeyShareRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing key share distribution including viewing, downloading,
 * and tracking distribution status.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShareDistributionService {

    private final KeyShareRepository shareRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final CeremonyCustodianRepository ceremonyCustodianRepository;
    private final EmailService emailService;

    /**
     * Gets all shares for a ceremony with distribution status
     */
    @Transactional(readOnly = true)
    public ShareDistributionResponse getSharesForCeremony(UUID ceremonyId) {
        log.info("Getting shares for ceremony: {}", ceremonyId);

        // Find master key for ceremony
        MasterKey masterKey = masterKeyRepository.findByKeyCeremonyId(ceremonyId)
                .orElseThrow(() -> new IllegalArgumentException("Master key not found for ceremony"));

        // Get all shares
        List<KeyShare> shares = shareRepository.findByMasterKey(masterKey);

        // Build share info list
        List<ShareInfo> shareInfoList = shares.stream()
                .map(share -> {
                    CeremonyCustodian ceremonyCustodian = share.getCeremonyCustodian();
                    return ShareInfo.builder()
                            .shareId(share.getShareId())
                            .shareIndex(share.getShareIndex())
                            .custodianName(ceremonyCustodian.getKeyCustodian().getFullName())
                            .custodianLabel(ceremonyCustodian.getCustodianLabel())
                            .custodianEmail(ceremonyCustodian.getKeyCustodian().getEmail())
                            .distributedAt(share.getDistributedAt())
                            .distributionMethod(share.getDistributionMethod())
                            .verificationHash(share.getShareVerificationHash())
                            .isDistributed(share.getDistributedAt() != null)
                            .build();
                })
                .collect(Collectors.toList());

        return ShareDistributionResponse.builder()
                .masterKey(masterKey)
                .shares(shareInfoList)
                .totalShares(shares.size())
                .distributedShares(shareInfoList.stream().filter(ShareInfo::isDistributed).count())
                .build();
    }

    /**
     * Generates downloadable share data in text format
     */
    @Transactional
    public byte[] generateShareDownload(UUID shareId) {
        log.info("Generating download for share: {}", shareId);

        KeyShare share = shareRepository.findByShareId(shareId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        CeremonyCustodian ceremonyCustodian = share.getCeremonyCustodian();
        MasterKey masterKey = share.getMasterKey();
        KeyCeremony ceremony = masterKey.getKeyCeremony();

        // Build formatted text document
        StringBuilder content = new StringBuilder();
        content.append("═══════════════════════════════════════════════════════════════\n");
        content.append("           HSM KEY CEREMONY - ENCRYPTED KEY SHARE\n");
        content.append("═══════════════════════════════════════════════════════════════\n\n");

        content.append("CEREMONY INFORMATION\n");
        content.append("─────────────────────────────────────────────────────────────\n");
        content.append(String.format("Ceremony Name:     %s\n", ceremony.getCeremonyName()));
        content.append(String.format("Ceremony ID:       %s\n", ceremony.getCeremonyId()));
        content.append(String.format("Completed:         %s\n", formatDateTime(ceremony.getCompletedAt())));
        content.append(String.format("Algorithm:         %s\n", ceremony.getAlgorithm()));
        content.append(String.format("Key Size:          %d bits\n", ceremony.getKeySize()));
        content.append("\n");

        content.append("MASTER KEY INFORMATION\n");
        content.append("─────────────────────────────────────────────────────────────\n");
        content.append(String.format("Master Key ID:     %s\n", masterKey.getMasterKeyId()));
        content.append(String.format("Fingerprint:       %s\n", masterKey.getKeyFingerprint()));
        content.append(String.format("Status:            %s\n", masterKey.getStatus()));
        content.append("\n");

        content.append("CUSTODIAN INFORMATION\n");
        content.append("─────────────────────────────────────────────────────────────\n");
        content.append(String.format("Custodian:         %s\n", ceremonyCustodian.getCustodianLabel()));
        content.append(String.format("Name:              %s\n", ceremonyCustodian.getKeyCustodian().getFullName()));
        content.append(String.format("Email:             %s\n", ceremonyCustodian.getKeyCustodian().getEmail()));
        content.append("\n");

        content.append("SHARE INFORMATION\n");
        content.append("─────────────────────────────────────────────────────────────\n");
        content.append(String.format("Share ID:          %s\n", share.getShareId()));
        content.append(String.format("Share Index:       %d of %d\n", share.getShareIndex(), ceremony.getNumberOfCustodians()));
        content.append(String.format("Threshold:         %d shares required for recovery\n", ceremony.getThreshold()));
        content.append(String.format("Polynomial Degree: %d\n", share.getPolynomialDegree()));
        content.append(String.format("Generated:         %s\n", formatDateTime(share.getGeneratedAt())));
        content.append("\n");

        content.append("VERIFICATION\n");
        content.append("─────────────────────────────────────────────────────────────\n");
        content.append(String.format("Verification Hash: %s\n", share.getShareVerificationHash()));
        content.append("\n");

        content.append("ENCRYPTED SHARE DATA\n");
        content.append("─────────────────────────────────────────────────────────────\n");
        content.append("Format: Base64-encoded AES-256-GCM encrypted data\n");
        content.append("The encrypted data includes:\n");
        content.append("  - Shamir share index and value\n");
        content.append("  - Prime modulus for reconstruction\n");
        content.append("  - Threshold information\n\n");

        // Encode share data as Base64
        String encodedShare = Base64.getEncoder().encodeToString(share.getShareDataEncrypted());

        // Format in 64-character lines
        content.append("BEGIN ENCRYPTED SHARE\n");
        for (int i = 0; i < encodedShare.length(); i += 64) {
            int end = Math.min(i + 64, encodedShare.length());
            content.append(encodedShare.substring(i, end)).append("\n");
        }
        content.append("END ENCRYPTED SHARE\n");
        content.append("\n");

        content.append("SECURITY NOTICE\n");
        content.append("─────────────────────────────────────────────────────────────\n");
        content.append("• Store this share in a secure location\n");
        content.append("• Do NOT share with unauthorized personnel\n");
        content.append("• Any " + ceremony.getThreshold() + " shares can reconstruct the master key\n");
        content.append("• Required for HSM recovery operations\n");
        content.append("• Contact security team if compromised\n");
        content.append("\n");

        content.append("═══════════════════════════════════════════════════════════════\n");
        content.append("        Generated by HSM Simulator - ArtiVisi Intermedia\n");
        content.append(String.format("        %s\n", formatDateTime(LocalDateTime.now())));
        content.append("═══════════════════════════════════════════════════════════════\n");

        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Marks a share as distributed
     */
    @Transactional
    public void markAsDistributed(UUID shareId, KeyShare.DistributionMethod method) {
        log.info("Marking share {} as distributed via {}", shareId, method);

        KeyShare share = shareRepository.findByShareId(shareId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        share.setDistributedAt(LocalDateTime.now());
        share.setDistributionMethod(method);
        shareRepository.save(share);

        log.info("Share marked as distributed");
    }

    /**
     * Sends share via email with attachment
     */
    @Transactional
    public void sendShareViaEmail(UUID shareId) {
        log.info("Sending share via email: {}", shareId);

        KeyShare share = shareRepository.findByShareId(shareId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        CeremonyCustodian ceremonyCustodian = share.getCeremonyCustodian();
        MasterKey masterKey = share.getMasterKey();
        KeyCeremony ceremony = masterKey.getKeyCeremony();
        String recipientEmail = ceremonyCustodian.getKeyCustodian().getEmail();
        String custodianName = ceremonyCustodian.getKeyCustodian().getFullName();

        // Generate share download content
        byte[] shareContent = generateShareDownload(shareId);

        // Build HTML email content
        String htmlContent = buildShareEmailHtml(
                custodianName,
                ceremonyCustodian.getCustodianLabel(),
                ceremony.getCeremonyName(),
                share.getShareId(),
                ceremony.getThreshold(),
                ceremony.getNumberOfCustodians()
        );

        // Send email with attachment
        try {
            emailService.sendHtmlEmailWithAttachment(
                    recipientEmail,
                    "Your HSM Key Share - " + ceremony.getCeremonyName(),
                    htmlContent,
                    "key-share-" + share.getShareId() + ".txt",
                    shareContent
            );

            // Mark as distributed
            markAsDistributed(UUID.fromString(share.getId().toString()), KeyShare.DistributionMethod.EMAIL);

            // Log appropriate message based on whether email was actually sent
            if (emailService.isEmailEnabled()) {
                log.info("Email sent successfully to {}", recipientEmail);
            } else {
                log.info("Email sending disabled - simulated email to {}", recipientEmail);
            }

        } catch (Exception e) {
            log.error("Failed to send email to {}", recipientEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Builds HTML email content for share distribution
     */
    private String buildShareEmailHtml(String custodianName, String custodianLabel,
                                       String ceremonyName, String shareId,
                                       int threshold, int totalShares) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 30px;
                            border-radius: 10px 10px 0 0;
                            text-align: center;
                        }
                        .content {
                            background: #f8f9fa;
                            padding: 30px;
                            border-radius: 0 0 10px 10px;
                        }
                        .info-box {
                            background: white;
                            padding: 20px;
                            border-radius: 8px;
                            margin: 20px 0;
                            border-left: 4px solid #667eea;
                        }
                        .warning-box {
                            background: #fff3cd;
                            border: 1px solid #ffc107;
                            padding: 15px;
                            border-radius: 8px;
                            margin: 20px 0;
                        }
                        .footer {
                            text-align: center;
                            color: #6c757d;
                            font-size: 12px;
                            margin-top: 30px;
                        }
                        h1, h2 {
                            margin: 0 0 15px 0;
                        }
                        .label {
                            font-weight: bold;
                            color: #667eea;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>🔐 HSM Key Share</h1>
                        <p>Secure Key Ceremony Distribution</p>
                    </div>

                    <div class="content">
                        <h2>Dear %s,</h2>

                        <p>You have been designated as <strong>%s</strong> for the HSM key ceremony <strong>"%s"</strong>.</p>

                        <div class="info-box">
                            <p><span class="label">Share ID:</span> %s</p>
                            <p><span class="label">Threshold:</span> %d of %d shares required for key recovery</p>
                            <p><span class="label">Your Role:</span> Custodian of encrypted key share</p>
                        </div>

                        <p>Your encrypted key share is attached to this email as a text file. Please:</p>
                        <ul>
                            <li>Download and store the attached file in a secure location</li>
                            <li>Keep multiple backups in different secure locations</li>
                            <li>DO NOT share this file with unauthorized personnel</li>
                            <li>Keep the file encrypted and password-protected if possible</li>
                        </ul>

                        <div class="warning-box">
                            <strong>⚠️ Security Notice</strong>
                            <p style="margin: 10px 0 0 0;">Any %d shares can be combined to reconstruct the master key. Protect this share as you would protect the master key itself.</p>
                        </div>

                        <p>If you have any questions or concerns, please contact your security administrator immediately.</p>
                    </div>

                    <div class="footer">
                        <p>This email was automatically generated by HSM Simulator</p>
                        <p>ArtiVisi Intermedia &copy; 2025</p>
                    </div>
                </body>
                </html>
                """.formatted(custodianName, custodianLabel, ceremonyName, shareId, threshold, totalShares, threshold);
    }

    /**
     * Gets share details for viewing
     */
    @Transactional(readOnly = true)
    public ShareDetailResponse getShareDetail(UUID shareId) {
        KeyShare share = shareRepository.findByShareId(shareId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        CeremonyCustodian ceremonyCustodian = share.getCeremonyCustodian();
        MasterKey masterKey = share.getMasterKey();

        return ShareDetailResponse.builder()
                .shareId(share.getShareId())
                .shareIndex(share.getShareIndex())
                .custodianName(ceremonyCustodian.getKeyCustodian().getFullName())
                .custodianLabel(ceremonyCustodian.getCustodianLabel())
                .verificationHash(share.getShareVerificationHash())
                .generatedAt(share.getGeneratedAt())
                .distributedAt(share.getDistributedAt())
                .distributionMethod(share.getDistributionMethod())
                .masterKeyFingerprint(masterKey.getKeyFingerprint())
                .encryptedShareSize(share.getShareDataEncrypted().length)
                .build();
    }

    // ===== Helper Methods =====

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ===== Response Classes =====

    @lombok.Data
    @lombok.Builder
    public static class ShareDistributionResponse {
        private MasterKey masterKey;
        private List<ShareInfo> shares;
        private int totalShares;
        private long distributedShares;
    }

    @lombok.Data
    @lombok.Builder
    public static class ShareInfo {
        private String shareId;
        private int shareIndex;
        private String custodianName;
        private String custodianLabel;
        private String custodianEmail;
        private LocalDateTime distributedAt;
        private KeyShare.DistributionMethod distributionMethod;
        private String verificationHash;
        private boolean isDistributed;
    }

    @lombok.Data
    @lombok.Builder
    public static class ShareDetailResponse {
        private String shareId;
        private int shareIndex;
        private String custodianName;
        private String custodianLabel;
        private String verificationHash;
        private LocalDateTime generatedAt;
        private LocalDateTime distributedAt;
        private KeyShare.DistributionMethod distributionMethod;
        private String masterKeyFingerprint;
        private int encryptedShareSize;
    }
}
