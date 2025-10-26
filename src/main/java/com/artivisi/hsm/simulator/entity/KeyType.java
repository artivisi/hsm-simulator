package com.artivisi.hsm.simulator.entity;

/**
 * Enumeration of cryptographic key types used in HSM operations
 */
public enum KeyType {
    /**
     * Local Master Key - Root key generated through key ceremony,
     * used for PIN storage encryption in HSM
     */
    LMK("Local Master Key"),

    /**
     * Terminal Master Key - Encrypts key distribution to terminals
     */
    TMK("Terminal Master Key"),

    /**
     * Terminal PIN Key - Encrypts PIN blocks at terminal level (child of TMK)
     */
    TPK("Terminal PIN Key"),

    /**
     * Terminal Security Key - Provides MAC and authentication (child of TMK)
     */
    TSK("Terminal Security Key"),

    /**
     * Zone Master Key - Encrypts inter-bank key exchanges
     */
    ZMK("Zone Master Key"),

    /**
     * Zone PIN Key - Protects PIN data between banks (child of ZMK)
     */
    ZPK("Zone PIN Key"),

    /**
     * Zone Session Key - Encrypts inter-bank transaction data (child of ZMK)
     */
    ZSK("Zone Session Key"),

    /**
     * Key Encryption Key - Generic key encryption key
     */
    KEK("Key Encryption Key");

    private final String description;

    KeyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
