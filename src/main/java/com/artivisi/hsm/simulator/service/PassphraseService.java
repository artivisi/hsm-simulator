package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.entity.PassphraseContribution;
import com.artivisi.hsm.simulator.util.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Service for passphrase validation, strength analysis, and secure hashing.
 * Uses Argon2id for passphrase hashing and implements entropy calculation
 * based on character diversity and length.
 */
@Service
@Slf4j
public class PassphraseService {

    private static final int MIN_LENGTH = 12;
    private static final int RECOMMENDED_LENGTH = 20;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^A-Za-z0-9]");

    private final PasswordEncoder passwordEncoder;

    public PassphraseService() {
        // Argon2id with recommended parameters for passphrase hashing
        // saltLength: 16 bytes, hashLength: 32 bytes, parallelism: 1, memory: 65536 KB, iterations: 3
        this.passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    /**
     * Validates and analyzes passphrase strength
     */
    public PassphraseValidationResult validatePassphrase(String passphrase) {
        if (passphrase == null || passphrase.isEmpty()) {
            return PassphraseValidationResult.builder()
                    .valid(false)
                    .errorMessage("Passphrase cannot be empty")
                    .build();
        }

        int length = passphrase.length();

        // Always calculate character type checks and entropy
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(passphrase).find();
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(passphrase).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(passphrase).find();
        boolean hasSpecial = SPECIAL_PATTERN.matcher(passphrase).find();

        BigDecimal entropyScore = calculateEntropy(passphrase, hasUppercase, hasLowercase, hasDigit, hasSpecial);
        PassphraseContribution.PassphraseStrength strength = determineStrength(entropyScore);

        // Check if passphrase meets minimum length requirement
        boolean isValid = length >= MIN_LENGTH;
        String errorMessage = isValid ? null :
            String.format("Passphrase must be at least %d characters long", MIN_LENGTH);

        return PassphraseValidationResult.builder()
                .valid(isValid)
                .errorMessage(errorMessage)
                .length(length)
                .hasUppercase(hasUppercase)
                .hasLowercase(hasLowercase)
                .hasDigit(hasDigit)
                .hasSpecial(hasSpecial)
                .entropyScore(entropyScore)
                .strength(strength)
                .meetsRecommendedLength(length >= RECOMMENDED_LENGTH)
                .build();
    }

    /**
     * Calculates entropy score based on character diversity and length.
     * Returns a score from 0 to 10.
     */
    public BigDecimal calculateEntropy(String passphrase, boolean hasUppercase, boolean hasLowercase,
                                       boolean hasDigit, boolean hasSpecial) {
        int length = passphrase.length();
        int charsetSize = 0;

        if (hasLowercase) charsetSize += 26;
        if (hasUppercase) charsetSize += 26;
        if (hasDigit) charsetSize += 10;
        if (hasSpecial) charsetSize += 32; // Common special characters

        if (charsetSize == 0) charsetSize = 1; // Avoid log(0)

        // Shannon entropy: H = L * log2(N)
        // where L = length, N = charset size
        double bitsOfEntropy = length * (Math.log(charsetSize) / Math.log(2));

        // Normalize to 0-10 scale (assuming max reasonable entropy is ~128 bits)
        double normalizedScore = (bitsOfEntropy / 128.0) * 10.0;

        // Cap at 10.0
        if (normalizedScore > 10.0) normalizedScore = 10.0;

        return BigDecimal.valueOf(normalizedScore)
                .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Determines passphrase strength based on entropy score
     */
    public PassphraseContribution.PassphraseStrength determineStrength(BigDecimal entropyScore) {
        double score = entropyScore.doubleValue();

        if (score < 4.0) return PassphraseContribution.PassphraseStrength.WEAK;
        if (score < 5.5) return PassphraseContribution.PassphraseStrength.FAIR;
        if (score < 7.0) return PassphraseContribution.PassphraseStrength.GOOD;
        if (score < 8.5) return PassphraseContribution.PassphraseStrength.STRONG;
        return PassphraseContribution.PassphraseStrength.VERY_STRONG;
    }

    /**
     * Hashes a passphrase using Argon2id.
     * Returns the hash string suitable for storage.
     */
    public String hashPassphrase(String passphrase) {
        log.debug("Hashing passphrase (length: {})", passphrase.length());
        return passwordEncoder.encode(passphrase);
    }

    /**
     * Verifies a plaintext passphrase against a stored hash.
     * Used for restoration operations where custodians re-enter passphrases.
     */
    public boolean verifyPassphrase(String plaintext, String hash) {
        return passwordEncoder.matches(plaintext, hash);
    }

    /**
     * Generates a contribution fingerprint from the passphrase hash.
     * This is used for verification without exposing the hash itself.
     */
    public String generateContributionFingerprint(String passphraseHash) {
        // Take SHA-256 of the Argon2 hash to create a fingerprint
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(passphraseHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return CryptoUtils.bytesToHexLowercase(hashBytes).substring(0, 16); // First 16 characters of hex
        } catch (Exception e) {
            log.error("Error generating contribution fingerprint", e);
            throw new RuntimeException("Failed to generate fingerprint", e);
        }
    }

    /**
     * Result object for passphrase validation
     */
    @lombok.Data
    @lombok.Builder
    public static class PassphraseValidationResult {
        private boolean valid;
        private String errorMessage;
        private int length;
        private boolean hasUppercase;
        private boolean hasLowercase;
        private boolean hasDigit;
        private boolean hasSpecial;
        private BigDecimal entropyScore;
        private PassphraseContribution.PassphraseStrength strength;
        private boolean meetsRecommendedLength;
    }
}
