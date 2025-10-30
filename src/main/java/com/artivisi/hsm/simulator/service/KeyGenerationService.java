package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.config.CryptoConstants;
import com.artivisi.hsm.simulator.util.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for cryptographic key generation operations including master key derivation,
 * Shamir Secret Sharing, and share encryption.
 */
@Service
@Slf4j
public class KeyGenerationService {

    private final SecureRandom secureRandom;

    public KeyGenerationService() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Derives a master key from multiple passphrase hashes using PBKDF2.
     * Combines all passphrase hashes as entropy sources.
     */
    public MasterKeyResult deriveMasterKey(List<String> passphraseHashes, byte[] salt, int iterations) {
        try {
            log.info("Deriving master key from {} passphrase contributions", passphraseHashes.size());

            // Combine all passphrase hashes into a single entropy source
            String combinedEntropy = String.join(":", passphraseHashes);

            // Derive key using PBKDF2
            PBEKeySpec spec = new PBEKeySpec(
                    combinedEntropy.toCharArray(),
                    salt,
                    iterations,
                    CryptoConstants.MASTER_KEY_BITS
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(CryptoConstants.KDF_ALGORITHM);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();

            // Clear the spec
            spec.clearPassword();

            // Calculate key fingerprint (SHA-256 hash of key)
            MessageDigest digest = MessageDigest.getInstance(CryptoConstants.HASH_ALGORITHM);
            byte[] fingerprintBytes = digest.digest(keyBytes);
            String fingerprint = CryptoUtils.bytesToHexLowercase(fingerprintBytes);

            // Calculate key checksum (first 8 bytes of SHA-256)
            String checksum = CryptoUtils.bytesToHexLowercase(Arrays.copyOf(fingerprintBytes, 8));

            // Calculate combined entropy hash
            byte[] entropyHashBytes = digest.digest(combinedEntropy.getBytes());
            String entropyHash = CryptoUtils.bytesToHexLowercase(entropyHashBytes);

            log.info("Master key derived successfully. Fingerprint: {}", fingerprint);

            return MasterKeyResult.builder()
                    .keyData(keyBytes)
                    .fingerprint(fingerprint)
                    .checksum(checksum)
                    .combinedEntropyHash(entropyHash)
                    .build();

        } catch (Exception e) {
            log.error("Error deriving master key", e);
            throw new RuntimeException("Failed to derive master key", e);
        }
    }

    /**
     * Generates a random AES-256 master key (alternative to PBKDF2 derivation).
     */
    public byte[] generateRandomMasterKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(CryptoConstants.MASTER_KEY_ALGORITHM);
            keyGen.init(CryptoConstants.MASTER_KEY_BITS, secureRandom);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (Exception e) {
            log.error("Error generating random master key", e);
            throw new RuntimeException("Failed to generate random master key", e);
        }
    }

    /**
     * Generates a cryptographically secure salt for PBKDF2.
     */
    public byte[] generateSalt() {
        return CryptoUtils.generateSalt();
    }

    /**
     * Creates Shamir Secret Shares from a secret (master key).
     *
     * @param secret The secret to split (master key bytes)
     * @param numShares Total number of shares to generate (n)
     * @param threshold Minimum shares required to reconstruct (k)
     * @return List of shares
     */
    public List<ShamirShare> createShamirShares(byte[] secret, int numShares, int threshold) {
        log.info("Creating Shamir shares: {} total, {} threshold", numShares, threshold);

        if (threshold > numShares) {
            throw new IllegalArgumentException("Threshold cannot exceed number of shares");
        }

        if (threshold < 2) {
            throw new IllegalArgumentException("Threshold must be at least 2");
        }

        try {
            // Use a large prime number for the finite field
            // For 256-bit keys, we need a prime larger than 2^256
            BigInteger prime = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
            // This is the prime used in secp256k1 (Bitcoin's curve)

            // Convert secret bytes to BigInteger
            BigInteger secretInt = new BigInteger(1, secret);

            if (secretInt.compareTo(prime) >= 0) {
                throw new IllegalArgumentException("Secret must be less than prime modulus");
            }

            // Generate random coefficients for polynomial of degree (threshold - 1)
            List<BigInteger> coefficients = new ArrayList<>();
            coefficients.add(secretInt); // a0 = secret

            for (int i = 1; i < threshold; i++) {
                BigInteger coefficient = new BigInteger(prime.bitLength(), secureRandom).mod(prime);
                coefficients.add(coefficient);
            }

            // Generate shares by evaluating polynomial at x = 1, 2, 3, ..., numShares
            List<ShamirShare> shares = new ArrayList<>();

            for (int x = 1; x <= numShares; x++) {
                BigInteger xValue = BigInteger.valueOf(x);
                BigInteger yValue = evaluatePolynomial(coefficients, xValue, prime);

                shares.add(ShamirShare.builder()
                        .shareIndex(x)
                        .xValue(xValue)
                        .yValue(yValue)
                        .prime(prime)
                        .threshold(threshold)
                        .build());
            }

            log.info("Successfully created {} Shamir shares", shares.size());
            return shares;

        } catch (Exception e) {
            log.error("Error creating Shamir shares", e);
            throw new RuntimeException("Failed to create Shamir shares", e);
        }
    }

