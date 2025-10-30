package com.artivisi.hsm.simulator.service;

import com.artivisi.hsm.simulator.util.CryptoUtils;
import org.junit.jupiter.api.Test;

/**
 * Test to generate proper sample keys for V2 migration using actual key derivation.
 * This test uses CryptoUtils to ensure consistency with production code.
 * Run this test to get the correct hex values for master_keys table.
 */
public class SampleKeyGeneratorTest {

    @Test
    public void generateSampleKeysForV2Migration() throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("SAMPLE KEY GENERATION FOR V2 MIGRATION");
        System.out.println("Using CryptoUtils for consistency with production code");
        System.out.println("=".repeat(80));
        System.out.println();

        // Generate LMK (Local Master Key)
        byte[] lmkKey = CryptoUtils.generateRandomKey(256);
        System.out.println("-- LMK-ISS001-SAMPLE (Local Master Key)");
        System.out.println("key_data: decode('" + CryptoUtils.bytesToHex(lmkKey) + "', 'hex')");
        System.out.println("key_fingerprint: '" + CryptoUtils.generateFingerprint(lmkKey) + "'");
        System.out.println("key_checksum: '" + CryptoUtils.generateChecksum(lmkKey) + "'");
        System.out.println();

        // Generate TMK (Terminal Master Key)
        byte[] tmkKey = CryptoUtils.generateRandomKey(256);
        System.out.println("-- TMK-ISS001-SAMPLE (Terminal Master Key)");
        System.out.println("key_data: decode('" + CryptoUtils.bytesToHex(tmkKey) + "', 'hex')");
        System.out.println("key_fingerprint: '" + CryptoUtils.generateFingerprint(tmkKey) + "'");
        System.out.println("key_checksum: '" + CryptoUtils.generateChecksum(tmkKey) + "'");
        System.out.println();

        // Derive TPK from TMK using PBKDF2
        String tpkContext = "TPK:TRM-ISS001-ATM-001";
        byte[] tpkKey = CryptoUtils.deriveKeyFromParent(tmkKey, tpkContext, 256);
        System.out.println("-- TPK-TRM-ISS001-ATM-001 (Terminal PIN Key - DERIVED from TMK)");
        System.out.println("key_data: decode('" + CryptoUtils.bytesToHex(tpkKey) + "', 'hex')");
        System.out.println("key_fingerprint: '" + CryptoUtils.generateFingerprint(tpkKey) + "'");
        System.out.println("key_checksum: '" + CryptoUtils.generateChecksum(tpkKey) + "'");
        System.out.println("kdf_salt: 'TRM-ISS001-ATM-001'");
        System.out.println();

        // Derive TSK from TMK using PBKDF2
        String tskContext = "TSK:TRM-ISS001-ATM-001";
        byte[] tskKey = CryptoUtils.deriveKeyFromParent(tmkKey, tskContext, 256);
        System.out.println("-- TSK-TRM-ISS001-ATM-001 (Terminal Security Key - DERIVED from TMK)");
        System.out.println("key_data: decode('" + CryptoUtils.bytesToHex(tskKey) + "', 'hex')");
        System.out.println("key_fingerprint: '" + CryptoUtils.generateFingerprint(tskKey) + "'");
        System.out.println("key_checksum: '" + CryptoUtils.generateChecksum(tskKey) + "'");
        System.out.println("kdf_salt: 'TRM-ISS001-ATM-001'");
        System.out.println();

        // Verify derivation is deterministic
        System.out.println("=".repeat(80));
        System.out.println("VERIFICATION: Re-derive TPK to confirm deterministic output");
        System.out.println("=".repeat(80));
        byte[] tpkKeyVerify = CryptoUtils.deriveKeyFromParent(tmkKey, tpkContext, 256);
        boolean matches = java.util.Arrays.equals(tpkKey, tpkKeyVerify);
        System.out.println("TPK derivation is deterministic: " + matches);
        System.out.println();
    }

}
