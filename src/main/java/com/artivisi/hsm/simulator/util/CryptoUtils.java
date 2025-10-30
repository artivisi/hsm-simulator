package com.artivisi.hsm.simulator.util;

import com.artivisi.hsm.simulator.config.CryptoConstants;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Centralized cryptographic utility methods for key generation, conversion, and hashing.
 * All cryptographic helper functions should be consolidated here to avoid code duplication.
 */
@Slf4j
public final class CryptoUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtils() {
        // Utility class - prevent instantiation
    }

    // ===== KEY GENERATION =====

    /**
     * Generates a random AES key using SecureRandom.
     *
     * @param keySize Key size in bits (128, 192, or 256)
     * @return Random key bytes
     */
    public static byte[] generateRandomKey(int keySize) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(CryptoConstants.MASTER_KEY_ALGORITHM);
            keyGen.init(keySize, SECURE_RANDOM);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (Exception e) {
            log.error("Error generating random key with size {} bits", keySize, e);
            throw new RuntimeException("Failed to generate random key", e);
        }
    }

    /**
     * Derives a key from a parent key using PBKDF2-SHA256.
     * This method matches the production implementation in KeyGenerationService.
     *
     * @param parentKey Parent key bytes (typically 256-bit master key)
     * @param context Context string used as salt (e.g., "TPK:TRM-ISS001-ATM-001")
     * @param outputBits Desired output key size in bits
     * @return Derived key bytes
     */
    public static byte[] deriveKeyFromParent(byte[] parentKey, String context, int outputBits) {
        try {
            // Convert parent key to hex string for PBKDF2 password
            String password = bytesToHex(parentKey);

            // Use context as salt
            byte[] salt = context.getBytes(StandardCharsets.UTF_8);

            // PBKDF2 with same parameters as production
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                CryptoConstants.KDF_ITERATIONS,  // 100,000 iterations
                outputBits
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(CryptoConstants.KDF_ALGORITHM);
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();

            // Clear sensitive data
            spec.clearPassword();

            return derivedKey;

        } catch (Exception e) {
            log.error("Failed to derive key from parent with context: {}", context, e);
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    // ===== HASH AND FINGERPRINT GENERATION =====

    /**
     * Generates a SHA-256 fingerprint from key data.
     * Returns first 24 characters of the hash (12 bytes) for compact representation.
     *
     * @param keyData Key bytes
     * @return Fingerprint string (24 hex characters, uppercase)
     */
    public static String generateFingerprint(byte[] keyData) {
        try {
            MessageDigest digest = MessageDigest.getInstance(CryptoConstants.HASH_ALGORITHM);
            byte[] hash = digest.digest(keyData);
            return bytesToHex(hash).substring(0, 24);
        } catch (Exception e) {
            log.error("Error generating fingerprint", e);
            throw new RuntimeException("Failed to generate fingerprint", e);
        }
    }

    /**
     * Generates a SHA-256 checksum from key data.
     * Returns first 16 characters of the hash (8 bytes) for compact representation.
     *
     * @param keyData Key bytes
     * @return Checksum string (16 hex characters, uppercase)
     */
    public static String generateChecksum(byte[] keyData) {
        try {
            MessageDigest digest = MessageDigest.getInstance(CryptoConstants.HASH_ALGORITHM);
            byte[] hash = digest.digest(keyData);
            return bytesToHex(hash).substring(0, 16);
        } catch (Exception e) {
            log.error("Error generating checksum", e);
            throw new RuntimeException("Failed to generate checksum", e);
        }
    }

    /**
     * Generates full SHA-256 hash from data.
     *
     * @param data Input data
     * @return Full SHA-256 hash (64 hex characters, lowercase)
     */
    public static String generateFullHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(CryptoConstants.HASH_ALGORITHM);
            byte[] hash = digest.digest(data);
            return bytesToHexLowercase(hash);
        } catch (Exception e) {
            log.error("Error generating hash", e);
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    // ===== BYTE/HEX CONVERSION =====

    /**
     * Converts byte array to uppercase hexadecimal string.
     *
     * @param bytes Input bytes
     * @return Hex string (uppercase)
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    /**
     * Converts byte array to lowercase hexadecimal string.
     *
     * @param bytes Input bytes
     * @return Hex string (lowercase)
     */
    public static String bytesToHexLowercase(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Converts hexadecimal string to byte array.
     * Accepts both uppercase and lowercase hex strings.
     *
     * @param hex Hex string
     * @return Byte array
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // ===== SECURE RANDOM =====

    /**
     * Generates cryptographically secure random bytes.
     *
     * @param numBytes Number of bytes to generate
     * @return Random bytes
     */
    public static byte[] generateRandomBytes(int numBytes) {
        byte[] bytes = new byte[numBytes];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Generates a cryptographically secure salt for key derivation.
     *
     * @return Salt bytes (32 bytes by default)
     */
    public static byte[] generateSalt() {
        return generateRandomBytes(CryptoConstants.KDF_SALT_BYTES);
    }
}
