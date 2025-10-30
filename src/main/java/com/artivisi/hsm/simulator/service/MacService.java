package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.config.CryptoConstants;
import com.artivisi.hsm.simulator.entity.GeneratedMac;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.GeneratedMacRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for MAC (Message Authentication Code) generation and verification.
 * Uses modern AES-CMAC and HMAC-SHA256 algorithms.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MacService {

    private final GeneratedMacRepository generatedMacRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final KeyGenerationService keyGenerationService;

    // Cache for derived MAC keys to avoid re-deriving on every operation
    private final ConcurrentHashMap<String, byte[]> derivedKeyCache = new ConcurrentHashMap<>();

    /**
     * Generate MAC for a message using specified key
     */
    @Transactional
    public GeneratedMac generateMac(UUID keyId, String message, String algorithm) {
        log.info("Generating MAC for message length: {}, algorithm: {}", message.length(), algorithm);

        MasterKey macKey = masterKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("MAC key not found: " + keyId));

        if (!isValidMacKey(macKey)) {
            throw new IllegalArgumentException("Invalid key type for MAC. Use TSK (terminal) or ZSK (zone) keys.");
        }

        String macValue = calculateMac(message, macKey, algorithm);

        GeneratedMac generatedMac = GeneratedMac.builder()
                .message(message)
                .messageLength(message.length())
                .macValue(macValue)
                .macAlgorithm(algorithm)
                .macKey(macKey)
                .status(GeneratedMac.MacStatus.ACTIVE)
                .verificationAttempts(0)
                .build();

        return generatedMacRepository.save(generatedMac);
    }

    /**
     * Verify MAC for a message
     */
    @Transactional
    public boolean verifyMac(String message, String providedMac, UUID keyId, String algorithm) {
        log.info("Verifying MAC for message length: {}", message.length());

        MasterKey macKey = masterKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("MAC key not found: " + keyId));

        if (!isValidMacKey(macKey)) {
            throw new IllegalArgumentException("Invalid key type for MAC. Use TSK (terminal) or ZSK (zone) keys.");
        }

        String calculatedMac = calculateMac(message, macKey, algorithm);
        boolean isValid = calculatedMac.equalsIgnoreCase(providedMac);

        // Find and update existing MAC record if exists
        generatedMacRepository.findByMessageAndMacKey_Id(message, keyId)
                .ifPresent(mac -> {
                    mac.setVerificationAttempts(mac.getVerificationAttempts() + 1);
                    mac.setLastVerifiedAt(LocalDateTime.now());
                    generatedMacRepository.save(mac);
                });

        log.info("MAC verification result: {}", isValid);
        return isValid;
    }

    // ===== Helper Methods =====

    private boolean isValidMacKey(MasterKey key) {
        KeyType keyType = key.getKeyType();
        // TSK is used for terminal MAC
        // ZSK is used for zone/inter-bank MAC
        return keyType == KeyType.TSK || keyType == KeyType.ZSK;
    }

    /**
     * Calculate MAC using specified algorithm
     */
    private String calculateMac(String message, MasterKey key, String algorithm) {
        return switch (algorithm.toUpperCase()) {
            case "AES-CMAC" -> calculateAesCmac(message, key, CryptoConstants.MAC_KEY_BYTES, CryptoConstants.MAC_OUTPUT_BYTES);
            case "AES-CMAC-256" -> calculateAesCmac(message, key, CryptoConstants.MAC_KEY_BYTES, CryptoConstants.MAC_OUTPUT_BYTES);
            case "AES-CMAC-128" -> calculateAesCmac(message, key, CryptoConstants.MAC_KEY_BYTES_COMPAT, CryptoConstants.MAC_OUTPUT_BYTES);
            case "AES-CMAC-64" -> calculateAesCmac(message, key, CryptoConstants.MAC_KEY_BYTES_COMPAT, CryptoConstants.MAC_OUTPUT_BYTES_COMPAT);
            case "HMAC-SHA256" -> calculateHmacSha256(message, key, CryptoConstants.MAC_OUTPUT_BYTES);
            case "HMAC-SHA256-FULL" -> calculateHmacSha256(message, key, 32); // Full 256-bit output
            case "HMAC-SHA256-64" -> calculateHmacSha256(message, key, CryptoConstants.MAC_OUTPUT_BYTES_COMPAT);
            default -> throw new IllegalArgumentException("Unsupported MAC algorithm: " + algorithm +
                ". Supported: AES-CMAC, AES-CMAC-256, AES-CMAC-128, AES-CMAC-64, HMAC-SHA256, HMAC-SHA256-FULL, HMAC-SHA256-64");
        };
    }

    /**
     * AES-CMAC (NIST SP 800-38B)
     * Modern replacement for DES-based MAC
     *
     * @param message Message to authenticate
     * @param key Master key (will derive appropriate operational key)
     * @param keySize Operational key size (16=AES-128, 32=AES-256)
     * @param outputBytes MAC output length (8 for banking compatibility, 16 for full security)
     */
    private String calculateAesCmac(String message, MasterKey key, int keySize, int outputBytes) {
        try {
            // Derive operational MAC key using context
            String context = keyGenerationService.buildKeyContext(
                key.getKeyType().toString(),
                key.getIdBank() != null ? key.getIdBank().toString() : "GLOBAL",
                "MAC"
            );

            byte[] macKeyBytes = getOrDeriveMacKey(key, context, keySize);

            // Use AES-CMAC
            Mac mac = Mac.getInstance(CryptoConstants.MAC_ALGORITHM_CMAC);
            SecretKeySpec secretKey = new SecretKeySpec(macKeyBytes, CryptoConstants.MASTER_KEY_ALGORITHM);
            mac.init(secretKey);

            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] fullMac = mac.doFinal(messageBytes);

            // Truncate if needed for banking compatibility
            byte[] macOutput = (outputBytes < fullMac.length)
                ? Arrays.copyOf(fullMac, outputBytes)
                : fullMac;

            log.debug("AES-CMAC calculated: key_size={}, output_bytes={}, context={}",
                     keySize, outputBytes, context);

            return bytesToHex(macOutput);

        } catch (Exception e) {
            log.error("Failed to calculate AES-CMAC for key: {}", key.getId(), e);
            throw new RuntimeException("Failed to calculate AES-CMAC", e);
        }
    }

    /**
     * HMAC-SHA256 MAC
     * Modern cryptographic hash-based MAC
     *
     * @param message Message to authenticate
     * @param key Master key (will derive appropriate operational key)
     * @param outputBytes MAC output length (8, 16, or 32 bytes)
     */
    private String calculateHmacSha256(String message, MasterKey key, int outputBytes) {
        try {
            // Derive operational MAC key using context
            String context = keyGenerationService.buildKeyContext(
                key.getKeyType().toString(),
                key.getIdBank() != null ? key.getIdBank().toString() : "GLOBAL",
                "HMAC"
            );

            byte[] macKeyBytes = getOrDeriveMacKey(key, context, CryptoConstants.MAC_KEY_BYTES);

            Mac mac = Mac.getInstance(CryptoConstants.MAC_ALGORITHM_HMAC);
            SecretKeySpec secretKey = new SecretKeySpec(macKeyBytes, CryptoConstants.MAC_ALGORITHM_HMAC);
            mac.init(secretKey);

            byte[] macBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // Truncate if needed
            byte[] macOutput = (outputBytes < macBytes.length)
                ? Arrays.copyOf(macBytes, outputBytes)
                : macBytes;

            log.debug("HMAC-SHA256 calculated: output_bytes={}, context={}", outputBytes, context);

            return bytesToHex(macOutput);

        } catch (Exception e) {
            log.error("Failed to calculate HMAC-SHA256 for key: {}", key.getId(), e);
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }

    /**
     * Get derived MAC key from cache or derive it
     */
    private byte[] getOrDeriveMacKey(MasterKey key, String context, int keySize) {
        String cacheKey = key.getId() + ":" + context + ":" + keySize;
        return derivedKeyCache.computeIfAbsent(cacheKey, k -> {
            log.debug("Deriving new MAC key: {}", cacheKey);
            return keyGenerationService.deriveOperationalKey(
                key.getKeyData(),
                context,
                keySize
            );
        });
    }

    /**
     * Clear the derived key cache (useful for testing or key rotation)
     */
    public void clearKeyCache() {
        derivedKeyCache.clear();
        log.info("Cleared MAC key cache");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