    /**
     * Reconstructs the secret from Shamir shares using Lagrange interpolation.
     *
     * @param shares List of shares (at least k shares required)
     * @return The reconstructed secret
     */
    public byte[] reconstructSecret(List<ShamirShare> shares) {
        if (shares.isEmpty()) {
            throw new IllegalArgumentException("At least one share is required");
        }

        int threshold = shares.get(0).getThreshold();
        if (shares.size() < threshold) {
            throw new IllegalArgumentException(
                    String.format("Insufficient shares: need %d, got %d", threshold, shares.size()));
        }

        BigInteger prime = shares.get(0).getPrime();

        try {
            // Use Lagrange interpolation to find f(0) = secret
            BigInteger secret = lagrangeInterpolation(shares, prime);

            // Convert BigInteger back to bytes
            byte[] secretBytes = secret.toByteArray();

            // Remove leading zero byte if present (BigInteger adds it for positive numbers)
            if (secretBytes.length > CryptoConstants.MASTER_KEY_BYTES && secretBytes[0] == 0) {
                secretBytes = Arrays.copyOfRange(secretBytes, 1, secretBytes.length);
            }

            // Pad with leading zeros if necessary
            if (secretBytes.length < CryptoConstants.MASTER_KEY_BYTES) {
                byte[] paddedBytes = new byte[CryptoConstants.MASTER_KEY_BYTES];
                System.arraycopy(secretBytes, 0, paddedBytes, CryptoConstants.MASTER_KEY_BYTES - secretBytes.length, secretBytes.length);
                secretBytes = paddedBytes;
            }

            log.info("Successfully reconstructed secret from {} shares", shares.size());
            return secretBytes;

        } catch (Exception e) {
            log.error("Error reconstructing secret", e);
            throw new RuntimeException("Failed to reconstruct secret", e);
        }
    }

