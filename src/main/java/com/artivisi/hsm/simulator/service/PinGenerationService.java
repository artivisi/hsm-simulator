package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.GeneratedPin;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.GeneratedPinRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Service for PIN generation, encryption, and verification using HSM keys
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PinGenerationService {

    private final GeneratedPinRepository generatedPinRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a random PIN and encrypt it with the specified key
     */
    @Transactional
    public GeneratedPin generatePin(UUID keyId, String accountNumber, Integer pinLength, String pinFormat) {
        String clearPin = generateRandomPin(pinLength);
        return generatePin(keyId, accountNumber, clearPin, pinFormat);
    }

    /**
     * Encrypt a given PIN with the specified key (for API usage)
     */
    @Transactional
    public GeneratedPin generatePin(UUID keyId, String accountNumber, String clearPin, String pinFormat) {
        log.info("Generating PIN for account: {}, length: {}, format: {}", accountNumber, clearPin.length(), pinFormat);

        MasterKey encryptionKey = masterKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("Encryption key not found: " + keyId));

        if (!isValidPinKey(encryptionKey)) {
            throw new IllegalArgumentException("Invalid key type for PIN encryption. Use LMK (storage), TPK (terminal), or ZPK (zone).");
        }

        // Create PIN block based on format
        String pinBlock = createPinBlock(clearPin, accountNumber, pinFormat);

        // Encrypt PIN block
        String encryptedPinBlock = encryptPinBlock(pinBlock, encryptionKey);

        // Generate PIN Verification Value (PVV)
        String pvv = generatePVV(clearPin, accountNumber);

        GeneratedPin generatedPin = GeneratedPin.builder()
                .accountNumber(accountNumber)
                .pinLength(clearPin.length())
                .pinFormat(pinFormat)
                .encryptedPinBlock(encryptedPinBlock)
                .pinVerificationValue(pvv)
                .encryptionKey(encryptionKey)
                .clearPin(clearPin)
                .status(GeneratedPin.PinStatus.ACTIVE)
                .verificationAttempts(0)
                .build();

        return generatedPinRepository.save(generatedPin);
    }

    /**
     * Verify a PIN against stored encrypted PIN
     */
    @Transactional
    public boolean verifyPin(String accountNumber, String enteredPin) {
        log.info("Verifying PIN for account: {}", accountNumber);

        GeneratedPin storedPin = generatedPinRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("PIN not found for account: " + accountNumber));

        if (storedPin.getStatus() != GeneratedPin.PinStatus.ACTIVE) {
            throw new IllegalStateException("PIN is not active for account: " + accountNumber);
        }

        // For simulation, compare with clear PIN
        boolean isValid = storedPin.getClearPin().equals(enteredPin);

        storedPin.setVerificationAttempts(storedPin.getVerificationAttempts() + 1);

        if (!isValid) {
            if (storedPin.getVerificationAttempts() >= 3) {
                storedPin.setStatus(GeneratedPin.PinStatus.BLOCKED);
                log.warn("PIN blocked for account {} after {} failed attempts",
                         accountNumber, storedPin.getVerificationAttempts());
            }
        }

        generatedPinRepository.save(storedPin);
        return isValid;
    }

    /**
     * Translate PIN from one encryption key to another
     */
    @Transactional
    public String translatePin(UUID sourcePinId, UUID targetKeyId) {
        log.info("Translating PIN from pin: {} to key: {}", sourcePinId, targetKeyId);

        GeneratedPin sourcePin = generatedPinRepository.findById(sourcePinId)
                .orElseThrow(() -> new IllegalArgumentException("Source PIN not found: " + sourcePinId));

        MasterKey targetKey = masterKeyRepository.findById(targetKeyId)
                .orElseThrow(() -> new IllegalArgumentException("Target key not found: " + targetKeyId));

        if (!isValidPinKey(targetKey)) {
            throw new IllegalArgumentException("Invalid target key type for PIN encryption. Use LMK (storage), TPK (terminal), or ZPK (zone).");
        }

        // Decrypt with source key (simulated)
        String pinBlock = createPinBlock(sourcePin.getClearPin(), sourcePin.getAccountNumber(),
                                         sourcePin.getPinFormat());

        // Encrypt with target key
        return encryptPinBlock(pinBlock, targetKey);
    }

    // ===== Helper Methods =====

    private boolean isValidPinKey(MasterKey key) {
        KeyType keyType = key.getKeyType();
        // LMK is used for PIN storage encryption in HSM
        // TPK/ZPK are used for PIN transmission encryption
        return keyType == KeyType.LMK ||
               keyType == KeyType.TPK ||
               keyType == KeyType.ZPK;
    }

    private String generateRandomPin(int length) {
        StringBuilder pin = new StringBuilder();
        for (int i = 0; i < length; i++) {
            pin.append(secureRandom.nextInt(10));
        }
        return pin.toString();
    }

    private String createPinBlock(String pin, String accountNumber, String format) {
        return switch (format) {
            case "ISO-0" -> createISO0PinBlock(pin, accountNumber);
            case "ISO-1" -> createISO1PinBlock(pin);
            case "ISO-3" -> createISO3PinBlock(pin, accountNumber);
            case "ISO-4" -> createISO4PinBlock(pin, accountNumber);
            default -> throw new IllegalArgumentException("Unsupported PIN format: " + format);
        };
    }

    private String createISO0PinBlock(String pin, String accountNumber) {
        // ISO Format 0: 0L[PIN][F]... XOR [0000][12 rightmost PAN digits excluding check digit]
        String pinField = String.format("0%d%s", pin.length(), pin);
        while (pinField.length() < 16) {
            pinField += "F";
        }

        String panField = "0000" + accountNumber.substring(accountNumber.length() - 13,
                                                           accountNumber.length() - 1);

        return xorHex(pinField, panField);
    }

    private String createISO1PinBlock(String pin) {
        // ISO Format 1: 1L[PIN][Random]
        String pinField = String.format("1%d%s", pin.length(), pin);
        while (pinField.length() < 16) {
            pinField += Integer.toHexString(secureRandom.nextInt(16));
        }
        return pinField;
    }

    private String createISO3PinBlock(String pin, String accountNumber) {
        // ISO Format 3: Similar to Format 0 but with different padding
        String pinField = String.format("3%d%s", pin.length(), pin);
        while (pinField.length() < 16) {
            pinField += secureRandom.nextInt(10);
        }

        String panField = "0000" + accountNumber.substring(accountNumber.length() - 13,
                                                           accountNumber.length() - 1);

        return xorHex(pinField, panField);
    }

    private String createISO4PinBlock(String pin, String accountNumber) {
        // ISO Format 4: 4L[PIN][Random] XOR [PAN]
        String pinField = String.format("4%d%s", pin.length(), pin);
        while (pinField.length() < 16) {
            pinField += Integer.toHexString(secureRandom.nextInt(16));
        }

        // Use last 16 digits of PAN for XOR
        String panField = "0000" + accountNumber.substring(accountNumber.length() - 13,
                                                           accountNumber.length() - 1);

        return xorHex(pinField, panField);
    }

    private String xorHex(String hex1, String hex2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(hex1.length(), hex2.length()); i++) {
            int val1 = Character.digit(hex1.charAt(i), 16);
            int val2 = Character.digit(hex2.charAt(i), 16);
            result.append(Integer.toHexString(val1 ^ val2));
        }
        return result.toString().toUpperCase();
    }

    private String encryptPinBlock(String pinBlock, MasterKey key) {
        try {
            // Use first 16 bytes of key for AES-128
            byte[] keyBytes = new byte[16];
            System.arraycopy(key.getKeyDataEncrypted(), 0, keyBytes, 0,
                           Math.min(16, key.getKeyDataEncrypted().length));

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Convert hex PIN block to bytes
            byte[] pinBlockBytes = hexToBytes(pinBlock);

            // Pad to 16 bytes if needed
            byte[] paddedPinBlock = new byte[16];
            System.arraycopy(pinBlockBytes, 0, paddedPinBlock, 0,
                           Math.min(pinBlockBytes.length, 16));

            byte[] encrypted = cipher.doFinal(paddedPinBlock);
            return bytesToHex(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt PIN block", e);
        }
    }

    private String generatePVV(String pin, String accountNumber) {
        try {
            String input = pin + accountNumber;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());

            // Take first 4 digits from hash
            StringBuilder pvv = new StringBuilder();
            for (byte b : hash) {
                int digit = Math.abs(b % 10);
                pvv.append(digit);
                if (pvv.length() == 4) break;
            }

            return pvv.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PVV", e);
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
