package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.GeneratedMac;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.GeneratedMacRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for MAC (Message Authentication Code) generation and verification
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MacService {

    private final GeneratedMacRepository generatedMacRepository;
    private final MasterKeyRepository masterKeyRepository;

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
        return switch (algorithm) {
            case "ISO9797-ALG3" -> calculateRetailMac(message, key);
            case "HMAC-SHA256" -> calculateHmacSha256(message, key);
            case "CBC-MAC" -> calculateCbcMac(message, key);
            default -> throw new IllegalArgumentException("Unsupported MAC algorithm: " + algorithm);
        };
    }

    /**
     * ISO 9797-1 Algorithm 3 (Retail MAC)
     * Single DES CBC over message, then 3DES on final block
     */
    private String calculateRetailMac(String message, MasterKey key) {
        try {
            // Pad message to multiple of 8 bytes (DES block size)
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] paddedMessage = padMessage(messageBytes, 8);

            // Use first 8 bytes of key for DES
            byte[] keyBytes = new byte[8];
            System.arraycopy(key.getKeyDataEncrypted(), 0, keyBytes, 0,
                    Math.min(8, key.getKeyDataEncrypted().length));

            // DES CBC over entire message
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
            Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new javax.crypto.spec.IvParameterSpec(new byte[8]));

            byte[] encrypted = cipher.doFinal(paddedMessage);

            // Take last 8 bytes (last block)
            byte[] lastBlock = new byte[8];
            System.arraycopy(encrypted, encrypted.length - 8, lastBlock, 0, 8);

            // For full retail MAC, would do 3DES on last block, but for simulation we'll use the DES result
            return bytesToHex(lastBlock);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate Retail MAC", e);
        }
    }

    /**
     * HMAC-SHA256 MAC
     */
    private String calculateHmacSha256(String message, MasterKey key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getKeyDataEncrypted(), "HmacSHA256");
            mac.init(secretKey);
            byte[] macBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // Take first 8 bytes for compatibility with payment systems
            byte[] truncated = new byte[8];
            System.arraycopy(macBytes, 0, truncated, 0, 8);

            return bytesToHex(truncated);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }

    /**
     * CBC-MAC (DES-based)
     */
    private String calculateCbcMac(String message, MasterKey key) {
        try {
            // Pad message to multiple of 8 bytes
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] paddedMessage = padMessage(messageBytes, 8);

            // Use first 8 bytes of key for DES
            byte[] keyBytes = new byte[8];
            System.arraycopy(key.getKeyDataEncrypted(), 0, keyBytes, 0,
                    Math.min(8, key.getKeyDataEncrypted().length));

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
            Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new javax.crypto.spec.IvParameterSpec(new byte[8]));

            byte[] encrypted = cipher.doFinal(paddedMessage);

            // Take last 8 bytes
            byte[] mac = new byte[8];
            System.arraycopy(encrypted, encrypted.length - 8, mac, 0, 8);

            return bytesToHex(mac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate CBC-MAC", e);
        }
    }

    /**
     * Pad message to block size using ISO/IEC 9797-1 padding method 2
     */
    private byte[] padMessage(byte[] message, int blockSize) {
        int paddingLength = blockSize - (message.length % blockSize);
        byte[] padded = new byte[message.length + paddingLength];
        System.arraycopy(message, 0, padded, 0, message.length);

        // First padding byte is 0x80
        padded[message.length] = (byte) 0x80;

        // Rest are 0x00 (already initialized to 0)
        return padded;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