    /**
     * Encrypts a Shamir share using AES-256-GCM.
     */
    public byte[] encryptShare(ShamirShare share, byte[] encryptionKey) {
        try {
            // Serialize share to bytes
            byte[] shareBytes = serializeShare(share);

            // Generate random IV
            byte[] iv = new byte[CryptoConstants.GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(CryptoConstants.SHARE_CIPHER);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, CryptoConstants.MASTER_KEY_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(CryptoConstants.GCM_TAG_BITS, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Encrypt
            byte[] encrypted = cipher.doFinal(shareBytes);

            // Combine IV + encrypted data
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return result;

        } catch (Exception e) {
            log.error("Error encrypting share", e);
            throw new RuntimeException("Failed to encrypt share", e);
        }
    }

    /**
     * Decrypts an encrypted Shamir share.
     */
    public ShamirShare decryptShare(byte[] encryptedData, byte[] encryptionKey) {
        try {
            // Extract IV and encrypted data
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, CryptoConstants.GCM_IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(encryptedData, CryptoConstants.GCM_IV_BYTES, encryptedData.length);

            // Create cipher
            Cipher cipher = Cipher.getInstance(CryptoConstants.SHARE_CIPHER);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, CryptoConstants.MASTER_KEY_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(CryptoConstants.GCM_TAG_BITS, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Decrypt
            byte[] decrypted = cipher.doFinal(encrypted);

            // Deserialize share
            return deserializeShare(decrypted);

        } catch (Exception e) {
            log.error("Error decrypting share", e);
            throw new RuntimeException("Failed to decrypt share", e);
        }
    }

    /**
     * Generates a verification hash for a share to detect tampering.
     */
    public String generateShareVerificationHash(byte[] shareData) {
        return CryptoUtils.generateFullHash(shareData);
    }

    /**
     * Derives an operational key from a master key using PBKDF2.
     * This replaces key truncation with cryptographic derivation.
     *
     * @param masterKey The full master key (256-bit)
     * @param context Context string (e.g., "TPK:BANK-001:TERM-ATM-123")
     * @param outputBytes Desired key length (16 for AES-128, 32 for AES-256)
     * @return Derived key of specified length
     */
    public byte[] deriveOperationalKey(byte[] masterKey, String context, int outputBytes) {
        // Delegate to CryptoUtils for consistent key derivation
        return CryptoUtils.deriveKeyFromParent(masterKey, context, outputBytes * 8);
    }

    /**
     * Builds a context string for key derivation.
     * Format: "KEY_TYPE:BANK_ID:IDENTIFIER"
     *
     * @param keyType Key type (TPK, TSK, ZPK, etc.)
     * @param bankId Bank identifier
     * @param identifier Terminal ID, zone ID, or other unique identifier
     * @return Context string for derivation
     */
    public String buildKeyContext(String keyType, String bankId, String identifier) {
        return String.join(CryptoConstants.CONTEXT_SEPARATOR, keyType, bankId, identifier);
    }

    // ===== Private Helper Methods =====

    private BigInteger evaluatePolynomial(List<BigInteger> coefficients, BigInteger x, BigInteger prime) {
        BigInteger result = BigInteger.ZERO;
        BigInteger xPower = BigInteger.ONE;

        for (BigInteger coefficient : coefficients) {
            result = result.add(coefficient.multiply(xPower)).mod(prime);
            xPower = xPower.multiply(x).mod(prime);
        }

        return result;
    }

    private BigInteger lagrangeInterpolation(List<ShamirShare> shares, BigInteger prime) {
        BigInteger secret = BigInteger.ZERO;

        for (int i = 0; i < shares.size(); i++) {
            ShamirShare shareI = shares.get(i);
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < shares.size(); j++) {
                if (i != j) {
                    ShamirShare shareJ = shares.get(j);
                    numerator = numerator.multiply(shareJ.getXValue().negate()).mod(prime);
                    denominator = denominator.multiply(shareI.getXValue().subtract(shareJ.getXValue())).mod(prime);
                }
            }

            BigInteger lagrangeBasis = numerator.multiply(denominator.modInverse(prime)).mod(prime);
            secret = secret.add(shareI.getYValue().multiply(lagrangeBasis)).mod(prime);
        }

        return secret.mod(prime);
    }

    private byte[] serializeShare(ShamirShare share) {
        // Simple serialization: shareIndex (4 bytes) + yValue bytes + prime bytes
        byte[] yBytes = share.getYValue().toByteArray();
        byte[] primeBytes = share.getPrime().toByteArray();

        byte[] result = new byte[4 + 4 + yBytes.length + 4 + primeBytes.length + 4];
        int offset = 0;

        // Share index
        result[offset++] = (byte) (share.getShareIndex() >> 24);
        result[offset++] = (byte) (share.getShareIndex() >> 16);
        result[offset++] = (byte) (share.getShareIndex() >> 8);
        result[offset++] = (byte) share.getShareIndex();

        // Y value length and data
        result[offset++] = (byte) (yBytes.length >> 24);
        result[offset++] = (byte) (yBytes.length >> 16);
        result[offset++] = (byte) (yBytes.length >> 8);
        result[offset++] = (byte) yBytes.length;
        System.arraycopy(yBytes, 0, result, offset, yBytes.length);
        offset += yBytes.length;

        // Prime length and data
        result[offset++] = (byte) (primeBytes.length >> 24);
        result[offset++] = (byte) (primeBytes.length >> 16);
        result[offset++] = (byte) (primeBytes.length >> 8);
        result[offset++] = (byte) primeBytes.length;
        System.arraycopy(primeBytes, 0, result, offset, primeBytes.length);
        offset += primeBytes.length;

        // Threshold
        result[offset++] = (byte) (share.getThreshold() >> 24);
        result[offset++] = (byte) (share.getThreshold() >> 16);
        result[offset++] = (byte) (share.getThreshold() >> 8);
        result[offset] = (byte) share.getThreshold();

        return result;
    }

    private ShamirShare deserializeShare(byte[] data) {
        int offset = 0;

        // Share index
        int shareIndex = ((data[offset++] & 0xFF) << 24) |
                        ((data[offset++] & 0xFF) << 16) |
                        ((data[offset++] & 0xFF) << 8) |
                        (data[offset++] & 0xFF);

        // Y value
        int yLength = ((data[offset++] & 0xFF) << 24) |
                     ((data[offset++] & 0xFF) << 16) |
                     ((data[offset++] & 0xFF) << 8) |
                     (data[offset++] & 0xFF);
        byte[] yBytes = Arrays.copyOfRange(data, offset, offset + yLength);
        offset += yLength;
        BigInteger yValue = new BigInteger(yBytes);

        // Prime
        int primeLength = ((data[offset++] & 0xFF) << 24) |
                         ((data[offset++] & 0xFF) << 16) |
                         ((data[offset++] & 0xFF) << 8) |
                         (data[offset++] & 0xFF);
        byte[] primeBytes = Arrays.copyOfRange(data, offset, offset + primeLength);
        offset += primeLength;
        BigInteger prime = new BigInteger(primeBytes);

        // Threshold
        int threshold = ((data[offset++] & 0xFF) << 24) |
                       ((data[offset++] & 0xFF) << 16) |
                       ((data[offset++] & 0xFF) << 8) |
                       (data[offset] & 0xFF);

        return ShamirShare.builder()
                .shareIndex(shareIndex)
                .xValue(BigInteger.valueOf(shareIndex))
                .yValue(yValue)
                .prime(prime)
                .threshold(threshold)
                .build();
    }


    // ===== Result Classes =====

    @lombok.Data
    @lombok.Builder
    public static class MasterKeyResult {
        private byte[] keyData;
        private String fingerprint;
        private String checksum;
        private String combinedEntropyHash;
    }

    @lombok.Data
    @lombok.Builder
    public static class ShamirShare {
        private int shareIndex;
        private BigInteger xValue;
        private BigInteger yValue;
        private BigInteger prime;
        private int threshold;
    }
}
