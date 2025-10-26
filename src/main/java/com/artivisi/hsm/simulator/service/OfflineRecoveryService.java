package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for offline key recovery using only share files (no database required).
 * Useful for disaster recovery scenarios where the database is lost.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OfflineRecoveryService {

    private final KeyGenerationService keyGenerationService;
    private final MasterKeyRepository masterKeyRepository;

    /**
     * Parses a key share file and extracts share information
     */
    public ParsedShare parseShareFile(MultipartFile file) throws Exception {
        log.info("Parsing share file: {}", file.getOriginalFilename());

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Extract share information using regex
        String shareId = extractValue(content, "Share ID:\\s+(.+)");
        String shareIndexStr = extractValue(content, "Share Index:\\s+(\\d+)");
        String thresholdStr = extractValue(content, "Threshold:\\s+(\\d+)");
        String masterKeyFingerprint = extractValue(content, "Fingerprint:\\s+(.+)");
        String kdfSalt = extractValue(content, "KDF Salt:\\s+(.+)");
        String ceremonyName = extractValue(content, "Ceremony Name:\\s+(.+)");
        String ceremonyId = extractValue(content, "Ceremony ID:\\s+(.+)");
        String custodianEmail = extractValue(content, "Email:\\s+(.+)");

        if (shareId == null || shareIndexStr == null || thresholdStr == null || ceremonyId == null || custodianEmail == null) {
            throw new IllegalArgumentException("Invalid share file format: missing required fields (shareId, shareIndex, threshold, ceremonyId, email)");
        }

        int shareIndex = Integer.parseInt(shareIndexStr.split(" ")[0]); // "1 of 3" -> "1"
        int threshold = Integer.parseInt(thresholdStr.split(" ")[0]); // "2 shares required" -> "2"

        // Extract encrypted share data
        byte[] encryptedShareData = extractEncryptedShare(content);

        if (encryptedShareData == null || encryptedShareData.length == 0) {
            throw new IllegalArgumentException("Invalid share file: no encrypted data found");
        }

        ParsedShare share = ParsedShare.builder()
                .shareId(shareId)
                .shareIndex(shareIndex)
                .threshold(threshold)
                .encryptedShareData(encryptedShareData)
                .masterKeyFingerprint(masterKeyFingerprint)
                .kdfSalt(kdfSalt)
                .ceremonyName(ceremonyName)
                .ceremonyId(ceremonyId)
                .custodianEmail(custodianEmail)
                .fileName(file.getOriginalFilename())
                .build();

        log.info("Successfully parsed share: {} (index {}, threshold {})", shareId, shareIndex, threshold);
        return share;
    }

    /**
     * Reconstructs master key from uploaded share files with custodian passphrases
     * @param sharesWithPassphrases List of shares paired with their passphrases
     */
    public OfflineRecoveryResult reconstructFromFiles(List<ShareWithPassphrase> sharesWithPassphrases) throws Exception {
        log.info("Starting offline recovery with {} shares", sharesWithPassphrases.size());

        if (sharesWithPassphrases.isEmpty()) {
            throw new IllegalArgumentException("No shares provided");
        }

        List<ParsedShare> shares = sharesWithPassphrases.stream()
                .map(ShareWithPassphrase::getShare)
                .toList();

        // Validate all shares have same threshold
        int threshold = shares.get(0).getThreshold();
        boolean allSameThreshold = shares.stream().allMatch(s -> s.getThreshold() == threshold);
        if (!allSameThreshold) {
            throw new IllegalArgumentException("All shares must have the same threshold requirement");
        }

        // Check if we have enough shares
        if (shares.size() < threshold) {
            throw new IllegalArgumentException(
                    String.format("Insufficient shares: %d provided, %d required", shares.size(), threshold)
            );
        }

        // Validate fingerprints match (if available)
        String expectedFingerprint = shares.stream()
                .filter(s -> s.getMasterKeyFingerprint() != null)
                .findFirst()
                .map(ParsedShare::getMasterKeyFingerprint)
                .orElse(null);

        if (expectedFingerprint != null) {
            boolean allMatch = shares.stream()
                    .filter(s -> s.getMasterKeyFingerprint() != null)
                    .allMatch(s -> s.getMasterKeyFingerprint().equals(expectedFingerprint));
            if (!allMatch) {
                throw new IllegalArgumentException("Shares have different master key fingerprints - they may be from different ceremonies");
            }
        }

        // Decrypt ALL provided shares using passphrases
        // Even though only 'threshold' shares are needed, using all provided shares
        // provides better verification and follows best security practices
        List<KeyGenerationService.ShamirShare> shamirShares = new ArrayList<>();
        for (ShareWithPassphrase swp : sharesWithPassphrases) {
            // Derive the encryption key from passphrase
            byte[] encryptionKey = deriveEncryptionKeyFromPassphrase(swp.getPassphrase());

            // Decrypt the share
            KeyGenerationService.ShamirShare shamirShare = keyGenerationService.decryptShare(
                    swp.getShare().getEncryptedShareData(),
                    encryptionKey
            );
            shamirShares.add(shamirShare);
        }

        // Reconstruct the master key
        byte[] reconstructedKey = keyGenerationService.reconstructSecret(shamirShares);

        // Generate fingerprint of reconstructed key
        String reconstructedFingerprint = generateKeyFingerprint(reconstructedKey);

        // Verify if we have original fingerprint
        boolean verified = false;
        if (expectedFingerprint != null) {
            // Compare fingerprints case-insensitively and strip any formatting (colons, spaces)
            String cleanReconstructed = reconstructedFingerprint.replaceAll("[^a-fA-F0-9]", "").toLowerCase();
            String cleanExpected = expectedFingerprint.replaceAll("[^a-fA-F0-9]", "").toLowerCase();
            verified = cleanReconstructed.equals(cleanExpected);

            log.info("Fingerprint comparison - Reconstructed: {}, Expected: {}, Match: {}",
                     cleanReconstructed, cleanExpected, verified);
        }

        log.info("Offline recovery completed. Verified: {}", verified);

        return OfflineRecoveryResult.builder()
                .success(true)
                .sharesUsed(shamirShares.size())
                .threshold(threshold)
                .reconstructedKey(reconstructedKey)
                .reconstructedFingerprint(reconstructedFingerprint)
                .originalFingerprint(expectedFingerprint)
                .verified(verified)
                .canVerify(expectedFingerprint != null)
                .ceremonyName(shares.get(0).getCeremonyName())
                .ceremonyId(shares.get(0).getCeremonyId())
                .message(verified ? "Key successfully reconstructed and verified" :
                        (expectedFingerprint != null ? "Key reconstructed but verification failed" :
                                "Key reconstructed (no original fingerprint for verification)"))
                .build();
    }

    /**
     * Extracts a value from content using regex
     */
    private String extractValue(String content, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extracts encrypted share data from content
     */
    private byte[] extractEncryptedShare(String content) {
        int beginIndex = content.indexOf("BEGIN ENCRYPTED SHARE");
        int endIndex = content.indexOf("END ENCRYPTED SHARE");

        if (beginIndex == -1 || endIndex == -1) {
            return null;
        }

        // Extract Base64 content between markers
        String base64Content = content.substring(beginIndex + "BEGIN ENCRYPTED SHARE".length(), endIndex);

        // Remove whitespace and newlines
        base64Content = base64Content.replaceAll("\\s+", "");

        // Decode Base64
        return Base64.getDecoder().decode(base64Content);
    }

    /**
     * Derives encryption key from custodian passphrase using PBKDF2
     * Must match the logic in CeremonyService.deriveEncryptionKeyFromPassphrase()
     */
    private byte[] deriveEncryptionKeyFromPassphrase(String passphrase) {
        // Use a fixed salt for passphrase-based encryption
        // This allows offline recovery with only the passphrase
        byte[] salt = "HSM_SHARE_ENCRYPTION_SALT_V1".getBytes(StandardCharsets.UTF_8);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 100000, 256);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key from passphrase", e);
        }
    }

    /**
     * Generates SHA-256 fingerprint of a key
     */
    private String generateKeyFingerprint(byte[] keyData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyData);
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key fingerprint", e);
        }
    }

    /**
     * Converts bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Loads a reconstructed master key into the HSM (database)
     */
    @Transactional
    public String loadIntoHSM(OfflineRecoveryResult recoveryResult) {
        log.info("Loading reconstructed key into HSM: {}", recoveryResult.getCeremonyName());

        if (!recoveryResult.isSuccess()) {
            throw new IllegalArgumentException("Cannot load failed recovery result");
        }

        if (!recoveryResult.isVerified() && recoveryResult.isCanVerify()) {
            log.warn("Loading unverified key - fingerprint mismatch detected");
        }

        // Generate master key ID
        String masterKeyId = "MK-RECOVERED-" + LocalDateTime.now().getYear() + "-" +
                             UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create MasterKey entity
        MasterKey masterKey = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .keyCeremony(null) // No ceremony for recovered keys
                .keyType("HSM_MASTER_KEY")
                .algorithm("AES")
                .keySize(256)
                .keyDataEncrypted(recoveryResult.getReconstructedKey())
                .keyFingerprint(recoveryResult.getReconstructedFingerprint())
                .keyChecksum(calculateChecksum(recoveryResult.getReconstructedKey()))
                .combinedEntropyHash("RECOVERED_KEY_NO_ENTROPY_HASH") // Placeholder for recovered keys
                .generationMethod("RECOVERED")
                .kdfIterations(0) // Not applicable for recovered keys
                .kdfSalt("RECOVERED_KEY_NO_KDF_SALT") // Placeholder for recovered keys
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        masterKey = masterKeyRepository.save(masterKey);

        log.info("Master key loaded into HSM with ID: {}", masterKeyId);
        return masterKeyId;
    }

    /**
     * Calculates a simple checksum for the key
     */
    private String calculateChecksum(byte[] keyData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(keyData);
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }

    // ===== Data Classes =====

    @lombok.Data
    @lombok.Builder
    public static class ShareWithPassphrase {
        private ParsedShare share;
        private String passphrase;
    }

    @lombok.Data
    @lombok.Builder
    public static class ParsedShare {
        private String shareId;
        private int shareIndex;
        private int threshold;
        private byte[] encryptedShareData;
        private String masterKeyFingerprint;
        private String kdfSalt;
        private String ceremonyName;
        private String ceremonyId;
        private String custodianEmail;
        private String fileName;
    }

    @lombok.Data
    @lombok.Builder
    public static class OfflineRecoveryResult {
        private boolean success;
        private int sharesUsed;
        private int threshold;
        private byte[] reconstructedKey;
        private String reconstructedFingerprint;
        private String originalFingerprint;
        private boolean verified;
        private boolean canVerify;
        private String ceremonyName;
        private String ceremonyId;
        private String message;
    }
}
