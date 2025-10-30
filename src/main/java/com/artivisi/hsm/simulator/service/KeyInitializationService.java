package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.Bank;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.entity.Terminal;
import com.artivisi.hsm.simulator.repository.BankRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import com.artivisi.hsm.simulator.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for initializing complete key sets for HSM instances
 * Useful for setting up multiple HSM instances for zone translation testing
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeyInitializationService {

    private final MasterKeyRepository masterKeyRepository;
    private final BankRepository bankRepository;
    private final TerminalRepository terminalRepository;
    private final KeyOperationService keyOperationService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Initialize complete key set for all banks or specific bank
     * Clears existing sample keys and creates comprehensive key hierarchy
     *
     * @param clearExisting whether to clear existing sample keys
     * @param keySize key size in bits (128, 192, or 256)
     * @param bankCode optional bank code to initialize keys for specific bank only (e.g., "ISS001")
     * @param shareZoneKeysWith optional bank code to copy zone keys from (for shared HSM setup)
     */
    @Transactional
    public Map<String, Object> initializeAllKeys(boolean clearExisting, Integer keySize, String bankCode, String shareZoneKeysWith) {
        log.info("========================================");
        log.info("KEY INITIALIZATION - COMPLETE KEY SET");
        log.info("========================================");
        log.info("Parameters:");
        log.info("  - Bank Code: {}", bankCode == null ? "ALL BANKS" : bankCode);
        log.info("  - Share Zone Keys With: {}", shareZoneKeysWith == null ? "CREATE NEW" : shareZoneKeysWith);
        log.info("  - Clear Existing: {}", clearExisting);
        log.info("  - Key Size: {} bits", keySize);

        Map<String, Object> result = new HashMap<>();
        List<String> createdKeys = new ArrayList<>();
        List<String> clearedKeys = new ArrayList<>();

        try {
            // Step 1: Clear existing keys if requested
            if (clearExisting) {
                log.info("----------------------------------------");
                log.info("STEP 1: Clearing Existing Keys");
                log.info("----------------------------------------");
                if (bankCode != null && !bankCode.isEmpty()) {
                    // Clear keys for specific bank
                    Bank bank = bankRepository.findByBankCode(bankCode)
                            .orElseThrow(() -> new RuntimeException("Bank not found: " + bankCode));
                    clearedKeys = clearKeysForBank(bank);
                    log.info("Cleared {} keys for bank {}", clearedKeys.size(), bankCode);
                } else {
                    // Clear all keys (including sample keys)
                    clearedKeys = clearAllKeys();
                    log.info("Cleared {} keys (all banks)", clearedKeys.size());
                }
                result.put("clearedKeys", clearedKeys);
            }

            // Step 2: Create keys for each bank
            log.info("----------------------------------------");
            log.info("STEP 2: Creating Bank-Specific Keys");
            log.info("----------------------------------------");

            // Filter banks by bankCode if provided
            List<Bank> banks;
            if (bankCode != null && !bankCode.isEmpty()) {
                Bank bank = bankRepository.findByBankCode(bankCode)
                        .orElseThrow(() -> new RuntimeException("Bank not found: " + bankCode));
                banks = List.of(bank);
                log.info("Initializing keys for single bank: {}", bankCode);
            } else {
                banks = bankRepository.findAll();
                log.info("Initializing keys for all banks: {} banks found", banks.size());
            }

            // Step 3a: Load source zone keys if sharing
            Map<KeyType, MasterKey> sourceZoneKeys = new HashMap<>();
            if (shareZoneKeysWith != null && !shareZoneKeysWith.isEmpty()) {
                log.info("Loading zone keys from source bank: {}", shareZoneKeysWith);
                Bank sourceBank = bankRepository.findByBankCode(shareZoneKeysWith)
                        .orElseThrow(() -> new RuntimeException("Source bank not found for zone key sharing: " + shareZoneKeysWith));

                List<MasterKey> zoneKeys = masterKeyRepository.findAll().stream()
                        .filter(key -> key.getIdBank() != null && key.getIdBank().equals(sourceBank.getId()))
                        .filter(key -> key.getKeyType() == KeyType.ZMK ||
                                     key.getKeyType() == KeyType.ZPK ||
                                     key.getKeyType() == KeyType.ZSK)
                        .collect(Collectors.toList());

                for (MasterKey key : zoneKeys) {
                    sourceZoneKeys.put(key.getKeyType(), key);
                    log.info("Found source zone key: {} ({})", key.getMasterKeyId(), key.getKeyType());
                }

                if (sourceZoneKeys.isEmpty()) {
                    throw new RuntimeException("No zone keys found for source bank: " + shareZoneKeysWith);
                }

                log.info("Loaded {} zone keys from {}", sourceZoneKeys.size(), shareZoneKeysWith);
            }

            Map<String, List<String>> bankKeys = new HashMap<>();

            for (Bank bank : banks) {
                List<String> keysForBank = new ArrayList<>();
                log.info("Processing bank: {} ({})", bank.getBankName(), bank.getBankCode());

                // Create LMK (Local Master Key) for this bank
                MasterKey lmk = createBankLMK(bank, keySize);
                keysForBank.add(lmk.getMasterKeyId());
                createdKeys.add(lmk.getMasterKeyId());
                log.info("Created LMK for {}: {}", bank.getBankCode(), lmk.getMasterKeyId());

                // Create TMK (Terminal Master Key)
                MasterKey tmk = createTMK(bank, keySize);
                keysForBank.add(tmk.getMasterKeyId());
                createdKeys.add(tmk.getMasterKeyId());

                // Create ZMK (Zone Master Key) - or copy from source
                MasterKey zmk;
                if (sourceZoneKeys.containsKey(KeyType.ZMK)) {
                    log.info("Copying ZMK from source bank");
                    zmk = copyZoneKey(sourceZoneKeys.get(KeyType.ZMK), bank, "ZMK");
                } else {
                    zmk = createZMK(bank, keySize);
                }
                keysForBank.add(zmk.getMasterKeyId());
                createdKeys.add(zmk.getMasterKeyId());

                // Create child keys for terminals (TPK, TSK)
                List<Terminal> terminals = terminalRepository.findByBank(bank);
                for (Terminal terminal : terminals) {
                    // TPK (Terminal PIN Key) - child of TMK
                    MasterKey tpk = createTPK(tmk, terminal, keySize);
                    keysForBank.add(tpk.getMasterKeyId());
                    createdKeys.add(tpk.getMasterKeyId());

                    // TSK (Terminal Security Key) - child of TMK
                    MasterKey tsk = createTSK(tmk, terminal, keySize);
                    keysForBank.add(tsk.getMasterKeyId());
                    createdKeys.add(tsk.getMasterKeyId());
                }

                // Create zone keys for inter-bank communication
                // ZPK (Zone PIN Key) - child of ZMK - or copy from source
                MasterKey zpk;
                if (sourceZoneKeys.containsKey(KeyType.ZPK)) {
                    log.info("Copying ZPK from source bank");
                    zpk = copyZoneKey(sourceZoneKeys.get(KeyType.ZPK), bank, "ZPK");
                } else {
                    zpk = createZPK(zmk, bank, keySize);
                }
                keysForBank.add(zpk.getMasterKeyId());
                createdKeys.add(zpk.getMasterKeyId());

                // ZSK (Zone Session Key) - child of ZMK - or copy from source
                MasterKey zsk;
                if (sourceZoneKeys.containsKey(KeyType.ZSK)) {
                    log.info("Copying ZSK from source bank");
                    zsk = copyZoneKey(sourceZoneKeys.get(KeyType.ZSK), bank, "ZSK");
                } else {
                    zsk = createZSK(zmk, bank, keySize);
                }
                keysForBank.add(zsk.getMasterKeyId());
                createdKeys.add(zsk.getMasterKeyId());

                bankKeys.put(bank.getBankCode(), keysForBank);
                log.info("Created {} keys for bank {}", keysForBank.size(), bank.getBankCode());
            }

            log.info("========================================");
            log.info("INITIALIZATION COMPLETE");
            log.info("========================================");
            log.info("Total Keys Created: {}", createdKeys.size());
            log.info("Banks Configured: {}", banks.size());

            result.put("success", true);
            result.put("totalKeysCreated", createdKeys.size());
            result.put("banksConfigured", banks.size());
            result.put("createdKeys", createdKeys);
            result.put("keysByBank", bankKeys);
            result.put("keyHierarchy", buildKeyHierarchySummary());

            return result;

        } catch (Exception e) {
            log.error("========================================");
            log.error("INITIALIZATION FAILED: {}", e.getMessage());
            log.error("========================================", e);
            throw new RuntimeException("Key initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Clear all keys for a specific bank
     */
    private List<String> clearKeysForBank(Bank bank) {
        List<String> clearedKeys = new ArrayList<>();

        // Find all terminals for this bank
        List<Terminal> terminals = terminalRepository.findByBank(bank);
        List<UUID> terminalIds = terminals.stream()
                .map(Terminal::getId)
                .collect(Collectors.toList());

        // Find and delete all keys associated with this bank
        List<MasterKey> bankKeys = masterKeyRepository.findAll().stream()
                .filter(key -> {
                    // Keys directly linked to the bank
                    if (key.getIdBank() != null && key.getIdBank().equals(bank.getId())) {
                        return true;
                    }
                    // Keys linked to terminals of this bank
                    if (key.getIdTerminal() != null && terminalIds.contains(key.getIdTerminal())) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        for (MasterKey key : bankKeys) {
            log.info("Deleting key: {} ({})", key.getMasterKeyId(), key.getKeyType());
            clearedKeys.add(key.getMasterKeyId());
            masterKeyRepository.delete(key);
        }

        return clearedKeys;
    }

    /**
     * Clear all keys (for complete reset)
     */
    private List<String> clearAllKeys() {
        List<String> clearedKeys = new ArrayList<>();

        List<MasterKey> allKeys = masterKeyRepository.findAll();
        for (MasterKey key : allKeys) {
            log.info("Deleting key: {} ({})", key.getMasterKeyId(), key.getKeyType());
            clearedKeys.add(key.getMasterKeyId());
        }

        // Delete all at once for efficiency
        masterKeyRepository.deleteAll();

        return clearedKeys;
    }

    /**
     * Create Bank-Specific LMK (Local Master Key) for PIN storage
     */
    private MasterKey createBankLMK(Bank bank, Integer keySize) {
        byte[] keyData = generateRandomKey(keySize);
        String masterKeyId = "LMK-" + bank.getBankCode() + "-" + generateShortId();

        MasterKey key = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .keyType(KeyType.LMK)
                .idBank(bank.getId())
                .algorithm("AES")
                .keySize(keySize)
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .generationMethod("SECURE_RANDOM")
                .kdfIterations(0)
                .kdfSalt("N/A")
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(key);
    }

    /**
     * Create TMK (Terminal Master Key) for a bank
     */
    private MasterKey createTMK(Bank bank, Integer keySize) {
        byte[] keyData = generateRandomKey(keySize);
        String masterKeyId = "TMK-" + bank.getBankCode() + "-" + generateShortId();

        MasterKey key = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .keyType(KeyType.TMK)
                .idBank(bank.getId())
                .algorithm("AES")
                .keySize(keySize)
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .generationMethod("SECURE_RANDOM")
                .kdfIterations(0)
                .kdfSalt("N/A")
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(key);
    }

    /**
     * Create ZMK (Zone Master Key) for a bank
     */
    private MasterKey createZMK(Bank bank, Integer keySize) {
        byte[] keyData = generateRandomKey(keySize);
        String masterKeyId = "ZMK-" + bank.getBankCode() + "-" + generateShortId();

        MasterKey key = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .keyType(KeyType.ZMK)
                .idBank(bank.getId())
                .algorithm("AES")
                .keySize(keySize)
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .generationMethod("SECURE_RANDOM")
                .kdfIterations(0)
                .kdfSalt("N/A")
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(key);
    }

    /**
     * Create TPK (Terminal PIN Key) - child of TMK using PBKDF2 derivation
     */
    private MasterKey createTPK(MasterKey tmk, Terminal terminal, Integer keySize) {
        log.debug("Creating TPK for terminal {} using PBKDF2 derivation from TMK {}",
                  terminal.getTerminalId(), tmk.getMasterKeyId());

        return keyOperationService.generateTPK(
            tmk.getId(),
            terminal.getId(),
            "Auto-generated during key initialization"
        );
    }

    /**
     * Create TSK (Terminal Security Key) - child of TMK using PBKDF2 derivation
     */
    private MasterKey createTSK(MasterKey tmk, Terminal terminal, Integer keySize) {
        log.debug("Creating TSK for terminal {} using PBKDF2 derivation from TMK {}",
                  terminal.getTerminalId(), tmk.getMasterKeyId());

        return keyOperationService.generateTSK(
            tmk.getId(),
            terminal.getId(),
            "Auto-generated during key initialization"
        );
    }

    /**
     * Create ZPK (Zone PIN Key) - child of ZMK using PBKDF2 derivation
     */
    private MasterKey createZPK(MasterKey zmk, Bank bank, Integer keySize) {
        log.debug("Creating ZPK for bank {} using PBKDF2 derivation from ZMK {}",
                  bank.getBankCode(), zmk.getMasterKeyId());

        return keyOperationService.generateZPK(
            zmk.getId(),
            bank.getBankCode(),
            "Auto-generated during key initialization"
        );
    }

    /**
     * Create ZSK (Zone Session Key) - child of ZMK using PBKDF2 derivation
     */
    private MasterKey createZSK(MasterKey zmk, Bank bank, Integer keySize) {
        log.debug("Creating ZSK for bank {} using PBKDF2 derivation from ZMK {}",
                  bank.getBankCode(), zmk.getMasterKeyId());

        return keyOperationService.generateZSK(
            zmk.getId(),
            bank.getBankCode(),
            "Auto-generated during key initialization"
        );
    }

    /**
     * Build key hierarchy summary
     */
    private Map<String, Object> buildKeyHierarchySummary() {
        Map<String, Object> hierarchy = new HashMap<>();

        List<MasterKey> allKeys = masterKeyRepository.findAll();

        // Count by type
        Map<KeyType, Long> keysByType = allKeys.stream()
                .collect(Collectors.groupingBy(MasterKey::getKeyType, Collectors.counting()));

        hierarchy.put("totalKeys", allKeys.size());
        hierarchy.put("keysByType", keysByType);

        // Parent-child relationships
        List<String> parentChildRelations = allKeys.stream()
                .filter(key -> key.getParentKeyId() != null)
                .map(key -> {
                    String parentKeyId = allKeys.stream()
                            .filter(parent -> parent.getId().equals(key.getParentKeyId()))
                            .map(MasterKey::getMasterKeyId)
                            .findFirst()
                            .orElse("UNKNOWN");
                    return key.getMasterKeyId() + " → " + parentKeyId;
                })
                .collect(Collectors.toList());

        hierarchy.put("parentChildRelations", parentChildRelations);

        return hierarchy;
    }

    /**
     * Copy zone key from source bank to target bank (for shared HSM setup)
     */
    private MasterKey copyZoneKey(MasterKey sourceKey, Bank targetBank, String keyTypePrefix) {
        String masterKeyId = keyTypePrefix + "-" + targetBank.getBankCode() + "-SHARED-" + generateShortId();

        MasterKey key = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .keyType(sourceKey.getKeyType())
                .idBank(targetBank.getId())
                .parentKeyId(sourceKey.getParentKeyId()) // Keep parent relationship if exists
                .algorithm(sourceKey.getAlgorithm())
                .keySize(sourceKey.getKeySize())
                .keyData(sourceKey.getKeyData()) // COPY SAME KEY MATERIAL
                .keyFingerprint(sourceKey.getKeyFingerprint())
                .keyChecksum(sourceKey.getKeyChecksum())
                .generationMethod("SHARED_FROM_" + sourceKey.getMasterKeyId())
                .kdfIterations(0)
                .kdfSalt("N/A")
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        log.info("Copied zone key {} from {} with same key material", masterKeyId, sourceKey.getMasterKeyId());
        return masterKeyRepository.save(key);
    }

    // Helper methods

    private byte[] generateRandomKey(Integer keySize) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize, secureRandom);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate key", e);
        }
    }

    private String generateFingerprint(byte[] keyData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyData);
            return bytesToHex(hash).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate fingerprint", e);
        }
    }

    private String generateChecksum(byte[] keyData) {
        try {
            // Use SHA-256 instead of MD5 for checksums
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyData);
            return bytesToHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate checksum", e);
        }
    }

    private String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
