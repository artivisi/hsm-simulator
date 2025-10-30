package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.config.CryptoConstants;
import com.artivisi.hsm.simulator.util.CryptoUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Test to generate real sample data for V2 migration.
 * This test generates actual encrypted PIN blocks and MAC values
 * that can be used in the database migration for testing.
 *
 * Run this test to get real hex values for V2__insert_sample_data.sql
 */
public class SampleDataGeneratorTest {

    private final SecureRandom secureRandom = new SecureRandom();

    @BeforeAll
    static void setUp() {
        // Register BouncyCastle provider for AES-CMAC support
        Security.addProvider(new BouncyCastleProvider());
    }

    // Sample keys from V2 migration (these are the actual keys in the database)
    private static final String LMK_HEX = "5DD14D36637632409C34F8F876F22BAF467D5633730ABA7273CF72027EDF90BA";
    private static final String TMK_HEX = "A1D565F24C52BBB31F3B2A975325A56B3820B10CC3DB8D07F402BF5E7E00FAAC";
    private static final String TPK_HEX = "246A31D729B280DD7FCDA3BB7F187ABFA1BB0811D7EF3D68FDCA63579F3748B0";
    private static final String TSK_HEX = "3AC638783EF600FE5E25E8A2EE5B0D222EB810DDF64C3681DD11AFEFAF41614B";

    // Test data
    private static final String TEST_PAN = "4111111111111111";
    private static final String TEST_PIN = "1234";
    private static final String TEST_MESSAGE = "0800822000000000000004000000000000000000001234567890123456";

    @Test
    public void generateSampleDataForV2Migration() throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("SAMPLE DATA GENERATION FOR V2 MIGRATION");
        System.out.println("Generating real encrypted PIN blocks and MAC values");
        System.out.println("=".repeat(80));
        System.out.println();

        // Generate PIN blocks
        generatePinBlocks();

        System.out.println();

