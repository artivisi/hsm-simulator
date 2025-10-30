package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.config.CryptoConstants;
import com.artivisi.hsm.simulator.dto.KeyExchangeRequest;
import com.artivisi.hsm.simulator.dto.KeyExchangeResponse;
import com.artivisi.hsm.simulator.dto.PinEncryptRequest;
import com.artivisi.hsm.simulator.dto.PinEncryptResponse;
import com.artivisi.hsm.simulator.entity.GeneratedPin;
import com.artivisi.hsm.simulator.entity.KeyType;
import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import com.artivisi.hsm.simulator.service.KeyGenerationService;
import com.artivisi.hsm.simulator.service.KeyInitializationService;
import com.artivisi.hsm.simulator.service.KeyOperationService;
import com.artivisi.hsm.simulator.service.MacService;
import com.artivisi.hsm.simulator.service.PinGenerationService;
import com.artivisi.hsm.simulator.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
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
    private final KeyInitializationService keyInitializationService;
    private final KeyGenerationService keyGenerationService;
    private final MasterKeyRepository masterKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * POST /api/hsm/pin/encrypt
     * Encrypt PIN block with provided cleartext PIN
     * Returns encrypted PIN block and PVV for storage
     */
    @PostMapping("/pin/encrypt")
    public ResponseEntity<?> encryptPin(@RequestBody PinEncryptRequest request) {
        log.info("API: Encrypting PIN for account {}, format {}", request.getAccountNumber(), request.getFormat());

        try {
            // Validate inputs
            if (request.getPin() == null || request.getPin().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PIN is required"
                ));
            }

            if (request.getPin().length() < 4 || request.getPin().length() > 12) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PIN length must be between 4 and 12 digits"
                ));
            }

            if (!request.getPin().matches("\\d+")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PIN must contain only digits"
                ));
            }

            UUID keyId = UUID.fromString(request.getKeyId());

            // Use the provided cleartext PIN (not random generation)
            GeneratedPin generatedPin = pinGenerationService.generatePin(
                    keyId,
                    request.getAccountNumber(),
                    request.getPin(),  // Use actual PIN from request
                    request.getFormat()
            );

            PinEncryptResponse response = PinEncryptResponse.builder()
                    .encryptedPinBlock(generatedPin.getEncryptedPinBlock())
                    .format(generatedPin.getPinFormat())
                    .pvv(generatedPin.getPinVerificationValue())
                    .build();

            log.info("PIN encrypted successfully for account {}, PVV: {}",
                     request.getAccountNumber(), generatedPin.getPinVerificationValue());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error encrypting PIN", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/pin/generate-pinblock
     * Generate PIN block encrypted with LMK
     * Input: PAN, PIN format, plaintext PIN
     * Output: Encrypted PIN block, format, PVV
     */
    @PostMapping("/pin/generate-pinblock")
    public ResponseEntity<?> generatePinBlock(@RequestBody Map<String, String> request) {
        String pan = request.get("pan");
        String pin = request.get("pin");
        String format = request.getOrDefault("format", "ISO-0");

        log.info("API: Generating PIN block for PAN {}, format {}", pan, format);

        try {
            if (pan == null || pan.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PAN is required"
                ));
            }

            if (pin == null || pin.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PIN is required"
                ));
            }

            if (pin.length() < 4 || pin.length() > 12) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PIN length must be between 4 and 12 digits"
                ));
            }

            if (!pin.matches("\\d+")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PIN must contain only digits"
                ));
            }

            // Find active LMK key
            MasterKey lmkKey = masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                    .stream()
                    .filter(k -> k.getKeyType() == KeyType.LMK)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active LMK key found"));

            // Generate PIN block with plaintext PIN
            GeneratedPin generatedPin = pinGenerationService.generatePin(
                    lmkKey.getId(),
                    pan,
                    pin,
                    format
            );

            return ResponseEntity.ok(Map.of(
                    "encryptedPinBlock", generatedPin.getEncryptedPinBlock(),
                    "format", generatedPin.getPinFormat(),
                    "pvv", generatedPin.getPinVerificationValue(),
                    "keyId", lmkKey.getMasterKeyId(),
                    "keyType", "LMK"
            ));
        } catch (Exception e) {
            log.error("Error generating PIN block", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/pin/verify-with-translation
     * Verify PIN with TPK to LMK translation
     * Input: pinBlockUnderLMK, pinBlockUnderTPK, terminalId, pan, pinFormat
     * This simulates HSM operation where terminal sends PIN block encrypted with TPK,
     * and HSM translates it to LMK for verification against stored PIN
     */
    @PostMapping("/pin/verify-with-translation")
    public ResponseEntity<?> verifyPinWithTranslation(@RequestBody Map<String, String> request) {
        String pinBlockUnderLMK = request.get("pinBlockUnderLMK");
        String pinBlockUnderTPK = request.get("pinBlockUnderTPK");
        String terminalId = request.get("terminalId");
        String pan = request.get("pan");
        String pinFormat = request.getOrDefault("pinFormat", "ISO-0");

        log.info("========================================");
        log.info("PIN VERIFICATION REQUEST - Method A (Translation)");
        log.info("========================================");
        log.info("Endpoint: POST /api/hsm/pin/verify-with-translation");
        log.info("Terminal ID: {}", terminalId);
        log.info("PAN: {}", maskPan(pan));
        log.info("PIN Format: {}", pinFormat);
        log.info("PIN Block (TPK): {} chars", pinBlockUnderTPK != null ? pinBlockUnderTPK.length() : 0);
        log.info("PIN Block (LMK): {} chars", pinBlockUnderLMK != null ? pinBlockUnderLMK.length() : 0);

        try {
            // Validate inputs
            if (pinBlockUnderLMK == null || pinBlockUnderLMK.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "pinBlockUnderLMK is required"
                ));
            }

            if (pinBlockUnderTPK == null || pinBlockUnderTPK.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "pinBlockUnderTPK is required"
                ));
            }

            if (terminalId == null || terminalId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "terminalId is required"
                ));
            }

            if (pan == null || pan.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "pan is required"
                ));
            }

            // Find active LMK key
            MasterKey lmkKey = masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                    .stream()
                    .filter(k -> k.getKeyType() == KeyType.LMK)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active LMK key found"));

            // Find active TPK key for the terminal's bank
            // For simplicity, we'll use the first active TPK key
            MasterKey tpkKey = masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                    .stream()
                    .filter(k -> k.getKeyType() == KeyType.TPK)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active TPK key found"));

            // Verify PIN with translation
            boolean isValid = pinGenerationService.verifyPinWithTranslation(
                    pinBlockUnderTPK,
                    pinBlockUnderLMK,
                    pan,
                    pinFormat,
                    tpkKey.getId(),
                    lmkKey.getId()
            );

            log.info("========================================");
            log.info("PIN VERIFICATION RESPONSE - Method A");
            log.info("========================================");
            log.info("Result: {}", isValid ? "SUCCESS" : "FAILED");
            log.info("Terminal: {}", terminalId);
            log.info("PAN: {}", maskPan(pan));
            log.info("TPK Key: {}", tpkKey.getMasterKeyId());
            log.info("LMK Key: {}", lmkKey.getMasterKeyId());
            log.info("========================================");

            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "message", isValid ? "PIN verified successfully" : "PIN verification failed",
                    "terminalId", terminalId,
                    "pan", maskPan(pan),
                    "pinFormat", pinFormat,
                    "lmkKeyId", lmkKey.getMasterKeyId(),
                    "tpkKeyId", tpkKey.getMasterKeyId()
            ));
        } catch (Exception e) {
            log.error("========================================");
            log.error("PIN VERIFICATION ERROR - Method A");
            log.error("Terminal: {}", terminalId);
            log.error("PAN: {}", maskPan(pan));
            log.error("Error: {}", e.getMessage());
            log.error("========================================", e);

            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/pin/verify-with-pvv
     * Verify PIN using PVV (PIN Verification Value) method
     * Input: pinBlockUnderTPK, storedPVV, terminalId, pan, pinFormat
     *
     * This is the most common method in banking (ISO 9564)
     * Flow:
     * 1. Terminal → Core Bank: PIN block (TPK) + PAN
     * 2. Core Bank → Database: Query stored PVV for PAN
     * 3. Core Bank → HSM: PIN block (TPK) + stored PVV + PAN
     * 4. HSM: Decrypt PIN block, calculate PVV, compare with stored PVV
     */
    @PostMapping("/pin/verify-with-pvv")
    public ResponseEntity<?> verifyPinWithPVV(@RequestBody Map<String, String> request) {
        String pinBlockUnderTPK = request.get("pinBlockUnderTPK");
        String storedPVV = request.get("storedPVV");
        String terminalId = request.get("terminalId");
        String pan = request.get("pan");
        String pinFormat = request.getOrDefault("pinFormat", "ISO-0");

        log.info("========================================");
        log.info("PIN VERIFICATION REQUEST - Method B (PVV)");
        log.info("========================================");
        log.info("Endpoint: POST /api/hsm/pin/verify-with-pvv");
        log.info("Terminal ID: {}", terminalId);
        log.info("PAN: {}", maskPan(pan));
        log.info("PIN Format: {}", pinFormat);
        log.info("PIN Block (TPK): {} chars", pinBlockUnderTPK != null ? pinBlockUnderTPK.length() : 0);
        log.info("Stored PVV: {}", storedPVV);

        try {
            // Validate inputs
            if (pinBlockUnderTPK == null || pinBlockUnderTPK.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "pinBlockUnderTPK is required"
                ));
            }

            if (storedPVV == null || storedPVV.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "storedPVV is required"
                ));
            }

            if (terminalId == null || terminalId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "terminalId is required"
                ));
            }

            if (pan == null || pan.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "pan is required"
                ));
            }

            // Find active TPK key
            MasterKey tpkKey = masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                    .stream()
                    .filter(k -> k.getKeyType() == KeyType.TPK)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active TPK key found"));

            // Find active LMK key (used as PVK for PVV calculation)
            MasterKey pvkKey = masterKeyRepository.findByStatus(MasterKey.KeyStatus.ACTIVE)
                    .stream()
                    .filter(k -> k.getKeyType() == KeyType.LMK)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active LMK key found"));

            // Verify PIN with PVV
            boolean isValid = pinGenerationService.verifyPinWithPVV(
                    pinBlockUnderTPK,
                    storedPVV,
                    pan,
                    pinFormat,
                    tpkKey.getId(),
                    pvkKey.getId()
            );

            log.info("========================================");
            log.info("PIN VERIFICATION RESPONSE - Method B (PVV)");
            log.info("========================================");
            log.info("Result: {}", isValid ? "SUCCESS" : "FAILED");
            log.info("Method: PVV (PIN Verification Value) - ISO 9564");
            log.info("Terminal: {}", terminalId);
            log.info("PAN: {}", maskPan(pan));
            log.info("TPK Key: {}", tpkKey.getMasterKeyId());
            log.info("PVK Key: {}", pvkKey.getMasterKeyId());
            log.info("Stored PVV: {}", storedPVV);
            log.info("========================================");

            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "message", isValid ? "PIN verified successfully using PVV" : "PIN verification failed - PVV mismatch",
                    "method", "PVV (PIN Verification Value)",
                    "terminalId", terminalId,
                    "pan", maskPan(pan),
                    "pinFormat", pinFormat,
                    "tpkKeyId", tpkKey.getMasterKeyId(),
                    "pvkKeyId", pvkKey.getMasterKeyId(),
                    "storedPVV", storedPVV
            ));
        } catch (Exception e) {
            log.error("========================================");
            log.error("PIN VERIFICATION ERROR - Method B (PVV)");
            log.error("Terminal: {}", terminalId);
            log.error("PAN: {}", maskPan(pan));
            log.error("Error: {}", e.getMessage());
            log.error("========================================", e);

            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/pin/translate/tpk-to-zpk
     * Translate PIN block from TPK to ZPK (Acquirer side: Terminal to Zone)
     *
     * Used by acquirer when forwarding cardholder PIN to issuer for inter-bank transaction.
     * The acquirer receives PIN encrypted under TPK from the terminal, then re-encrypts
     * under ZPK for transmission to the issuer.
     *
     * Flow:
     * 1. Terminal encrypts PIN with TPK → sends to Acquirer
     * 2. Acquirer calls this endpoint → translates TPK to ZPK
     * 3. Acquirer sends ZPK-encrypted PIN to Issuer
     *
     * Request body:
     * {
     *   "pinBlockUnderTPK": "encrypted_hex_string",
     *   "pan": "4111111111111111",
     *   "pinFormat": "ISO-0",
     *   "tpkKeyId": "uuid-of-tpk-key",
     *   "zpkKeyId": "uuid-of-zpk-key"
     * }
     */
    @PostMapping("/pin/translate/tpk-to-zpk")
    public ResponseEntity<?> translateTpkToZpk(@RequestBody Map<String, String> request) {
        String pinBlockUnderTPK = request.get("pinBlockUnderTPK");
        String pan = request.get("pan");
        String pinFormat = request.get("pinFormat");
        String tpkKeyId = request.get("tpkKeyId");
        String zpkKeyId = request.get("zpkKeyId");

        log.info("API: Translating PIN block TPK → ZPK for PAN {}, format {}", pan, pinFormat);

        try {
            // Validate input
            if (pinBlockUnderTPK == null || pan == null || pinFormat == null || tpkKeyId == null || zpkKeyId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Missing required parameters: pinBlockUnderTPK, pan, pinFormat, tpkKeyId, zpkKeyId"
                ));
            }

            // Translate PIN block
            String pinBlockUnderZPK = pinGenerationService.translateTpkToZpk(
                    pinBlockUnderTPK,
                    pan,
                    pinFormat,
                    UUID.fromString(tpkKeyId),
                    UUID.fromString(zpkKeyId)
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "PIN block translated from TPK to ZPK successfully",
                    "pinBlockUnderZPK", pinBlockUnderZPK,
                    "pan", pan,
                    "pinFormat", pinFormat,
                    "tpkKeyId", tpkKeyId,
                    "zpkKeyId", zpkKeyId
            ));
        } catch (Exception e) {
            log.error("Error translating PIN block TPK → ZPK", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/pin/translate/zpk-to-lmk
     * Translate PIN block from ZPK to LMK (Issuer side: Zone to Storage)
     *
     * Used by issuer when receiving PIN from acquirer for verification.
     * The issuer receives PIN encrypted under ZPK, then re-encrypts under LMK
     * for verification against stored PIN.
     *
     * Flow:
     * 1. Acquirer sends ZPK-encrypted PIN to Issuer
     * 2. Issuer calls this endpoint → translates ZPK to LMK
     * 3. Issuer verifies LMK-encrypted PIN against database
     *
     * Request body:
     * {
     *   "pinBlockUnderZPK": "encrypted_hex_string",
     *   "pan": "4111111111111111",
     *   "pinFormat": "ISO-0",
     *   "zpkKeyId": "uuid-of-zpk-key",
     *   "lmkKeyId": "uuid-of-lmk-key"
     * }
     */
    @PostMapping("/pin/translate/zpk-to-lmk")
    public ResponseEntity<?> translateZpkToLmk(@RequestBody Map<String, String> request) {
        String pinBlockUnderZPK = request.get("pinBlockUnderZPK");
        String pan = request.get("pan");
        String pinFormat = request.get("pinFormat");
        String zpkKeyId = request.get("zpkKeyId");
        String lmkKeyId = request.get("lmkKeyId");

        log.info("API: Translating PIN block ZPK → LMK for PAN {}, format {}", pan, pinFormat);

        try {
            // Validate input
            if (pinBlockUnderZPK == null || pan == null || pinFormat == null || zpkKeyId == null || lmkKeyId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Missing required parameters: pinBlockUnderZPK, pan, pinFormat, zpkKeyId, lmkKeyId"
                ));
            }

            // Translate PIN block
            String pinBlockUnderLMK = pinGenerationService.translateZpkToLmk(
                    pinBlockUnderZPK,
                    pan,
                    pinFormat,
                    UUID.fromString(zpkKeyId),
                    UUID.fromString(lmkKeyId)
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "PIN block translated from ZPK to LMK successfully",
                    "pinBlockUnderLMK", pinBlockUnderLMK,
                    "pan", pan,
                    "pinFormat", pinFormat,
                    "zpkKeyId", zpkKeyId,
                    "lmkKeyId", lmkKeyId
            ));
        } catch (Exception e) {
            log.error("Error translating PIN block ZPK → LMK", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/hsm/keys/initialize
     * Initialize complete key set for a specific bank or all banks
     *
     * This endpoint creates a comprehensive key hierarchy including:
     * - LMK (Local Master Key) for PIN storage
     * - TMK (Terminal Master Keys) for specified bank(s)
     * - TPK (Terminal PIN Keys) for each terminal
     * - TSK (Terminal Security Keys) for each terminal
     * - ZMK (Zone Master Keys) for specified bank(s)
     * - ZPK (Zone PIN Keys) for inter-bank PIN operations
     * - ZSK (Zone Session Keys) for inter-bank messaging
     *
     * Two Setup Modes:
     *
     * 1. Shared HSM (Recommended for workshops - single instance):
     *    - Initialize issuer: {"bankCode": "ISS001"}
     *    - Initialize acquirer: {"bankCode": "ACQ001", "shareZoneKeysWith": "ISS001"}
     *    - Both banks share zone keys automatically (no manual copy needed)
     *    - Each bank has its own LMK, TMK, TPK, TSK
     *
     * 2. Multi-HSM (Production simulation - separate instances):
     *    - Run separate HSM instances (different ports/databases)
     *    - Initialize each with bankCode only
     *    - Manually copy zone keys between databases
     *
     * Request body (optional):
     * {
     *   "bankCode": "ISS001",            // Optional: specific bank code
     *   "shareZoneKeysWith": "ACQ001",   // Optional: bank code to copy zone keys from
     *   "clearExisting": true,            // Default: true
     *   "keySize": 256                   // Default: 256 bits
     * }
     */
    @PostMapping("/keys/initialize")
    public ResponseEntity<?> initializeKeys(@RequestBody(required = false) Map<String, Object> request) {
        boolean clearExisting = true; // Default: clear existing sample keys
        Integer keySize = 256; // Default key size
        String bankCode = null; // Default: all banks
        String shareZoneKeysWith = null; // Default: create new zone keys

        if (request != null) {
            if (request.containsKey("clearExisting")) {
                clearExisting = (Boolean) request.get("clearExisting");
            }
            if (request.containsKey("keySize")) {
                keySize = (Integer) request.get("keySize");
            }
            if (request.containsKey("bankCode")) {
                bankCode = (String) request.get("bankCode");
            }
            if (request.containsKey("shareZoneKeysWith")) {
                shareZoneKeysWith = (String) request.get("shareZoneKeysWith");
            }
        }

        log.info("API: Initializing complete key set (bankCode: {}, shareZoneKeysWith: {}, clearExisting: {}, keySize: {})",
                 bankCode == null ? "ALL" : bankCode,
                 shareZoneKeysWith == null ? "NONE" : shareZoneKeysWith,
                 clearExisting, keySize);

        try {
            Map<String, Object> result = keyInitializationService.initializeAllKeys(
                clearExisting, keySize, bankCode, shareZoneKeysWith);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error initializing keys", e);
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
            String algorithm = request.getOrDefault("algorithm", "AES-CMAC");

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
            String algorithm = request.getOrDefault("algorithm", "AES-CMAC");

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
            // Derive KEK (Key Encryption Key) using context
            String context = keyGenerationService.buildKeyContext(
                encryptingKey.getKeyType().toString(),
                encryptingKey.getIdBank() != null ? encryptingKey.getIdBank().toString() : "GLOBAL",
                "KEK"
            );

            byte[] kekBytes = keyGenerationService.deriveOperationalKey(
                encryptingKey.getKeyData(),
                context,
                CryptoConstants.ZONE_KEY_BYTES  // 32 bytes for AES-256
            );

            // Use AES-256 GCM mode for secure key wrapping
            Cipher cipher = Cipher.getInstance(CryptoConstants.KEK_CIPHER);
            SecretKeySpec secretKey = new SecretKeySpec(kekBytes, CryptoConstants.MASTER_KEY_ALGORITHM);

            // Generate random IV for GCM
            byte[] iv = new byte[CryptoConstants.GCM_IV_BYTES];
            secureRandom.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(CryptoConstants.GCM_TAG_BITS, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt the key
            byte[] encrypted = cipher.doFinal(keyToEncrypt);

            // Prepend IV to encrypted data (IV:ciphertext)
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return CryptoUtils.bytesToHex(result);
        } catch (Exception e) {
            log.error("Failed to encrypt key under key", e);
            throw new RuntimeException("Failed to encrypt key", e);
        }
    }

    private String calculateKeyCheckValue(byte[] key) {
        try {
            // KCV uses ECB mode with zeros - this is standard practice
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec secretKey = new SecretKeySpec(key, CryptoConstants.MASTER_KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(new byte[16]); // Encrypt zeros
            return CryptoUtils.bytesToHex(encrypted).substring(0, CryptoConstants.KCV_HEX_LENGTH);
        } catch (Exception e) {
            log.error("Failed to calculate KCV", e);
            throw new RuntimeException("Failed to calculate KCV", e);
        }
    }

    /**
     * Mask PAN for logging - shows first 6 and last 4 digits
     */
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return "******";
        }
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}
