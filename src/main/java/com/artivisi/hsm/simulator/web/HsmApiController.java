package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.dto.KeyExchangeRequest;
import com.artivisi.hsm.simulator.dto.KeyExchangeResponse;
import com.artivisi.hsm.simulator.dto.PinEncryptRequest;
import com.artivisi.hsm.simulator.dto.PinEncryptResponse;
import com.artivisi.hsm.simulator.entity.GeneratedPin;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import com.artivisi.hsm.simulator.service.KeyOperationService;
import com.artivisi.hsm.simulator.service.MacService;
import com.artivisi.hsm.simulator.service.PinGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API Controller for HSM Workshop
 * Provides endpoints for PIN, MAC, and Key operations
 */
@RestController
@RequestMapping("/api/hsm")
@Slf4j
@RequiredArgsConstructor
public class HsmApiController {

    private final PinGenerationService pinGenerationService;
    private final MacService macService;
    private final KeyOperationService keyOperationService;
    private final MasterKeyRepository masterKeyRepository;

    /**
     * POST /api/hsm/pin/encrypt
     * Encrypt PIN block
     */
    @PostMapping("/pin/encrypt")
    public ResponseEntity<?> encryptPin(@RequestBody PinEncryptRequest request) {
        log.info("API: Encrypting PIN for account {}, format {}", request.getAccountNumber(), request.getFormat());

        try {
            UUID keyId = UUID.fromString(request.getKeyId());
            GeneratedPin generatedPin = pinGenerationService.generatePin(
                    keyId,
                    request.getAccountNumber(),
                    request.getPin().length(),
                    request.getFormat()
            );

            PinEncryptResponse response = PinEncryptResponse.builder()
                    .encryptedPinBlock(generatedPin.getEncryptedPinBlock())
                    .format(generatedPin.getPinFormat())
                    .pvv(generatedPin.getPinVerificationValue())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error encrypting PIN", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/pin/verify
     * Verify PIN block
     */
    @PostMapping("/pin/verify")
    public ResponseEntity<?> verifyPin(@RequestBody Map<String, String> request) {
        log.info("API: Verifying PIN for account {}", request.get("accountNumber"));

        try {
            String accountNumber = request.get("accountNumber");
            String pin = request.get("pin");

            boolean isValid = pinGenerationService.verifyPin(accountNumber, pin);

            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "message", isValid ? "PIN is valid" : "PIN is invalid"
            ));
        } catch (Exception e) {
            log.error("Error verifying PIN", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/mac/generate
     * Generate MAC for message
     */
    @PostMapping("/mac/generate")
    public ResponseEntity<?> generateMac(@RequestBody Map<String, String> request) {
        log.info("API: Generating MAC for message length {}", request.get("message").length());

        try {
            UUID keyId = UUID.fromString(request.get("keyId"));
            String message = request.get("message");
            String algorithm = request.getOrDefault("algorithm", "ISO9797-ALG3");

            var generatedMac = macService.generateMac(keyId, message, algorithm);

            return ResponseEntity.ok(Map.of(
                    "macValue", generatedMac.getMacValue(),
                    "algorithm", generatedMac.getMacAlgorithm(),
                    "messageLength", generatedMac.getMessageLength()
            ));
        } catch (Exception e) {
            log.error("Error generating MAC", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/mac/verify
     * Verify MAC for message
     */
    @PostMapping("/mac/verify")
    public ResponseEntity<?> verifyMac(@RequestBody Map<String, String> request) {
        log.info("API: Verifying MAC for message length {}", request.get("message").length());

        try {
            UUID keyId = UUID.fromString(request.get("keyId"));
            String message = request.get("message");
            String mac = request.get("mac");
            String algorithm = request.getOrDefault("algorithm", "ISO9797-ALG3");

            boolean isValid = macService.verifyMac(message, mac, keyId, algorithm);

            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "message", isValid ? "MAC is valid" : "MAC is invalid"
            ));
        } catch (Exception e) {
            log.error("Error verifying MAC", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/key/generate
     * Generate new cryptographic key
     */
    @PostMapping("/key/generate")
    public ResponseEntity<?> generateKey(@RequestBody Map<String, Object> request) {
        log.info("API: Generating key type {}", request.get("keyType"));

        try {
            String keyTypeStr = (String) request.get("keyType");
            Integer keySize = (Integer) request.getOrDefault("keySize", 256);
            String description = (String) request.getOrDefault("description", "Generated via API");

            // Map workshop key types to our key types
            // ZAK and TEK both map to ZSK as per requirement
            KeyType keyType = switch (keyTypeStr) {
                case "ZMK" -> KeyType.ZMK;
                case "ZPK" -> KeyType.ZPK;
                case "ZAK", "TEK" -> KeyType.ZSK; // Map ZAK and TEK to ZSK
                case "TMK" -> KeyType.TMK;
                case "TPK" -> KeyType.TPK;
                case "TSK" -> KeyType.TSK;
                default -> throw new IllegalArgumentException("Unsupported key type: " + keyTypeStr);
            };

            MasterKey generatedKey;

            // For ZMK and TMK, generate root keys
            if (keyType == KeyType.ZMK) {
                // Need bank ID for ZMK generation
                UUID bankId = request.containsKey("bankId")
                    ? UUID.fromString((String) request.get("bankId"))
                    : getFirstActiveBankId();
                generatedKey = keyOperationService.generateZMK(bankId, keySize, description);
            } else if (keyType == KeyType.TMK) {
                UUID bankId = request.containsKey("bankId")
                    ? UUID.fromString((String) request.get("bankId"))
                    : getFirstActiveBankId();
                generatedKey = keyOperationService.generateTMK(bankId, keySize, description);
            } else {
                throw new IllegalArgumentException("Direct generation only supported for ZMK and TMK. Other keys must be derived.");
            }

            return ResponseEntity.ok(Map.of(
                    "keyId", generatedKey.getId().toString(),
                    "masterKeyId", generatedKey.getMasterKeyId(),
                    "keyType", generatedKey.getKeyType().toString(),
                    "keyChecksum", generatedKey.getKeyChecksum(),
                    "keyFingerprint", generatedKey.getKeyFingerprint().substring(0, 16) // First 16 chars
            ));
        } catch (Exception e) {
            log.error("Error generating key", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/key/exchange
     * Exchange key from source encryption to target encryption
     */
    @PostMapping("/key/exchange")
    public ResponseEntity<?> exchangeKey(@RequestBody KeyExchangeRequest request) {
        log.info("API: Exchanging key type {} from {} to {}",
                request.getKeyType(), request.getSourceKeyId(), request.getTargetKeyId());

        try {
            UUID sourceKeyId = UUID.fromString(request.getSourceKeyId());
            UUID targetKeyId = UUID.fromString(request.getTargetKeyId());

            MasterKey sourceKey = masterKeyRepository.findById(sourceKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("Source key not found"));
            MasterKey targetKey = masterKeyRepository.findById(targetKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("Target key not found"));

            // In real HSM, this would decrypt with source key and re-encrypt with target key
            // For simulation, we'll use the target key to encrypt a session key
            byte[] sessionKey = generateSessionKey();
            String encryptedKey = encryptKeyUnderKey(sessionKey, targetKey);
            String keyCheckValue = calculateKeyCheckValue(sessionKey);

            KeyExchangeResponse response = KeyExchangeResponse.builder()
                    .encryptedKey(encryptedKey)
                    .keyCheckValue(keyCheckValue)
                    .keyType(request.getKeyType())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error exchanging key", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // Helper methods

    private UUID getFirstActiveBankId() {
        return masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(MasterKey::getId)
                .orElseThrow(() -> new IllegalStateException("No active banks found"));
    }

    private byte[] generateSessionKey() {
        byte[] key = new byte[16]; // 128-bit key
        new java.security.SecureRandom().nextBytes(key);
        return key;
    }

    private String encryptKeyUnderKey(byte[] keyToEncrypt, MasterKey encryptingKey) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding");
            byte[] keyBytes = new byte[16];
            System.arraycopy(encryptingKey.getKeyDataEncrypted(), 0, keyBytes, 0,
                    Math.min(16, encryptingKey.getKeyDataEncrypted().length));
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(keyToEncrypt);
            return bytesToHex(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt key", e);
        }
    }

    private String calculateKeyCheckValue(byte[] key) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key, "AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(new byte[16]); // Encrypt zeros
            return bytesToHex(encrypted).substring(0, 6); // First 6 hex chars
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate KCV", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