        // Generate MAC
        generateMac();

        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("Copy the values above to V2__insert_sample_data.sql");
        System.out.println("=".repeat(80));
    }

    private void generatePinBlocks() throws Exception {
        System.out.println("-- GENERATING PIN BLOCKS");
        System.out.println("-- PAN: " + TEST_PAN);
        System.out.println("-- PIN: " + TEST_PIN);
        System.out.println();

        // Step 1: Create PIN block (ISO-0 format)
        String pinBlock = createISO0PinBlock(TEST_PIN, TEST_PAN);
        System.out.println("Clear PIN Block (ISO-0): " + pinBlock);
        System.out.println();

        // Step 2: Derive operational keys from master keys
        byte[] lmkKey = CryptoUtils.hexToBytes(LMK_HEX);
        byte[] tpkKey = CryptoUtils.hexToBytes(TPK_HEX);

        // Derive PIN operational keys (16 bytes for AES-128)
        // IMPORTANT: Use actual bank UUID from database, NOT "GLOBAL"
        // Bank ISS001 UUID: 48a9e84c-ff57-4483-bf83-b255f34a6466
        String bankUuid = "48a9e84c-ff57-4483-bf83-b255f34a6466";
        String lmkContext = "LMK:" + bankUuid + ":PIN";
        String tpkContext = "TPK:" + bankUuid + ":PIN";

        byte[] lmkPinKey = CryptoUtils.deriveKeyFromParent(lmkKey, lmkContext, 128);
        byte[] tpkPinKey = CryptoUtils.deriveKeyFromParent(tpkKey, tpkContext, 128);

        System.out.println("Derived LMK PIN Key (first 8 bytes): " + CryptoUtils.bytesToHex(lmkPinKey).substring(0, 16));
        System.out.println("Derived TPK PIN Key (first 8 bytes): " + CryptoUtils.bytesToHex(tpkPinKey).substring(0, 16));
        System.out.println();

        // Step 3: Encrypt PIN block under LMK
        String encryptedPinBlockLMK = encryptPinBlock(pinBlock, lmkPinKey);
        System.out.println("-- Encrypted PIN Block under LMK:");
        System.out.println("encrypted_pin_block: '" + encryptedPinBlockLMK + "'");
        System.out.println();

        // Step 4: Encrypt PIN block under TPK
        String encryptedPinBlockTPK = encryptPinBlock(pinBlock, tpkPinKey);
        System.out.println("-- Encrypted PIN Block under TPK:");
        System.out.println("encrypted_pin_block: '" + encryptedPinBlockTPK + "'");
        System.out.println();

        // Step 5: Generate PVV (PIN Verification Value)
        String pvv = generatePVV(TEST_PIN, TEST_PAN);
        System.out.println("-- PIN Verification Value (PVV):");
        System.out.println("pin_verification_value: '" + pvv + "'");
        System.out.println();

        // Verification: Decrypt and compare
        System.out.println("-- VERIFICATION:");
        String decryptedLMK = decryptPinBlock(encryptedPinBlockLMK, lmkPinKey);
        String decryptedTPK = decryptPinBlock(encryptedPinBlockTPK, tpkPinKey);
        System.out.println("Decrypted from LMK: " + decryptedLMK + " (matches: " + decryptedLMK.equals(pinBlock) + ")");
        System.out.println("Decrypted from TPK: " + decryptedTPK + " (matches: " + decryptedTPK.equals(pinBlock) + ")");
    }

    private void generateMac() throws Exception {
        System.out.println("-- GENERATING MAC");
        System.out.println("-- Message: " + TEST_MESSAGE);
        System.out.println("-- Algorithm: AES-CMAC");
        System.out.println();

        // Derive MAC operational key from TSK (16 bytes for AES-128)
        // IMPORTANT: Use actual bank UUID from database, NOT "GLOBAL"
        byte[] tskKey = CryptoUtils.hexToBytes(TSK_HEX);
        String bankUuid = "48a9e84c-ff57-4483-bf83-b255f34a6466";
        String tskContext = "TSK:" + bankUuid + ":MAC";
        byte[] tskMacKey = CryptoUtils.deriveKeyFromParent(tskKey, tskContext, 128);

        System.out.println("Derived TSK MAC Key (first 8 bytes): " + CryptoUtils.bytesToHex(tskMacKey).substring(0, 16));
        System.out.println();

        // Calculate AES-CMAC
        String macValue = calculateAesCmac(TEST_MESSAGE, tskMacKey);
        System.out.println("-- MAC Value (AES-CMAC, 16 hex chars = 64 bits):");
        System.out.println("mac_value: '" + macValue + "'");
    }

    /**
     * Create ISO-0 PIN block format
     * Format: 0L[PIN][F]... XOR [0000][12 rightmost PAN digits excluding check digit]
     */
    private String createISO0PinBlock(String pin, String pan) {
        // PIN field: 0 + length + PIN + padding with F
        String pinField = String.format("0%d%s", pin.length(), pin);
        while (pinField.length() < 16) {
            pinField += "F";
        }

        // PAN field: 0000 + 12 rightmost digits of PAN (excluding check digit)
        String panField = "0000" + pan.substring(pan.length() - 13, pan.length() - 1);

        // XOR the two fields
        return xorHex(pinField, panField);
    }

    /**
     * XOR two hex strings
     */
    private String xorHex(String hex1, String hex2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(hex1.length(), hex2.length()); i++) {
            int val1 = Character.digit(hex1.charAt(i), 16);
            int val2 = Character.digit(hex2.charAt(i), 16);
            result.append(Integer.toHexString(val1 ^ val2));
        }
        return result.toString().toUpperCase();
    }

    /**
     * Encrypt PIN block using AES-128-CBC
     * Returns: IV + Ciphertext (hex encoded)
     */
    private String encryptPinBlock(String pinBlock, byte[] key) throws Exception {
        // Generate random IV
        byte[] iv = new byte[CryptoConstants.CBC_IV_BYTES];
        secureRandom.nextBytes(iv);

        // Create cipher
        Cipher cipher = Cipher.getInstance(CryptoConstants.PIN_CIPHER);
        SecretKeySpec secretKey = new SecretKeySpec(key, CryptoConstants.MASTER_KEY_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        // Convert hex PIN block to bytes and encrypt
        byte[] pinBlockBytes = CryptoUtils.hexToBytes(pinBlock);
        byte[] encrypted = cipher.doFinal(pinBlockBytes);

        // Prepend IV to encrypted data (IV:ciphertext)
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

        return CryptoUtils.bytesToHex(result);
    }

    /**
     * Decrypt PIN block using AES-128-CBC
     * Input: IV + Ciphertext (hex encoded)
     */
    private String decryptPinBlock(String encryptedPinBlock, byte[] key) throws Exception {
        // Convert hex to bytes
        byte[] encryptedBytes = CryptoUtils.hexToBytes(encryptedPinBlock);

        // Extract IV and ciphertext
        byte[] iv = new byte[CryptoConstants.CBC_IV_BYTES];
        byte[] ciphertext = new byte[encryptedBytes.length - CryptoConstants.CBC_IV_BYTES];
        System.arraycopy(encryptedBytes, 0, iv, 0, CryptoConstants.CBC_IV_BYTES);
        System.arraycopy(encryptedBytes, CryptoConstants.CBC_IV_BYTES, ciphertext, 0, ciphertext.length);

        // Create cipher and decrypt
        Cipher cipher = Cipher.getInstance(CryptoConstants.PIN_CIPHER);
        SecretKeySpec secretKey = new SecretKeySpec(key, CryptoConstants.MASTER_KEY_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decrypted = cipher.doFinal(ciphertext);

        return CryptoUtils.bytesToHex(decrypted);
    }

    /**
     * Generate PVV (PIN Verification Value) using SHA-256
     */
    private String generatePVV(String pin, String pan) throws Exception {
        String input = pin + pan;
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
    }

    /**
     * Calculate AES-CMAC (truncated to 16 hex chars = 64 bits)
     */
    private String calculateAesCmac(String message, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("AESCMAC", "BC");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        mac.init(secretKey);

        byte[] messageBytes = message.getBytes();
        byte[] macBytes = mac.doFinal(messageBytes);

        // Return first 8 bytes (64 bits) as hex
        String fullMac = CryptoUtils.bytesToHex(macBytes);
        return fullMac.substring(0, 16);
    }
}
