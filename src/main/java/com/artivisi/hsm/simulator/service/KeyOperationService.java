package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.Bank;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.entity.Terminal;
import com.artivisi.hsm.simulator.repository.BankRepository;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import com.artivisi.hsm.simulator.repository.TerminalRepository;
import com.artivisi.hsm.simulator.util.CryptoUtils;
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
import java.util.UUID;

/**
 * Service for key operations: generation, revocation, rotation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeyOperationService {

    private final MasterKeyRepository masterKeyRepository;
    private final BankRepository bankRepository;
    private final TerminalRepository terminalRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new Terminal Master Key (TMK) for a bank
     */
    @Transactional
    public MasterKey generateTMK(UUID bankId, Integer keySize, String description) {
        log.info("Generating TMK for bank: {}, keySize: {}", bankId, keySize);

        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new IllegalArgumentException("Bank not found: " + bankId));

        byte[] keyData = generateRandomKey(keySize);
        String masterKeyId = generateKeyId("TMK", bank.getBankCode());

        MasterKey tmk = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .keyType(KeyType.TMK)
                .algorithm("AES")
                .keySize(keySize)
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .combinedEntropyHash(generateEntropyHash(keyData))
                .generationMethod("RANDOM")
                .kdfIterations(0)
                .kdfSalt("N/A")
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(tmk);
    }

    /**
     * Generate a new Terminal PIN Key (TPK) derived from TMK
     */
    @Transactional
    public MasterKey generateTPK(UUID tmkId, UUID terminalId, String description) {
        log.info("Generating TPK from TMK: {} for terminal: {}", tmkId, terminalId);

        MasterKey tmk = masterKeyRepository.findById(tmkId)
                .orElseThrow(() -> new IllegalArgumentException("TMK not found: " + tmkId));

        Terminal terminal = terminalRepository.findById(terminalId)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found: " + terminalId));

        if (tmk.getKeyType() != KeyType.TMK) {
            throw new IllegalArgumentException("Parent key must be TMK, got: " + tmk.getKeyType());
        }

        byte[] keyData = deriveKeyFromParent(tmk.getKeyData(), "TPK", terminal.getTerminalId());
        String masterKeyId = generateKeyId("TPK", terminal.getTerminalId());

        MasterKey tpk = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .parentKeyId(tmk.getId())
                .idBank(terminal.getBank().getId())
                .idTerminal(terminal.getId())
                .keyType(KeyType.TPK)
                .algorithm("AES")
                .keySize(tmk.getKeySize())
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .combinedEntropyHash(generateEntropyHash(keyData))
                .generationMethod("DERIVED")
                .kdfIterations(100000)
                .kdfSalt(terminal.getTerminalId())
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(tpk);
    }

    /**
     * Generate a new Terminal Security Key (TSK) derived from TMK
     */
    @Transactional
    public MasterKey generateTSK(UUID tmkId, UUID terminalId, String description) {
        log.info("Generating TSK from TMK: {} for terminal: {}", tmkId, terminalId);

        MasterKey tmk = masterKeyRepository.findById(tmkId)
                .orElseThrow(() -> new IllegalArgumentException("TMK not found: " + tmkId));

        Terminal terminal = terminalRepository.findById(terminalId)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found: " + terminalId));

        if (tmk.getKeyType() != KeyType.TMK) {
            throw new IllegalArgumentException("Parent key must be TMK, got: " + tmk.getKeyType());
        }

        byte[] keyData = deriveKeyFromParent(tmk.getKeyData(), "TSK", terminal.getTerminalId());
        String masterKeyId = generateKeyId("TSK", terminal.getTerminalId());

        MasterKey tsk = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .parentKeyId(tmk.getId())
                .idBank(terminal.getBank().getId())
                .idTerminal(terminal.getId())
                .keyType(KeyType.TSK)
                .algorithm("AES")
                .keySize(tmk.getKeySize())
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .combinedEntropyHash(generateEntropyHash(keyData))
                .generationMethod("DERIVED")
                .kdfIterations(100000)
                .kdfSalt(terminal.getTerminalId())
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(tsk);
    }

    /**
     * Generate a new Zone Master Key (ZMK) for inter-bank communication
     */
    @Transactional
    public MasterKey generateZMK(UUID bankId, Integer keySize, String description) {
        log.info("Generating ZMK for bank: {}, keySize: {}", bankId, keySize);

        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new IllegalArgumentException("Bank not found: " + bankId));

        byte[] keyData = generateRandomKey(keySize);
        String masterKeyId = generateKeyId("ZMK", bank.getBankCode());

        MasterKey zmk = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .keyType(KeyType.ZMK)
                .algorithm("AES")
                .keySize(keySize)
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .combinedEntropyHash(generateEntropyHash(keyData))
                .generationMethod("RANDOM")
                .kdfIterations(0)
                .kdfSalt("N/A")
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(zmk);
    }

    /**
     * Generate a new Zone PIN Key (ZPK) derived from ZMK
     */
    @Transactional
    public MasterKey generateZPK(UUID zmkId, String zoneIdentifier, String description) {
        log.info("Generating ZPK from ZMK: {} for zone: {}", zmkId, zoneIdentifier);

        MasterKey zmk = masterKeyRepository.findById(zmkId)
                .orElseThrow(() -> new IllegalArgumentException("ZMK not found: " + zmkId));

        if (zmk.getKeyType() != KeyType.ZMK) {
            throw new IllegalArgumentException("Parent key must be ZMK, got: " + zmk.getKeyType());
        }

        byte[] keyData = deriveKeyFromParent(zmk.getKeyData(), "ZPK", zoneIdentifier);
        String masterKeyId = generateKeyId("ZPK", zoneIdentifier);

        MasterKey zpk = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .parentKeyId(zmk.getId())
                .keyType(KeyType.ZPK)
                .algorithm("AES")
                .keySize(zmk.getKeySize())
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .combinedEntropyHash(generateEntropyHash(keyData))
                .generationMethod("DERIVED")
                .kdfIterations(100000)
                .kdfSalt(zoneIdentifier)
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(zpk);
    }

    /**
     * Generate a new Zone Session Key (ZSK) derived from ZMK
     */
    @Transactional
    public MasterKey generateZSK(UUID zmkId, String zoneIdentifier, String description) {
        log.info("Generating ZSK from ZMK: {} for zone: {}", zmkId, zoneIdentifier);

        MasterKey zmk = masterKeyRepository.findById(zmkId)
                .orElseThrow(() -> new IllegalArgumentException("ZMK not found: " + zmkId));

        if (zmk.getKeyType() != KeyType.ZMK) {
            throw new IllegalArgumentException("Parent key must be ZMK, got: " + zmk.getKeyType());
        }

        byte[] keyData = deriveKeyFromParent(zmk.getKeyData(), "ZSK", zoneIdentifier);
        String masterKeyId = generateKeyId("ZSK", zoneIdentifier);

        MasterKey zsk = MasterKey.builder()
                .masterKeyId(masterKeyId)
                .parentKeyId(zmk.getId())
                .keyType(KeyType.ZSK)
                .algorithm("AES")
                .keySize(zmk.getKeySize())
                .keyData(keyData)
                .keyFingerprint(generateFingerprint(keyData))
                .keyChecksum(generateChecksum(keyData))
                .combinedEntropyHash(generateEntropyHash(keyData))
                .generationMethod("DERIVED")
                .kdfIterations(100000)
                .kdfSalt(zoneIdentifier)
                .status(MasterKey.KeyStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        return masterKeyRepository.save(zsk);
    }

    /**
     * Revoke a key with reason
     */
    @Transactional
    public void revokeKey(UUID keyId, String reason, String revokedBy) {
        log.info("Revoking key: {}, reason: {}, by: {}", keyId, reason, revokedBy);

        MasterKey key = masterKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("Key not found: " + keyId));

        if (key.getStatus() == MasterKey.KeyStatus.REVOKED) {
            throw new IllegalStateException("Key is already revoked: " + key.getMasterKeyId());
        }

        key.setStatus(MasterKey.KeyStatus.REVOKED);
        key.setRevokedAt(LocalDateTime.now());
        key.setRevocationReason(reason);

        masterKeyRepository.save(key);
        log.info("Key revoked successfully: {}", key.getMasterKeyId());
    }

    // ===== Helper Methods =====

    /**
     * Generate a random cryptographic key
     */
    private byte[] generateRandomKey(int keySize) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize, secureRandom);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate random key", e);
        }
    }

    /**
     * Derive a key from parent key using PBKDF2
     */
    private byte[] deriveKeyFromParent(byte[] parentKey, String keyType, String salt) {
        try {
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            // Use parent key + key type as password material
            String passwordMaterial = CryptoUtils.bytesToHex(parentKey) + keyType;
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    passwordMaterial.toCharArray(),
                    salt.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    100000,
                    256
            );
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive key from parent", e);
        }
    }

    /**
     * Generate a unique key ID
     */
    private String generateKeyId(String keyType, String identifier) {
        return String.format("%s-%s-%s",
                keyType,
                identifier.replaceAll("[^A-Za-z0-9]", "").toUpperCase(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    /**
     * Generate SHA-256 fingerprint
     */
    private String generateFingerprint(byte[] keyData) {
        return CryptoUtils.generateFullHash(keyData);
    }

    /**
     * Generate SHA-256 checksum (replaces MD5 for security)
     */
    private String generateChecksum(byte[] keyData) {
        return CryptoUtils.generateFullHash(keyData);
    }

    /**
     * Generate entropy hash
     */
    private String generateEntropyHash(byte[] keyData) {
        return generateFingerprint(keyData);
    }

    /**
     * Get key data in plaintext (hex encoded) for educational purposes
     */
    public String getKeyPlaintext(UUID keyId) {
        log.info("Retrieving plaintext for key: {}", keyId);

        MasterKey key = masterKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("Key not found: " + keyId));

        return CryptoUtils.bytesToHex(key.getKeyData());
    }
}
