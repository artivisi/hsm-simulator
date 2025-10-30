package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.config.CryptoConstants;
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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for PIN generation, encryption, and verification using HSM keys
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PinGenerationService {

    private final GeneratedPinRepository generatedPinRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final KeyGenerationService keyGenerationService;
    private final SecureRandom secureRandom = new SecureRandom();

    // Cache for derived PIN keys to avoid re-deriving on every operation
    private final ConcurrentHashMap<String, byte[]> derivedKeyCache = new ConcurrentHashMap<>();

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

    /**
     * Verify PIN using encrypted PIN blocks (TPK from terminal, LMK from database)
     * Method: PIN Block Comparison
     * This simulates HSM operation where:
     * 1. Core bank app sends PIN block from terminal (encrypted under TPK)
     * 2. Core bank app sends stored PIN block from database (encrypted under LMK)
     * 3. HSM decrypts both and compares the clear PINs directly
     */
    @Transactional
    public boolean verifyPinWithTranslation(
            String encryptedPinBlockUnderTPK,
            String encryptedPinBlockUnderLMK,
            String pan,
            String pinFormat,
            UUID tpkKeyId,
            UUID lmkKeyId) {

        log.info("========================================");
        log.info("PIN VERIFICATION - METHOD: PIN Block Comparison");
        log.info("========================================");
        log.info("Input Parameters:");
        log.info("  - PAN: {}", maskPan(pan));
        log.info("  - PIN Format: {}", pinFormat);
        log.info("  - Encrypted PIN Block (TPK): {}", encryptedPinBlockUnderTPK);
        log.info("  - Encrypted PIN Block (LMK): {}", encryptedPinBlockUnderLMK);

        try {
            // Step 1: Retrieve keys
            log.info("----------------------------------------");
            log.info("STEP 1: Retrieve Cryptographic Keys");
            log.info("----------------------------------------");

            MasterKey tpkKey = masterKeyRepository.findById(tpkKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("TPK key not found: " + tpkKeyId));
            log.info("TPK Key Retrieved:");
            log.info("  - Key ID: {}", tpkKey.getMasterKeyId());
            log.info("  - Key Type: {}", tpkKey.getKeyType());
            log.info("  - Algorithm: {}", tpkKey.getAlgorithm());
            log.info("  - Key Size: {} bits", tpkKey.getKeySize());

            MasterKey lmkKey = masterKeyRepository.findById(lmkKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("LMK key not found: " + lmkKeyId));
            log.info("LMK Key Retrieved:");
            log.info("  - Key ID: {}", lmkKey.getMasterKeyId());
            log.info("  - Key Type: {}", lmkKey.getKeyType());
            log.info("  - Algorithm: {}", lmkKey.getAlgorithm());
            log.info("  - Key Size: {} bits", lmkKey.getKeySize());

            // Validate key types
            if (tpkKey.getKeyType() != KeyType.TPK) {
                throw new IllegalArgumentException("Source key must be TPK, got: " + tpkKey.getKeyType());
            }
            if (lmkKey.getKeyType() != KeyType.LMK) {
                throw new IllegalArgumentException("Target key must be LMK, got: " + lmkKey.getKeyType());
            }

            // Step 2: Decrypt PIN block under TPK
            log.info("----------------------------------------");
            log.info("STEP 2: Decrypt PIN Block from Terminal");
            log.info("----------------------------------------");
            log.info("Encrypted PIN Block (TPK): {}", encryptedPinBlockUnderTPK);
            log.info("Decryption Algorithm: AES/ECB");

            String clearPinBlockFromTPK = decryptPinBlock(encryptedPinBlockUnderTPK, tpkKey);
            log.info("Clear PIN Block (decrypted): {}", clearPinBlockFromTPK);

            // Step 3: Extract PIN from terminal PIN block
            log.info("----------------------------------------");
            log.info("STEP 3: Extract PIN from Terminal PIN Block");
            log.info("----------------------------------------");
            log.info("PIN Format: {}", pinFormat);
            log.info("Clear PIN Block: {}", clearPinBlockFromTPK);

            String pinFromTerminal = extractPinFromPinBlock(clearPinBlockFromTPK, pan, pinFormat);
            log.info("Extracted PIN from Terminal: {}", maskPin(pinFromTerminal));
            log.info("PIN Length: {}", pinFromTerminal.length());

            // Step 4: Decrypt PIN block under LMK
            log.info("----------------------------------------");
            log.info("STEP 4: Decrypt PIN Block from Database");
            log.info("----------------------------------------");
            log.info("Encrypted PIN Block (LMK): {}", encryptedPinBlockUnderLMK);
            log.info("Decryption Algorithm: AES/ECB");

            String clearPinBlockFromLMK = decryptPinBlock(encryptedPinBlockUnderLMK, lmkKey);
            log.info("Clear PIN Block (decrypted): {}", clearPinBlockFromLMK);

            // Step 5: Extract PIN from database PIN block
            log.info("----------------------------------------");
            log.info("STEP 5: Extract PIN from Database PIN Block");
            log.info("----------------------------------------");
            log.info("PIN Format: {}", pinFormat);
            log.info("Clear PIN Block: {}", clearPinBlockFromLMK);

            String pinFromDatabase = extractPinFromPinBlock(clearPinBlockFromLMK, pan, pinFormat);
            log.info("Extracted PIN from Database: {}", maskPin(pinFromDatabase));
            log.info("PIN Length: {}", pinFromDatabase.length());

            // Step 6: Compare PINs
            log.info("----------------------------------------");
            log.info("STEP 6: Compare Extracted PINs");
            log.info("----------------------------------------");
            log.info("Terminal PIN: {}", maskPin(pinFromTerminal));
            log.info("Database PIN: {}", maskPin(pinFromDatabase));

            boolean isValid = pinFromTerminal.equals(pinFromDatabase);

            log.info("Comparison Result: {}", isValid ? "MATCH" : "MISMATCH");
            log.info("========================================");
            log.info("VERIFICATION RESULT: {}", isValid ? "SUCCESS" : "FAILED");
            log.info("========================================");

            return isValid;

        } catch (Exception e) {
            log.error("========================================");
            log.error("VERIFICATION ERROR: {}", e.getMessage());
            log.error("========================================");
            throw new RuntimeException("PIN verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verify PIN using PVV (PIN Verification Value) method
     * Method: PVV Calculation and Comparison
     * This is the most common method in banking (ISO 9564)
     *
     * Flow:
     * 1. Terminal sends encrypted PIN block (under TPK) + PAN
     * 2. Core bank app retrieves stored PVV from database
     * 3. HSM decrypts PIN block to get clear PIN
     * 4. HSM calculates PVV from clear PIN + PAN using PVK
     * 5. HSM compares calculated PVV with stored PVV
     */
    @Transactional
    public boolean verifyPinWithPVV(
            String encryptedPinBlockUnderTPK,
            String storedPVV,
            String pan,
            String pinFormat,
            UUID tpkKeyId,
            UUID pvkKeyId) {

        log.info("========================================");
        log.info("PIN VERIFICATION - METHOD: PVV (PIN Verification Value)");
        log.info("========================================");
        log.info("Input Parameters:");
        log.info("  - PAN: {}", maskPan(pan));
        log.info("  - PIN Format: {}", pinFormat);
        log.info("  - Encrypted PIN Block (TPK): {}", encryptedPinBlockUnderTPK);
        log.info("  - Stored PVV: {}", storedPVV);

        try {
            // Step 1: Retrieve keys
            log.info("----------------------------------------");
            log.info("STEP 1: Retrieve Cryptographic Keys");
            log.info("----------------------------------------");

            MasterKey tpkKey = masterKeyRepository.findById(tpkKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("TPK key not found: " + tpkKeyId));
            log.info("TPK Key Retrieved:");
            log.info("  - Key ID: {}", tpkKey.getMasterKeyId());
            log.info("  - Key Type: {}", tpkKey.getKeyType());
            log.info("  - Algorithm: {}", tpkKey.getAlgorithm());

            MasterKey pvkKey = masterKeyRepository.findById(pvkKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("PVK key not found: " + pvkKeyId));
            log.info("PVK Key Retrieved:");
            log.info("  - Key ID: {}", pvkKey.getMasterKeyId());
            log.info("  - Key Type: {}", pvkKey.getKeyType());
            log.info("  - Algorithm: {}", pvkKey.getAlgorithm());

            // Validate key types
            if (tpkKey.getKeyType() != KeyType.TPK) {
                throw new IllegalArgumentException("PIN decryption key must be TPK, got: " + tpkKey.getKeyType());
            }
            if (pvkKey.getKeyType() != KeyType.LMK) {
                throw new IllegalArgumentException("PVV calculation key must be LMK (acting as PVK), got: " + pvkKey.getKeyType());
            }

            // Step 2: Decrypt PIN block under TPK
            log.info("----------------------------------------");
            log.info("STEP 2: Decrypt PIN Block from Terminal");
            log.info("----------------------------------------");
            log.info("Encrypted PIN Block (TPK): {}", encryptedPinBlockUnderTPK);
            log.info("Decryption Algorithm: AES/ECB");

            String clearPinBlock = decryptPinBlock(encryptedPinBlockUnderTPK, tpkKey);
            log.info("Clear PIN Block (decrypted): {}", clearPinBlock);

            // Step 3: Extract clear PIN
            log.info("----------------------------------------");
            log.info("STEP 3: Extract Clear PIN from PIN Block");
            log.info("----------------------------------------");
            log.info("PIN Format: {}", pinFormat);
            log.info("Clear PIN Block: {}", clearPinBlock);
            log.info("PAN (for XOR): {}", maskPan(pan));

            String clearPin = extractPinFromPinBlock(clearPinBlock, pan, pinFormat);
            log.info("Extracted Clear PIN: {}", maskPin(clearPin));
            log.info("PIN Length: {} digits", clearPin.length());

            // Step 4: Calculate PVV from clear PIN
            log.info("----------------------------------------");
            log.info("STEP 4: Calculate PVV (PIN Verification Value)");
            log.info("----------------------------------------");
            log.info("PVV Calculation Method: SHA-256 based");
            log.info("Input for PVV calculation:");
            log.info("  - Clear PIN: {}", maskPin(clearPin));
            log.info("  - PAN: {}", maskPan(pan));
            log.info("  - Concatenated: PIN + PAN");

            String calculatedPVV = generatePVV(clearPin, pan);
            log.info("Calculated PVV: {}", calculatedPVV);
            log.info("PVV Generation Algorithm:");
            log.info("  1. Concatenate PIN + PAN");
            log.info("  2. Apply SHA-256 hash function");
            log.info("  3. Extract first 4 decimal digits from hash");
            log.info("  4. Result: {}", calculatedPVV);

            // Step 5: Compare PVVs
            log.info("----------------------------------------");
            log.info("STEP 5: Compare Calculated vs Stored PVV");
            log.info("----------------------------------------");
            log.info("Stored PVV (from database): {}", storedPVV);
            log.info("Calculated PVV (from PIN): {}", calculatedPVV);

            boolean isValid = calculatedPVV.equals(storedPVV);

            log.info("Comparison Result: {}", isValid ? "MATCH" : "MISMATCH");

            if (!isValid) {
                log.warn("PVV Mismatch Details:");
                log.warn("  - Expected (stored): {}", storedPVV);
                log.warn("  - Received (calculated): {}", calculatedPVV);
                log.warn("  - Difference: PVVs do not match");
            }

            log.info("========================================");
            log.info("VERIFICATION RESULT: {}", isValid ? "SUCCESS" : "FAILED");
            log.info("========================================");

            return isValid;

        } catch (Exception e) {
            log.error("========================================");
            log.error("VERIFICATION ERROR: {}", e.getMessage());
            log.error("========================================");
            throw new RuntimeException("PIN verification with PVV failed: " + e.getMessage(), e);
        }
    }

    /**
     * Translate PIN block from TPK to ZPK (Acquirer side: Terminal to Zone)
     * Used when acquirer needs to forward PIN to issuer for inter-bank transaction
     */
    public String translateTpkToZpk(
            String encryptedPinBlockUnderTPK,
            String pan,
            String pinFormat,
            UUID tpkKeyId,
            UUID zpkKeyId) {

        log.info("========================================");
        log.info("PIN TRANSLATION: TPK → ZPK (Acquirer)");
        log.info("========================================");
        log.info("PAN: {}", maskPan(pan));
        log.info("PIN Format: {}", pinFormat);
        log.info("TPK Key ID: {}", tpkKeyId);
        log.info("ZPK Key ID: {}", zpkKeyId);

        try {
            // Step 1: Retrieve TPK key
            MasterKey tpkKey = masterKeyRepository.findById(tpkKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("TPK key not found: " + tpkKeyId));

            if (tpkKey.getKeyType() != KeyType.TPK) {
                throw new IllegalArgumentException("Source key must be TPK, got: " + tpkKey.getKeyType());
            }

            log.info("TPK Key: {} ({})", tpkKey.getMasterKeyId(), tpkKey.getKeyType());

            // Step 2: Retrieve ZPK key
            MasterKey zpkKey = masterKeyRepository.findById(zpkKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("ZPK key not found: " + zpkKeyId));

            if (zpkKey.getKeyType() != KeyType.ZPK) {
                throw new IllegalArgumentException("Target key must be ZPK, got: " + zpkKey.getKeyType());
            }

            log.info("ZPK Key: {} ({})", zpkKey.getMasterKeyId(), zpkKey.getKeyType());

            // Step 3: Decrypt PIN block under TPK
            log.info("Step 1: Decrypting PIN block under TPK");
            String clearPinBlock = decryptPinBlock(encryptedPinBlockUnderTPK, tpkKey);
            log.info("Clear PIN block: {}", clearPinBlock);

            // Step 4: Extract PIN from clear PIN block
            log.info("Step 2: Extracting PIN from clear PIN block");
            String pin = extractPinFromPinBlock(clearPinBlock, pan, pinFormat);
            log.info("Extracted PIN: {}", maskPin(pin));

            // Step 5: Create new PIN block
            log.info("Step 3: Creating PIN block for ZPK encryption");
            String newPinBlock = createPinBlock(pin, pan, pinFormat);
            log.info("New PIN block: {}", newPinBlock);

            // Step 6: Encrypt PIN block under ZPK
            log.info("Step 4: Encrypting PIN block under ZPK");
            String encryptedPinBlockUnderZPK = encryptPinBlock(newPinBlock, zpkKey);
            log.info("Encrypted PIN block under ZPK: {}", encryptedPinBlockUnderZPK);

            log.info("========================================");
            log.info("TRANSLATION COMPLETE: TPK → ZPK");
            log.info("========================================");

            return encryptedPinBlockUnderZPK;

        } catch (Exception e) {
            log.error("========================================");
            log.error("TRANSLATION ERROR (TPK → ZPK): {}", e.getMessage());
            log.error("========================================", e);
            throw new RuntimeException("PIN translation TPK to ZPK failed: " + e.getMessage(), e);
        }
    }

    /**
     * Translate PIN block from ZPK to LMK (Issuer side: Zone to Storage)
     * Used when issuer receives PIN from acquirer and needs to verify against stored PIN
     */
    public String translateZpkToLmk(
            String encryptedPinBlockUnderZPK,
            String pan,
            String pinFormat,
            UUID zpkKeyId,
            UUID lmkKeyId) {

        log.info("========================================");
        log.info("PIN TRANSLATION: ZPK → LMK (Issuer)");
        log.info("========================================");
        log.info("PAN: {}", maskPan(pan));
        log.info("PIN Format: {}", pinFormat);
        log.info("ZPK Key ID: {}", zpkKeyId);
        log.info("LMK Key ID: {}", lmkKeyId);

        try {
            // Step 1: Retrieve ZPK key
            MasterKey zpkKey = masterKeyRepository.findById(zpkKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("ZPK key not found: " + zpkKeyId));

            if (zpkKey.getKeyType() != KeyType.ZPK) {
                throw new IllegalArgumentException("Source key must be ZPK, got: " + zpkKey.getKeyType());
            }

            log.info("ZPK Key: {} ({})", zpkKey.getMasterKeyId(), zpkKey.getKeyType());

            // Step 2: Retrieve LMK key
            MasterKey lmkKey = masterKeyRepository.findById(lmkKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("LMK key not found: " + lmkKeyId));

            if (lmkKey.getKeyType() != KeyType.LMK) {
                throw new IllegalArgumentException("Target key must be LMK, got: " + lmkKey.getKeyType());
            }

            log.info("LMK Key: {} ({})", lmkKey.getMasterKeyId(), lmkKey.getKeyType());

            // Step 3: Decrypt PIN block under ZPK
            log.info("Step 1: Decrypting PIN block under ZPK");
            String clearPinBlock = decryptPinBlock(encryptedPinBlockUnderZPK, zpkKey);
            log.info("Clear PIN block: {}", clearPinBlock);

            // Step 4: Extract PIN from clear PIN block
            log.info("Step 2: Extracting PIN from clear PIN block");
            String pin = extractPinFromPinBlock(clearPinBlock, pan, pinFormat);
            log.info("Extracted PIN: {}", maskPin(pin));

            // Step 5: Create new PIN block
            log.info("Step 3: Creating PIN block for LMK encryption");
            String newPinBlock = createPinBlock(pin, pan, pinFormat);
            log.info("New PIN block: {}", newPinBlock);

            // Step 6: Encrypt PIN block under LMK
            log.info("Step 4: Encrypting PIN block under LMK");
            String encryptedPinBlockUnderLMK = encryptPinBlock(newPinBlock, lmkKey);
            log.info("Encrypted PIN block under LMK: {}", encryptedPinBlockUnderLMK);

            log.info("========================================");
            log.info("TRANSLATION COMPLETE: ZPK → LMK");
            log.info("========================================");

            return encryptedPinBlockUnderLMK;

        } catch (Exception e) {
            log.error("========================================");
            log.error("TRANSLATION ERROR (ZPK → LMK): {}", e.getMessage());
            log.error("========================================", e);
            throw new RuntimeException("PIN translation ZPK to LMK failed: " + e.getMessage(), e);
        }
    }

    private String maskPin(String pin) {
        if (pin == null || pin.length() < 2) return "****";
        return pin.charAt(0) + "***" + pin.charAt(pin.length() - 1);
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) return "******";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
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
            // Derive operational PIN key using context
            String context = keyGenerationService.buildKeyContext(
                key.getKeyType().toString(),
                key.getIdBank() != null ? key.getIdBank().toString() : "GLOBAL",
                "PIN"
            );

            byte[] pinKeyBytes = getOrDerivePinKey(key, context);

            // Use AES-128 CBC mode for secure PIN block encryption
            SecretKeySpec secretKey = new SecretKeySpec(pinKeyBytes, CryptoConstants.MASTER_KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(CryptoConstants.PIN_CIPHER);

            // Generate random IV for CBC mode
            byte[] iv = new byte[CryptoConstants.CBC_IV_BYTES];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            // Convert hex PIN block to bytes
            byte[] pinBlockBytes = hexToBytes(pinBlock);

            // Encrypt (PKCS5Padding handles padding automatically)
            byte[] encrypted = cipher.doFinal(pinBlockBytes);

            // Prepend IV to encrypted data (IV:ciphertext)
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return bytesToHex(result);
        } catch (Exception e) {
            log.error("Failed to encrypt PIN block", e);
            throw new RuntimeException("Failed to encrypt PIN block", e);
        }
    }

    private String decryptPinBlock(String encryptedPinBlock, MasterKey key) {
        try {
            // Derive operational PIN key using context
            String context = keyGenerationService.buildKeyContext(
                key.getKeyType().toString(),
                key.getIdBank() != null ? key.getIdBank().toString() : "GLOBAL",
                "PIN"
            );

            byte[] pinKeyBytes = getOrDerivePinKey(key, context);

            // Use AES-128 CBC mode
            SecretKeySpec secretKey = new SecretKeySpec(pinKeyBytes, CryptoConstants.MASTER_KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(CryptoConstants.PIN_CIPHER);

            // Convert hex to bytes
            byte[] encryptedBytes = hexToBytes(encryptedPinBlock);

            // Extract IV and ciphertext
            byte[] iv = new byte[CryptoConstants.CBC_IV_BYTES];
            byte[] ciphertext = new byte[encryptedBytes.length - CryptoConstants.CBC_IV_BYTES];
            System.arraycopy(encryptedBytes, 0, iv, 0, CryptoConstants.CBC_IV_BYTES);
            System.arraycopy(encryptedBytes, CryptoConstants.CBC_IV_BYTES, ciphertext, 0, ciphertext.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Decrypt
            byte[] decrypted = cipher.doFinal(ciphertext);
            return bytesToHex(decrypted);
        } catch (Exception e) {
            log.error("Failed to decrypt PIN block", e);
            throw new RuntimeException("Failed to decrypt PIN block", e);
        }
    }

    /**
     * Get derived PIN key from cache or derive it
     */
    private byte[] getOrDerivePinKey(MasterKey key, String context) {
        String cacheKey = key.getId() + ":" + context;
        return derivedKeyCache.computeIfAbsent(cacheKey, k -> {
            log.debug("Deriving new PIN key: {}", cacheKey);
            return keyGenerationService.deriveOperationalKey(
                key.getKeyData(),
                context,
                CryptoConstants.PIN_KEY_BYTES  // 16 bytes for AES-128
            );
        });
    }

    /**
     * Clear the derived key cache (useful for testing or key rotation)
     */
    public void clearKeyCache() {
        derivedKeyCache.clear();
        log.info("Cleared PIN key cache");
    }

    private String extractPinFromPinBlock(String clearPinBlock, String pan, String format) {
        return switch (format) {
            case "ISO-0" -> extractPinFromISO0(clearPinBlock, pan);
            case "ISO-1" -> extractPinFromISO1(clearPinBlock);
            case "ISO-3" -> extractPinFromISO3(clearPinBlock, pan);
            case "ISO-4" -> extractPinFromISO4(clearPinBlock, pan);
            default -> throw new IllegalArgumentException("Unsupported PIN format: " + format);
        };
    }

    private String extractPinFromISO0(String pinBlock, String pan) {
        // Reverse XOR operation
        String panField = "0000" + pan.substring(pan.length() - 13, pan.length() - 1);
        String pinField = xorHex(pinBlock, panField);

        // Extract length and PIN
        int pinLength = Character.digit(pinField.charAt(1), 16);
        return pinField.substring(2, 2 + pinLength);
    }

    private String extractPinFromISO1(String pinBlock) {
        // Extract length and PIN (no XOR)
        int pinLength = Character.digit(pinBlock.charAt(1), 16);
        return pinBlock.substring(2, 2 + pinLength);
    }

    private String extractPinFromISO3(String pinBlock, String pan) {
        // Similar to ISO-0 but with format indicator 3
        String panField = "0000" + pan.substring(pan.length() - 13, pan.length() - 1);
        String pinField = xorHex(pinBlock, panField);

        int pinLength = Character.digit(pinField.charAt(1), 16);
        return pinField.substring(2, 2 + pinLength);
    }

    private String extractPinFromISO4(String pinBlock, String pan) {
        // Similar to ISO-0 but with format indicator 4
        String panField = "0000" + pan.substring(pan.length() - 13, pan.length() - 1);
        String pinField = xorHex(pinBlock, panField);

        int pinLength = Character.digit(pinField.charAt(1), 16);
        return pinField.substring(2, 2 + pinLength);
    }

    private String generatePVV(String pin, String accountNumber) {
        try {
            String input = pin + accountNumber;
            MessageDigest digest = MessageDigest.getInstance(CryptoConstants.HASH_ALGORITHM);
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
