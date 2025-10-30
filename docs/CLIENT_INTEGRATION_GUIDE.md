# HSM Simulator - Client Integration Guide

## Overview

This guide explains how to integrate client applications with the HSM Simulator API, focusing on the cryptographic standards and best practices implemented in the system.

**Version**: 2.0
**Last Updated**: 2025-10-30
**Security Standard**: AES-256, SHA-256, PBKDF2-SHA256

---

## Table of Contents

1. [Cryptographic Standards](#cryptographic-standards)
2. [Key Management](#key-management)
3. [PIN Operations](#pin-operations)
4. [MAC Operations](#mac-operations)
5. [Key Exchange Operations](#key-exchange-operations)
6. [Code Examples](#code-examples)
7. [Testing Guidelines](#testing-guidelines)
8. [Security Best Practices](#security-best-practices)

---

## Cryptographic Standards

### Master Cryptographic Configuration

All cryptographic operations in the HSM Simulator follow these standards:

| Component | Algorithm | Mode/Type | Key Size | Notes |
|-----------|-----------|-----------|----------|-------|
| **Master Keys** | AES | - | 256-bit | Generated via PBKDF2 or SecureRandom |
| **Share Encryption** | AES | GCM | 256-bit | 12-byte IV, 128-bit auth tag |
| **PIN Encryption** | AES | CBC | 128-bit* | Random IV per operation, PKCS5 padding |
| **MAC (Modern)** | AES | CMAC | 256-bit* | NIST SP 800-38B compliant |
| **MAC (HMAC)** | SHA-256 | HMAC | 256-bit* | Full or truncated output |
| **Key Wrapping** | AES | GCM | 256-bit* | Random IV per operation |
| **Hashing** | SHA-256 | - | - | All fingerprints and checksums |
| **Key Derivation** | PBKDF2 | SHA-256 | Variable | 100,000 iterations, unique context |

\* Derived from 256-bit master key using PBKDF2 with context

### Key Derivation Context Format

All operational keys are derived from master keys using:

```
Context = "KEY_TYPE:BANK_UUID:IDENTIFIER"
```

Examples:
- PIN Key: `"TPK:48a9e84c-ff57-4483-bf83-b255f34a6466:PIN"`
- MAC Key: `"TSK:48a9e84c-ff57-4483-bf83-b255f34a6466:MAC"`
- KEK: `"ZMK:48a9e84c-ff57-4483-bf83-b255f34a6466:KEK"`

**Important**: The derivation ensures that:
1. Each terminal has a unique operational key
2. Compromise of one terminal doesn't affect others
3. No key truncation occurs (full 256-bit entropy used)

⚠️ **CRITICAL**: `BANK_UUID` must be the **actual database UUID** (e.g., `48a9e84c-ff57-4483-bf83-b255f34a6466`), **NOT** the string `"GLOBAL"` or bank code like `"ISS001"`.

---

## Key Management

### Key Types and Hierarchy

```
HSM_MASTER_KEY (Root)
├── LMK (Local Master Key) - PIN storage
├── TMK (Terminal Master Key) - Terminal operations
│   ├── TPK (Terminal PIN Key) - Derived per terminal
│   └── TSK (Terminal Security Key) - Derived per terminal
└── ZMK (Zone Master Key) - Inter-bank operations
    ├── ZPK (Zone PIN Key) - Derived per zone
    └── ZSK (Zone Session Key) - Derived per zone
```

### Key Generation Methods

#### 1. Key Ceremony (Recommended for Production)

The most secure method using Shamir Secret Sharing (2-of-3 threshold):

```http
POST /api/ceremony/start
Content-Type: application/json

{
  "numberOfCustodians": 3,
  "threshold": 2,
  "keySize": 256,
  "algorithm": "AES",
  "purpose": "Production LMK Generation"
}
```

**Security Features:**
- Multi-custodian authorization required
- PBKDF2-SHA256 key derivation (100,000 iterations)
- Unique random salt per share (32 bytes)
- AES-256-GCM share encryption with random IV
- SHA-256 verification hashes

#### 2. Secure Random Generation (For Testing)

```http
POST /api/keys/initialize/lmk
Content-Type: application/json

{
  "keySize": 256
}
```

**Response:**
```json
{
  "masterKeyId": "LMK-20251030-001",
  "keyType": "LMK",
  "algorithm": "AES",
  "keySize": 256,
  "keyFingerprint": "a1b2c3d4e5f6789012345678",
  "keyChecksum": "abc123def456",
  "generationMethod": "SECURE_RANDOM",
  "status": "ACTIVE"
}
```

---

## Client-Side Key Derivation (REQUIRED)

**⚠️ CRITICAL REQUIREMENT**: Clients performing cryptographic operations (PIN encryption, MAC generation) **MUST derive operational keys** from master keys. **Never use master keys directly!**

#### Why Key Derivation is Required

| Scenario | Master Key | Operational Key | Result |
|----------|-----------|-----------------|--------|
| ❌ **Wrong** | 32-byte TPK (AES-256) | **Use directly** | BadPaddingException, decryption fails |
| ✅ **Correct** | 32-byte TPK (AES-256) | **Derive 16-byte key** via PBKDF2 | Successful encryption/decryption |

#### Why PBKDF2 Instead of Simple Truncation?

**Question**: Why not just truncate the 32-byte master key to 16 bytes?

```java
// ❌ Why is this WRONG and INSECURE?
byte[] tpkMasterKey = hexToBytes(tpkMasterKeyHex); // 32 bytes
byte[] operationalKey = Arrays.copyOf(tpkMasterKey, 16); // Just take first 16 bytes
```

**Security Justification:**

| Approach | Security Issues | Mitigation via PBKDF2 |
|----------|----------------|----------------------|
| **Simple Truncation** | • No domain separation (PIN key = MAC key if same master key)<br>• Predictable relationship between master and operational keys<br>• No computational barrier to brute-force<br>• Same key across different contexts | ✅ Context-based derivation creates unique keys per purpose<br>✅ One-way function prevents master key recovery<br>✅ 100,000 iterations add computational cost<br>✅ Different contexts yield cryptographically independent keys |
| **Direct Use (32-byte)** | • Key size mismatch with HSM's operational key size<br>• HSM expects AES-128 (16 bytes) for PIN/MAC ops<br>• Decryption failure due to wrong key | ✅ Consistent 16-byte operational keys<br>✅ Matches HSM's derived key exactly<br>✅ Successful encryption/decryption |

**Real-World Impact:**

```java
// Example: Bank ISS001 has TPK master key
byte[] tpkMaster = hexToBytes("246A31D7..."); // 32 bytes

// ❌ INSECURE: Simple truncation
byte[] pinKey = Arrays.copyOf(tpkMaster, 16);
byte[] macKey = Arrays.copyOf(tpkMaster, 16);
// Result: pinKey == macKey (SAME KEY FOR DIFFERENT PURPOSES!)

// ✅ SECURE: PBKDF2 with different contexts
byte[] pinKey = deriveKey(tpkMaster, "TPK:48a9e84c...:PIN", 128);
byte[] macKey = deriveKey(tpkMaster, "TPK:48a9e84c...:MAC", 128);
// Result: pinKey ≠ macKey (cryptographically independent)
```

**Key Benefits of PBKDF2 Derivation:**

1. **Domain Separation**: Different contexts (":PIN", ":MAC", ":KEK") produce completely different keys from the same master key
2. **Forward Security**: Compromising an operational key doesn't reveal the master key or other operational keys
3. **Brute-Force Protection**: 100,000 iterations make offline attacks computationally expensive
4. **Standard Compliance**: PBKDF2-SHA256 is NIST-approved (SP 800-132) for key derivation
5. **Context Binding**: Keys are cryptographically bound to their intended purpose and bank UUID

#### Key Derivation Algorithm

```
Algorithm: PBKDF2-SHA256
Iterations: 100,000 (MUST match HSM)
Salt: Context string as UTF-8 bytes
Input: Master key hex as char array
Output: 16 bytes (128 bits) for PIN/MAC operations
```

#### Java Implementation

```java
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Derive operational key from master key.
 *
 * CRITICAL: This MUST match the HSM's key derivation exactly!
 *
 * @param masterKeyBytes Master key (32 bytes for AES-256)
 * @param context Context string (e.g., "TPK:48a9e84c-ff57-4483-bf83-b255f34a6466:PIN")
 * @param outputBits Output key size in bits (128 for AES-128 PIN operations)
 * @return Derived operational key
 */
public static byte[] deriveOperationalKey(byte[] masterKeyBytes, String context, int outputBits) {
    try {
        // Convert master key bytes to hex, then to char array
        String masterKeyHex = bytesToHex(masterKeyBytes);
        char[] keyChars = masterKeyHex.toCharArray();

        // Use context as salt
        byte[] salt = context.getBytes(StandardCharsets.UTF_8);

        // PBKDF2 with 100,000 iterations (MUST match HSM!)
        PBEKeySpec spec = new PBEKeySpec(keyChars, salt, 100_000, outputBits);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        return factory.generateSecret(spec).getEncoded();
    } catch (Exception e) {
        throw new RuntimeException("Key derivation failed", e);
    }
}

// Example usage
String tpkMasterKeyHex = "246A31D729B280DD7FCDA3BB7F187ABFA1BB0811D7EF3D68FDCA63579F3748B0"; // 64 hex chars = 32 bytes
String bankUuid = "48a9e84c-ff57-4483-bf83-b255f34a6466"; // From database
String context = "TPK:" + bankUuid + ":PIN";

byte[] tpkMasterKey = hexToBytes(tpkMasterKeyHex); // 32 bytes
byte[] tpkOperationalKey = deriveOperationalKey(tpkMasterKey, context, 128); // 16 bytes

// Now use tpkOperationalKey (NOT tpkMasterKey!) for encryption
```

#### Python Implementation

```python
import hashlib
from typing import bytes

def derive_operational_key(master_key_bytes: bytes, context: str, output_bits: int = 128) -> bytes:
    """
    Derive operational key from master key using PBKDF2.

    CRITICAL: This MUST match the HSM's key derivation exactly!

    Args:
        master_key_bytes: Master key (32 bytes for AES-256)
        context: Context string (e.g., "TPK:48a9e84c-ff57-4483-bf83-b255f34a6466:PIN")
        output_bits: Output key size in bits (128 for AES-128 PIN operations)

    Returns:
        Derived operational key
    """
    # Convert master key bytes to hex
    master_key_hex = master_key_bytes.hex().upper()

    # Use context as salt
    salt = context.encode('utf-8')

    # PBKDF2 with 100,000 iterations (MUST match HSM!)
    output_bytes = output_bits // 8
    derived_key = hashlib.pbkdf2_hmac(
        'sha256',
        master_key_hex.encode('utf-8'),
        salt,
        100_000,
        dklen=output_bytes
    )

    return derived_key

# Example usage
tpk_master_key_hex = "246A31D729B280DD7FCDA3BB7F187ABFA1BB0811D7EF3D68FDCA63579F3748B0"
bank_uuid = "48a9e84c-ff57-4483-bf83-b255f34a6466"
context = f"TPK:{bank_uuid}:PIN"

tpk_master_key = bytes.fromhex(tpk_master_key_hex)  # 32 bytes
tpk_operational_key = derive_operational_key(tpk_master_key, context, 128)  # 16 bytes

# Now use tpk_operational_key (NOT tpk_master_key!) for encryption
```

#### Configuration Setup

**application.properties** (or equivalent):
```properties
# Master Key (from HSM database - 64 hex chars = 32 bytes)
hsm.tpk.master.key=246A31D729B280DD7FCDA3BB7F187ABFA1BB0811D7EF3D68FDCA63579F3748B0

# Bank UUID (from HSM database - MUST be actual UUID!)
hsm.bank.uuid=48a9e84c-ff57-4483-bf83-b255f34a6466

# DO NOT store derived keys - derive them at runtime!
```

#### Common Mistakes to Avoid

| ❌ Wrong | ✅ Correct |
|---------|-----------|
| Use master key directly for encryption | Derive operational key first |
| Context: `"TPK:GLOBAL:PIN"` | Context: `"TPK:48a9e84c...:PIN"` (actual UUID) |
| Context: `"TPK:ISS001:PIN"` (bank code) | Context: `"TPK:48a9e84c...:PIN"` (actual UUID) |
| Iterations: `10_000` | Iterations: `100_000` |
| Key size: 32 bytes (AES-256) | Key size: 16 bytes (AES-128) for PIN ops |
| Store derived keys in config | Derive at runtime from master key |

---

## PIN Operations

### PIN Block Format (ISO 9564)

Supported formats:
- **ISO-0**: XOR with PAN (most common)
- **ISO-1**: No PAN (simple padding)
- **ISO-3**: XOR with PAN (enhanced)
- **ISO-4**: XOR with PAN (extended PIN)

### Encrypt PIN

**Endpoint**: `POST /api/hsm/pin/encrypt`

**Request:**
```json
{
  "keyId": "TPK-SAMPLE-001",
  "accountNumber": "1234567890123456",
  "clearPin": "123456",
  "pinFormat": "ISO-0"
}
```

**Response:**
```json
{
  "encryptedPinBlock": "A1B2C3D4E5F6...",
  "pinVerificationValue": "1234",
  "keyCheckValue": "ABC123"
}
```

### Cryptographic Process (Client Side)

#### Encryption Flow:

```
1. Create PIN Block (ISO-0 format):
   PIN Field: 0x06 (length) + "123456" + random padding
   PAN Field: 0x0000 + last 13 digits of PAN (excluding check digit)
   PIN Block = PIN Field XOR PAN Field

2. Convert to bytes:
   pinBlockBytes = hexToBytes(pinBlock)

3. HSM derives operational key:
   context = "TPK:BANK-001:TERM-ATM-123"
   pinKey128 = deriveKey(masterKey256, context, 16 bytes)

4. Encrypt with AES-128-CBC:
   - Generate random IV (16 bytes)
   - Encrypt: ciphertext = AES_CBC_PKCS5(pinKey128, IV, pinBlockBytes)
   - Format: IV || ciphertext (IV prepended)

5. Return hex-encoded result
```

#### Java Example:

```java
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class PinEncryption {

    public static String encryptPinBlock(byte[] pinKey128, String pinBlock) throws Exception {
        // Convert PIN block to bytes
        byte[] pinBlockBytes = hexToBytes(pinBlock);

        // Generate random IV
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(pinKey128, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        // Encrypt
        byte[] encrypted = cipher.doFinal(pinBlockBytes);

        // Prepend IV to ciphertext
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

        return bytesToHex(result);
    }

    public static String decryptPinBlock(byte[] pinKey128, String encryptedHex) throws Exception {
        byte[] encryptedBytes = hexToBytes(encryptedHex);

        // Extract IV and ciphertext
        byte[] iv = new byte[16];
        byte[] ciphertext = new byte[encryptedBytes.length - 16];
        System.arraycopy(encryptedBytes, 0, iv, 0, 16);
        System.arraycopy(encryptedBytes, 16, ciphertext, 0, ciphertext.length);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(pinKey128, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        // Decrypt
        byte[] decrypted = cipher.doFinal(ciphertext);
        return bytesToHex(decrypted);
    }
}
```

---

## MAC Operations

### Supported MAC Algorithms

| Algorithm | Key Size | Output Size | Use Case |
|-----------|----------|-------------|----------|
| `AES-CMAC` | 256-bit | 128-bit (16 bytes) | Modern systems (recommended) |
| `AES-CMAC-256` | 256-bit | 128-bit (16 bytes) | Explicit AES-256 |
| `AES-CMAC-128` | 128-bit | 128-bit (16 bytes) | Legacy compatibility |
| `AES-CMAC-64` | 128-bit | 64-bit (8 bytes) | Banking compatibility |
| `HMAC-SHA256` | 256-bit | 128-bit (16 bytes) | Alternative modern |
| `HMAC-SHA256-FULL` | 256-bit | 256-bit (32 bytes) | Maximum security |
| `HMAC-SHA256-64` | 256-bit | 64-bit (8 bytes) | Banking compatibility |

### Generate MAC

**Endpoint**: `POST /api/hsm/mac/generate`

**Request:**
```json
{
  "keyId": "TSK-001",
  "message": "TRANSACTION_DATA_HERE",
  "algorithm": "AES-CMAC"
}
```

**Response:**
```json
{
  "macValue": "A1B2C3D4E5F67890",
  "algorithm": "AES-CMAC",
  "keyCheckValue": "ABC123"
}
```

### Verify MAC

**Endpoint**: `POST /api/hsm/mac/verify`

**Request:**
```json
{
  "keyId": "TSK-001",
  "message": "TRANSACTION_DATA_HERE",
  "macValue": "A1B2C3D4E5F67890",
  "algorithm": "AES-CMAC"
}
```

**Response:**
```json
{
  "valid": true
}
```

### Client-Side MAC Key Derivation (REQUIRED)

**⚠️ CRITICAL REQUIREMENT**: Just like PIN operations, MAC generation/verification **MUST use derived operational keys** from TSK (Terminal Security Key) master key. **Never use TSK master key directly!**

#### Key Derivation for MAC Operations

| Scenario | TSK Master Key | MAC Operational Key | Result |
|----------|---------------|---------------------|--------|
| ❌ **Wrong** | 32-byte TSK (AES-256) | **Use directly** | MAC mismatch, verification fails |
| ✅ **Correct** | 32-byte TSK (AES-256) | **Derive 16-byte key** via PBKDF2 | Successful MAC generation/verification |

**Why Not Simple Truncation for MAC Keys?**

Just like PIN operations, **PBKDF2 derivation is required instead of truncation** for the same security reasons:

- **Domain Separation**: Prevents using the same key material for different purposes (authentication vs. encryption)
- **Forward Security**: Compromising MAC key doesn't reveal TSK master key
- **Brute-Force Protection**: 100,000 iterations add computational cost to attacks
- **Context Binding**: MAC keys are cryptographically bound to bank UUID and MAC purpose

**Example of the Problem:**

```python
# ❌ INSECURE: Truncation means same key for all purposes
tsk_master = bytes.fromhex("3AC63878...")  # 32 bytes
mac_key = tsk_master[:16]  # Just take first 16 bytes
enc_key = tsk_master[:16]  # SAME KEY - security violation!

# ✅ SECURE: PBKDF2 creates different keys per context
mac_key = derive_key(tsk_master, "TSK:48a9e84c...:MAC", 128)
enc_key = derive_key(tsk_master, "TSK:48a9e84c...:ENC", 128)
# Result: mac_key ≠ enc_key (cryptographically independent)
```

**Derivation Parameters:**
```
Algorithm: PBKDF2-SHA256
Iterations: 100,000 (MUST match HSM)
Context: "TSK:<BANK_UUID>:MAC"
Output: 16 bytes (128 bits) for AES-CMAC operations
```

**Example Context:**
```
Context = "TSK:48a9e84c-ff57-4483-bf83-b255f34a6466:MAC"
```

⚠️ **CRITICAL**: Use the **actual database UUID** of the bank (e.g., `48a9e84c-ff57-4483-bf83-b255f34a6466`), **NOT** `"GLOBAL"` or bank code like `"ISS001"`.

### AES-CMAC Implementation (Client Side)

#### Java Example with Key Derivation:

```java
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MacGeneration {

    /**
     * Derive MAC operational key from TSK master key.
     * MUST match HSM's key derivation!
     */
    public static byte[] deriveMacKey(byte[] tskMasterKey, String bankUuid) throws Exception {
        // Convert master key to hex
        String masterKeyHex = bytesToHex(tskMasterKey);
        char[] keyChars = masterKeyHex.toCharArray();

        // Create context for MAC operations
        String context = "TSK:" + bankUuid + ":MAC";
        byte[] salt = context.getBytes(StandardCharsets.UTF_8);

        // PBKDF2 with 100,000 iterations
        PBEKeySpec spec = new PBEKeySpec(keyChars, salt, 100_000, 128); // 128 bits = 16 bytes
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Calculate AES-CMAC using derived operational key.
     */
    public static String calculateAesCmac(byte[] macKey128, String message, int outputBytes) throws Exception {
        // Initialize AES-CMAC
        Mac mac = Mac.getInstance("AESCMAC", "BC"); // Requires BouncyCastle
        SecretKeySpec keySpec = new SecretKeySpec(macKey128, "AES");
        mac.init(keySpec);

        // Calculate MAC
        byte[] messageBytes = message.getBytes("UTF-8");
        byte[] fullMac = mac.doFinal(messageBytes);

        // Truncate if needed (for banking compatibility)
        byte[] macOutput = (outputBytes < fullMac.length)
            ? Arrays.copyOf(fullMac, outputBytes)
            : fullMac;

        return bytesToHex(macOutput);
    }

    /**
     * Complete example: Derive key and calculate MAC.
     */
    public static String generateMac(String tskMasterKeyHex, String bankUuid, String message) throws Exception {
        // Step 1: Get TSK master key (32 bytes)
        byte[] tskMasterKey = hexToBytes(tskMasterKeyHex);

        // Step 2: Derive MAC operational key (16 bytes)
        byte[] macKey = deriveMacKey(tskMasterKey, bankUuid);

        // Step 3: Calculate MAC with derived key
        return calculateAesCmac(macKey, message, 8); // 8 bytes for banking
    }

    public static String calculateHmacSha256(byte[] macKey128, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(macKey128, "HmacSHA256");
        mac.init(keySpec);

        byte[] macBytes = mac.doFinal(message.getBytes("UTF-8"));
        return bytesToHex(macBytes);
    }
}

// Usage example:
String tskMasterKeyHex = "3AC638783EF600FE5E25E8A2EE5B0D222EB810DDF64C3681DD11AFEFAF41614B";
String bankUuid = "48a9e84c-ff57-4483-bf83-b255f34a6466";
String message = "0800822000000000000004000000000000000000001234567890123456";

// ❌ WRONG: Using master key directly
// String mac = calculateAesCmac(hexToBytes(tskMasterKeyHex), message, 8);

// ✅ CORRECT: Derive operational key first
String mac = generateMac(tskMasterKeyHex, bankUuid, message);
```

#### Python Example with Key Derivation:

```python
import hashlib
from cryptography.hazmat.primitives import cmac, hashes, hmac
from cryptography.hazmat.primitives.ciphers import algorithms
from cryptography.hazmat.backends import default_backend

def derive_mac_key(tsk_master_key: bytes, bank_uuid: str) -> bytes:
    """
    Derive MAC operational key from TSK master key.
    MUST match HSM's key derivation!

    Args:
        tsk_master_key: TSK master key (32 bytes for AES-256)
        bank_uuid: Bank UUID from database (e.g., "48a9e84c-ff57-4483-bf83-b255f34a6466")

    Returns:
        Derived MAC key (16 bytes for AES-128)
    """
    # Convert master key to hex
    master_key_hex = tsk_master_key.hex().upper()

    # Create context for MAC operations
    context = f"TSK:{bank_uuid}:MAC"
    salt = context.encode('utf-8')

    # PBKDF2 with 100,000 iterations
    derived_key = hashlib.pbkdf2_hmac(
        'sha256',
        master_key_hex.encode('utf-8'),
        salt,
        100_000,
        dklen=16  # 16 bytes = 128 bits
    )

    return derived_key

def calculate_aes_cmac(mac_key_128: bytes, message: str, output_bytes: int = 16) -> str:
    """Calculate AES-CMAC using derived operational key"""
    c = cmac.CMAC(algorithms.AES(mac_key_128), backend=default_backend())
    c.update(message.encode('utf-8'))
    full_mac = c.finalize()

    # Truncate if needed
    mac_output = full_mac[:output_bytes]
    return mac_output.hex().upper()

def generate_mac(tsk_master_key_hex: str, bank_uuid: str, message: str) -> str:
    """
    Complete example: Derive key and calculate MAC.

    Args:
        tsk_master_key_hex: TSK master key as hex string (64 hex chars = 32 bytes)
        bank_uuid: Bank UUID from database
        message: Message to MAC

    Returns:
        MAC value as hex string
    """
    # Step 1: Get TSK master key (32 bytes)
    tsk_master_key = bytes.fromhex(tsk_master_key_hex)

    # Step 2: Derive MAC operational key (16 bytes)
    mac_key = derive_mac_key(tsk_master_key, bank_uuid)

    # Step 3: Calculate MAC with derived key
    return calculate_aes_cmac(mac_key, message, output_bytes=8)  # 8 bytes for banking

def calculate_hmac_sha256(mac_key_128: bytes, message: str) -> str:
    """Calculate HMAC-SHA256"""
    h = hmac.HMAC(mac_key_128, hashes.SHA256(), backend=default_backend())
    h.update(message.encode('utf-8'))
    return h.finalize().hex().upper()

# Usage example:
tsk_master_key_hex = "3AC638783EF600FE5E25E8A2EE5B0D222EB810DDF64C3681DD11AFEFAF41614B"
bank_uuid = "48a9e84c-ff57-4483-bf83-b255f34a6466"
message = "0800822000000000000004000000000000000000001234567890123456"

# ❌ WRONG: Using master key directly
# mac = calculate_aes_cmac(bytes.fromhex(tsk_master_key_hex), message, 8)

# ✅ CORRECT: Derive operational key first
mac = generate_mac(tsk_master_key_hex, bank_uuid, message)
```

#### Common MAC Key Derivation Mistakes

| ❌ Wrong | ✅ Correct |
|---------|-----------|
| Use TSK master key (32 bytes) directly | Derive 16-byte operational key via PBKDF2 |
| Context: `"TSK:GLOBAL:MAC"` | Context: `"TSK:48a9e84c...:MAC"` (actual UUID) |
| Context: `"TSK:ISS001:MAC"` (bank code) | Context: `"TSK:48a9e84c...:MAC"` (actual UUID) |
| Different iteration count | Exactly 100,000 iterations |
| Store derived keys in config | Derive at runtime from master key |

---

## Key Exchange Operations

### Inter-Bank Key Exchange

**Endpoint**: `POST /api/hsm/key/exchange`

**Request:**
```json
{
  "sourceKeyId": "ZMK-BANK-A",
  "targetKeyId": "ZMK-BANK-B",
  "keyType": "ZPK"
}
```

**Response:**
```json
{
  "encryptedKey": "A1B2C3D4...",
  "keyCheckValue": "ABC123"
}
```

### Key Wrapping Process

```
1. Generate session key (random 128/256-bit)
2. Derive KEK from target ZMK:
   context = "ZMK:BANK-B:KEK"
   kek256 = deriveKey(targetZMK256, context, 32 bytes)

3. Wrap key with AES-256-GCM:
   - Generate random IV (12 bytes)
   - Encrypt: ciphertext = AES_GCM(kek256, IV, sessionKey)
   - Format: IV || ciphertext || auth_tag

4. Return hex-encoded wrapped key
```

---

## Code Examples

### Complete Client Implementation (Java)

```java
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.Arrays;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HsmClient {

    private static final String HSM_BASE_URL = "http://localhost:8080/api/hsm";
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HsmClient() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Encrypt PIN using HSM
     */
    public PinEncryptResponse encryptPin(String keyId, String accountNumber,
                                         String clearPin, String pinFormat) throws Exception {
        HttpPost request = new HttpPost(HSM_BASE_URL + "/pin/encrypt");
        request.setHeader("Content-Type", "application/json");

        PinEncryptRequest req = new PinEncryptRequest();
        req.setKeyId(keyId);
        req.setAccountNumber(accountNumber);
        req.setClearPin(clearPin);
        req.setPinFormat(pinFormat);

        String jsonBody = objectMapper.writeValueAsString(req);
        request.setEntity(new StringEntity(jsonBody));

        return httpClient.execute(request, response -> {
            String body = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(body, PinEncryptResponse.class);
        });
    }

    /**
     * Generate MAC using HSM
     */
    public MacResponse generateMac(String keyId, String message, String algorithm) throws Exception {
        HttpPost request = new HttpPost(HSM_BASE_URL + "/mac/generate");
        request.setHeader("Content-Type", "application/json");

        MacRequest req = new MacRequest();
        req.setKeyId(keyId);
        req.setMessage(message);
        req.setAlgorithm(algorithm);

        String jsonBody = objectMapper.writeValueAsString(req);
        request.setEntity(new StringEntity(jsonBody));

        return httpClient.execute(request, response -> {
            String body = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(body, MacResponse.class);
        });
    }

    /**
     * Verify MAC using HSM
     */
    public boolean verifyMac(String keyId, String message, String macValue, String algorithm) throws Exception {
        HttpPost request = new HttpPost(HSM_BASE_URL + "/mac/verify");
        request.setHeader("Content-Type", "application/json");

        MacVerifyRequest req = new MacVerifyRequest();
        req.setKeyId(keyId);
        req.setMessage(message);
        req.setMacValue(macValue);
        req.setAlgorithm(algorithm);

        String jsonBody = objectMapper.writeValueAsString(req);
        request.setEntity(new StringEntity(jsonBody));

        return httpClient.execute(request, response -> {
            String body = EntityUtils.toString(response.getEntity());
            MacVerifyResponse resp = objectMapper.readValue(body, MacVerifyResponse.class);
            return resp.isValid();
        });
    }

    // Helper method: Convert hex string to bytes
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    // Helper method: Convert bytes to hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
```

### Complete Client Implementation (Python)

```python
import requests
from typing import Dict, Optional
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import cmac
from cryptography.hazmat.backends import default_backend
import secrets

class HsmClient:
    """Client for HSM Simulator API"""

    def __init__(self, base_url: str = "http://localhost:8080/api/hsm"):
        self.base_url = base_url
        self.session = requests.Session()

    def encrypt_pin(self, key_id: str, account_number: str,
                    clear_pin: str, pin_format: str = "ISO-0") -> Dict:
        """Encrypt PIN using HSM"""
        url = f"{self.base_url}/pin/encrypt"
        payload = {
            "keyId": key_id,
            "accountNumber": account_number,
            "clearPin": clear_pin,
            "pinFormat": pin_format
        }

        response = self.session.post(url, json=payload)
        response.raise_for_status()
        return response.json()

    def generate_mac(self, key_id: str, message: str,
                     algorithm: str = "AES-CMAC") -> Dict:
        """Generate MAC using HSM"""
        url = f"{self.base_url}/mac/generate"
        payload = {
            "keyId": key_id,
            "message": message,
            "algorithm": algorithm
        }

        response = self.session.post(url, json=payload)
        response.raise_for_status()
        return response.json()

    def verify_mac(self, key_id: str, message: str, mac_value: str,
                   algorithm: str = "AES-CMAC") -> bool:
        """Verify MAC using HSM"""
        url = f"{self.base_url}/mac/verify"
        payload = {
            "keyId": key_id,
            "message": message,
            "macValue": mac_value,
            "algorithm": algorithm
        }

        response = self.session.post(url, json=payload)
        response.raise_for_status()
        return response.json().get("valid", False)

# Usage example
if __name__ == "__main__":
    client = HsmClient()

    # Encrypt PIN
    pin_result = client.encrypt_pin(
        key_id="TPK-SAMPLE-001",
        account_number="1234567890123456",
        clear_pin="123456",
        pin_format="ISO-0"
    )
    print(f"Encrypted PIN Block: {pin_result['encryptedPinBlock']}")
    print(f"PVV: {pin_result['pinVerificationValue']}")

    # Generate MAC
    mac_result = client.generate_mac(
        key_id="TSK-001",
        message="TRANSACTION_DATA",
        algorithm="AES-CMAC"
    )
    print(f"MAC Value: {mac_result['macValue']}")

    # Verify MAC
    is_valid = client.verify_mac(
        key_id="TSK-001",
        message="TRANSACTION_DATA",
        mac_value=mac_result['macValue'],
        algorithm="AES-CMAC"
    )
    print(f"MAC Valid: {is_valid}")
```

---

## Testing Guidelines

### Unit Testing

```java
@Test
public void testPinEncryptionDecryptionRoundTrip() throws Exception {
    HsmClient client = new HsmClient();

    // Encrypt PIN
    PinEncryptResponse encrypted = client.encryptPin(
        "TPK-SAMPLE-001",
        "1234567890123456",
        "123456",
        "ISO-0"
    );

    assertNotNull(encrypted.getEncryptedPinBlock());
    assertNotNull(encrypted.getPinVerificationValue());

    // Verify PIN (would normally decrypt and compare)
    // In production, use PVV for verification instead of decryption
}

@Test
public void testMacGenerationVerification() throws Exception {
    HsmClient client = new HsmClient();

    String message = "TEST_TRANSACTION_DATA";

    // Generate MAC
    MacResponse mac = client.generateMac("TSK-001", message, "AES-CMAC");

    assertNotNull(mac.getMacValue());
    assertEquals(32, mac.getMacValue().length()); // 16 bytes = 32 hex chars

    // Verify MAC
    boolean valid = client.verifyMac("TSK-001", message, mac.getMacValue(), "AES-CMAC");
    assertTrue(valid);

    // Verify with wrong MAC should fail
    boolean invalid = client.verifyMac("TSK-001", message, "WRONG_MAC", "AES-CMAC");
    assertFalse(invalid);
}
```

### Integration Testing

```bash
# Start HSM Simulator
docker compose up -d postgres
mvn spring-boot:run

# Test PIN Encryption
curl -X POST http://localhost:8080/api/hsm/pin/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "keyId": "TPK-SAMPLE-001",
    "accountNumber": "1234567890123456",
    "clearPin": "123456",
    "pinFormat": "ISO-0"
  }'

# Test MAC Generation
curl -X POST http://localhost:8080/api/hsm/mac/generate \
  -H "Content-Type: application/json" \
  -d '{
    "keyId": "TSK-001",
    "message": "TRANSACTION_DATA",
    "algorithm": "AES-CMAC"
  }'
```

---

## Security Best Practices

### 1. Key Management

✅ **DO:**
- Use Key Ceremony for production master key generation
- Rotate keys according to your security policy
- Store key IDs, not key material, in application databases
- Use unique contexts for key derivation
- Implement proper key lifecycle management (ACTIVE, SUSPENDED, REVOKED)

❌ **DON'T:**
- Hardcode key material in your application
- Reuse the same key for multiple purposes
- Transmit unencrypted key material over the network
- Use sample/test keys in production
- Skip key rotation

### 2. PIN Security

✅ **DO:**
- Always use PVV (PIN Verification Value) for PIN verification
- Use ISO-0 format for PAN-based PIN blocks
- Implement PIN retry limits (3-5 attempts)
- Use TLS for all PIN transmission
- Log PIN verification attempts

❌ **DON'T:**
- Store clear PINs anywhere
- Log PIN values (encrypted or clear)
- Transmit PINs without encryption
- Implement custom PIN encryption (use HSM)
- Skip PAN validation

### 3. MAC Security

✅ **DO:**
- Use AES-CMAC for new implementations
- Include transaction timestamp in MAC calculation
- Implement MAC verification on all critical operations
- Use separate MAC keys for different message types
- Log MAC verification failures

❌ **DON'T:**
- Reuse MACs across different messages
- Skip MAC verification
- Use deprecated algorithms (DES, MD5)
- Truncate MACs unnecessarily
- Implement custom MAC algorithms

### 4. Network Security

✅ **DO:**
- Always use TLS 1.2 or higher
- Implement mutual TLS for production
- Use certificate pinning where possible
- Implement rate limiting
- Monitor for unusual patterns

❌ **DON'T:**
- Expose HSM API directly to the internet
- Skip certificate validation
- Use self-signed certificates in production
- Log sensitive data in plain text

### 5. Error Handling

✅ **DO:**
- Return generic error messages to clients
- Log detailed errors server-side
- Implement retry logic with exponential backoff
- Validate all inputs before sending to HSM
- Handle timeout scenarios gracefully

❌ **DON'T:**
- Expose internal error details to clients
- Retry indefinitely
- Skip input validation
- Ignore HSM errors
- Assume operations always succeed

---

## Support and Resources

### Documentation
- **API Reference**: http://localhost:8080/swagger-ui.html (when running)
- **Source Code**: https://github.com/your-org/hsm-simulator
- **CLAUDE.md**: Project-specific implementation details

### Cryptographic Standards
- **NIST SP 800-38B**: AES-CMAC Specification
- **ISO 9564**: PIN Management and Security
- **ISO 9797-1**: Message Authentication Codes
- **RFC 5869**: HKDF Specification
- **RFC 8018**: PBKDF2 Specification

### Security Contacts
- **Security Issues**: Report to security@hsm-simulator.local
- **General Support**: support@hsm-simulator.local

---

## Changelog

### Version 2.0 (2025-10-30)
- **BREAKING**: Replaced all DES with AES-CMAC
- **BREAKING**: Replaced ECB mode with CBC/GCM
- **BREAKING**: Replaced MD5 with SHA-256
- **BREAKING**: Replaced key truncation with PBKDF2 derivation
- **NEW**: Added context-based key derivation
- **NEW**: Added unique salt per share in key ceremony
- **NEW**: Added caching for derived keys
- **IMPROVED**: Enhanced security across all operations
- **REMOVED**: All legacy DES-based MAC algorithms
- **REMOVED**: All ECB mode usage (except KCV calculation)
- **REMOVED**: All MD5 hashing

### Version 1.0 (2024-XX-XX)
- Initial release with basic HSM functionality

---

## License

This HSM Simulator is for **educational purposes only**. Do not use in production without proper security review and hardening.

---

**End of Client Integration Guide**
