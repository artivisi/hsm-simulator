package com.artivisi.hsm.simulator.config;

/**
 * Centralized cryptographic configuration for the HSM simulator.
 * All cryptographic operations MUST use these constants to ensure consistency.
 *
 * Standard: AES-256 for storage, derived keys for operations, no truncation.
 */
public final class CryptoConstants {

    private CryptoConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ===== Master Key Configuration =====

    /** Master key algorithm: AES-256 */
    public static final String MASTER_KEY_ALGORITHM = "AES";

    /** Master key size in bits */
    public static final int MASTER_KEY_BITS = 256;

    /** Master key size in bytes */
    public static final int MASTER_KEY_BYTES = MASTER_KEY_BITS / 8; // 32


    // ===== Key Derivation Configuration =====

    /** Key derivation function algorithm */
    public static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";

    /** PBKDF2 iteration count (100,000 iterations) */
    public static final int KDF_ITERATIONS = 100_000;

    /** PBKDF2 salt size in bytes (256 bits) */
    public static final int KDF_SALT_BYTES = 32;


    // ===== Operational Key Sizes =====

    /** PIN key size in bytes (AES-128 for ISO 9564 compatibility) */
    public static final int PIN_KEY_BYTES = 16;

    /** MAC key size in bytes (AES-256 for modern security) */
    public static final int MAC_KEY_BYTES = 32;

    /** MAC key size for legacy compatibility (AES-128) */
    public static final int MAC_KEY_BYTES_COMPAT = 16;

    /** Zone key size in bytes (AES-256) */
    public static final int ZONE_KEY_BYTES = 32;


    // ===== Cipher Modes and Algorithms =====

    /** Share encryption cipher (for Shamir shares) */
    public static final String SHARE_CIPHER = "AES/GCM/NoPadding";

    /** PIN block encryption cipher (CBC for security, PKCS5 for padding) */
    public static final String PIN_CIPHER = "AES/CBC/PKCS5Padding";

    /** Key-under-key encryption cipher */
    public static final String KEK_CIPHER = "AES/GCM/NoPadding";

    /** Zone key encryption cipher (inter-bank) */
    public static final String ZONE_CIPHER = "AES/GCM/NoPadding";


    // ===== GCM Parameters =====

    /** GCM initialization vector length in bytes */
    public static final int GCM_IV_BYTES = 12;

    /** GCM authentication tag length in bits */
    public static final int GCM_TAG_BITS = 128;


    // ===== CBC Parameters =====

    /** CBC initialization vector length in bytes (AES block size) */
    public static final int CBC_IV_BYTES = 16;


    // ===== MAC Configuration =====

    /** Modern MAC algorithm (recommended) */
    public static final String MAC_ALGORITHM_CMAC = "AESCMAC";

    /** Alternative modern MAC algorithm */
    public static final String MAC_ALGORITHM_HMAC = "HmacSHA256";

    /** MAC output size in bytes (full security) */
    public static final int MAC_OUTPUT_BYTES = 16;

    /** MAC output size for banking compatibility (64-bit) */
    public static final int MAC_OUTPUT_BYTES_COMPAT = 8;


    // ===== Hash Functions =====

    /** Standard hash algorithm for all fingerprints and checksums */
    public static final String HASH_ALGORITHM = "SHA-256";

    /** Fingerprint output length in hex characters (12 bytes) */
    public static final int FINGERPRINT_HEX_LENGTH = 24;

    /** Checksum output length in hex characters (8 bytes) */
    public static final int CHECKSUM_HEX_LENGTH = 16;


    // ===== Key Check Value (KCV) =====

    /** KCV output length in hex characters (3 bytes) */
    public static final int KCV_HEX_LENGTH = 6;


    // ===== PIN Configuration =====

    /** Minimum PIN length */
    public static final int PIN_LENGTH_MIN = 4;

    /** Maximum PIN length */
    public static final int PIN_LENGTH_MAX = 12;

    /** Default PIN length */
    public static final int PIN_LENGTH_DEFAULT = 6;


    // ===== Key Hierarchy Context Separators =====

    /** Context separator for key derivation */
    public static final String CONTEXT_SEPARATOR = ":";


    // ===== Validation Methods =====

    /**
     * Validates that a key size is acceptable for AES
     */
    public static boolean isValidKeySize(int bytes) {
        return bytes == 16 || bytes == 24 || bytes == 32; // AES-128, AES-192, AES-256
    }

    /**
     * Validates that a PIN length is acceptable
     */
    public static boolean isValidPinLength(int length) {
        return length >= PIN_LENGTH_MIN && length <= PIN_LENGTH_MAX;
    }

    /**
     * Validates that iteration count is acceptable for PBKDF2
     */
    public static boolean isValidIterationCount(int iterations) {
        return iterations >= 10_000; // Minimum recommended by NIST
    }
}
