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

**WARNING**: **CRITICAL**: `BANK_UUID` must be the **actual database UUID** (e.g., `48a9e84c-ff57-4483-bf83-b255f34a6466`), **NOT** the string `"GLOBAL"` or bank code like `"ISS001"`.

#### TPK/TSK Derivation Context: Bank UUID vs Terminal ID

**CRITICAL IMPLEMENTATION DETAIL** for terminal keys (TPK/TSK):

| Aspect | Uses Terminal ID | Uses Bank UUID |
|--------|------------------|----------------|
| **Master Key Generation** | **`kdfSalt` field stores terminal ID (e.g., `"TRM-ISS001-ATM-001"`) | **`idBank` field stores bank UUID |
| **Master Key Identification** | **`masterKeyId` includes terminal ID (e.g., `"TPK-TRM-ISS001-ATM-001"`) | - |
| **Database Associations** | **`idTerminal` foreign key | **`idBank` foreign key |
| **Operational Key Derivation** | ****NOT USED** | ****REQUIRED** in context string |

**Why This Matters:**

The confusion arises because:
1. **Master key storage** uses terminal ID for identification and salt
2. **Operational key derivation** uses bank UUID in the context string

```java
// WRONG: Using terminal ID in derivation context
String terminalId = "TRM-ISS001-ATM-001";
String wrongContext = "TPK:" + terminalId + ":PIN";  // **WILL FAIL!

// CORRECT: Using bank UUID in derivation context
UUID bankUuid = UUID.fromString("48a9e84c-ff57-4483-bf83-b255f34a6466");
String correctContext = "TPK:" + bankUuid + ":PIN";  // **WORKS
```

**Real-World Impact After Key Rotation:**

```
Before Rotation:
  Master Key: TPK-TRM-ISS001-ATM-001
  - idBank: 48a9e84c-ff57-4483-bf83-b255f34a6466 ✓
  - idTerminal: <terminal_uuid> ✓
  - kdfSalt: "TRM-ISS001-ATM-001" ✓
  Context: "TPK:48a9e84c-ff57-4483-bf83-b255f34a6466:PIN" ✓
  Result: PIN decryption WORKS ✓

After Rotation (if bank/terminal IDs missing):
  Master Key: TPK-TRMISS001ATM001-4B6E3217
  - idBank: NULL (INCORRECT)
  - idTerminal: NULL (INCORRECT)
  - kdfSalt: "TRM-ISS001-ATM-001" ✓
  Context: "TPK:GLOBAL:PIN" **(fallback due to NULL idBank)
  Result: PIN decryption FAILS **BadPaddingException
```

**Client Requirements:**

1. **DO NOT** construct context from terminal ID
2. **DO** obtain bank UUID from HSM key metadata
3. **DO** use the same bank UUID before and after rotation
4. **DO NOT** use `"GLOBAL"` or bank code strings

**How to Get Bank UUID:**

```java
// Option 1: From key retrieval response
KeyResponse key = hsmClient.getKey(terminalId, "TPK");
UUID bankUuid = key.getBankId();  // Use this for derivation

// Option 2: From terminal configuration (stored during provisioning)
Terminal terminal = configService.getTerminal(terminalId);
UUID bankUuid = terminal.getBankId();

// Derive operational key
String context = "TPK:" + bankUuid + ":PIN";
byte[] operationalKey = deriveOperationalKey(masterKeyBytes, context, 128);
```

**HSM Implementation Note:**

The HSM ensures all rotated keys maintain bank/terminal associations:
- New TPK keys automatically copy `idBank` and `idTerminal` from old keys
- Derivation context remains consistent across rotations
- No client-side changes needed after rotation

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

****WARNING**: CRITICAL REQUIREMENT**: Clients performing cryptographic operations (PIN encryption, MAC generation) **MUST derive operational keys** from master keys. **Never use master keys directly!**

#### Why Key Derivation is Required

| Scenario | Master Key | Operational Key | Result |
|----------|-----------|-----------------|--------|
| ****Wrong** | 32-byte TPK (AES-256) | **Use directly** | BadPaddingException, decryption fails |
| ****Correct** | 32-byte TPK (AES-256) | **Derive 16-byte key** via PBKDF2 | Successful encryption/decryption |

#### Why PBKDF2 Instead of Simple Truncation?

**Question**: Why not just truncate the 32-byte master key to 16 bytes?

```java
// **Why is this WRONG and INSECURE?
byte[] tpkMasterKey = hexToBytes(tpkMasterKeyHex); // 32 bytes
byte[] operationalKey = Arrays.copyOf(tpkMasterKey, 16); // Just take first 16 bytes
```

**Security Justification:**

| Approach | Security Issues | Mitigation via PBKDF2 |
|----------|----------------|----------------------|
| **Simple Truncation** | • No domain separation (PIN key = MAC key if same master key)<br>• Predictable relationship between master and operational keys<br>• No computational barrier to brute-force<br>• Same key across different contexts | **Context-based derivation creates unique keys per purpose<br>**One-way function prevents master key recovery<br>**100,000 iterations add computational cost<br>**Different contexts yield cryptographically independent keys |
| **Direct Use (32-byte)** | • Key size mismatch with HSM's operational key size<br>• HSM expects AES-128 (16 bytes) for PIN/MAC ops<br>• Decryption failure due to wrong key | **Consistent 16-byte operational keys<br>**Matches HSM's derived key exactly<br>**Successful encryption/decryption |

**Real-World Impact:**

```java
// Example: Bank ISS001 has TPK master key
byte[] tpkMaster = hexToBytes("246A31D7..."); // 32 bytes

// **INSECURE: Simple truncation
byte[] pinKey = Arrays.copyOf(tpkMaster, 16);
byte[] macKey = Arrays.copyOf(tpkMaster, 16);
// Result: pinKey == macKey (SAME KEY FOR DIFFERENT PURPOSES!)

// **SECURE: PBKDF2 with different contexts
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

| **Wrong | **Correct |
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

****WARNING**: CRITICAL REQUIREMENT**: Just like PIN operations, MAC generation/verification **MUST use derived operational keys** from TSK (Terminal Security Key) master key. **Never use TSK master key directly!**

#### Key Derivation for MAC Operations

| Scenario | TSK Master Key | MAC Operational Key | Result |
|----------|---------------|---------------------|--------|
| ****Wrong** | 32-byte TSK (AES-256) | **Use directly** | MAC mismatch, verification fails |
| ****Correct** | 32-byte TSK (AES-256) | **Derive 16-byte key** via PBKDF2 | Successful MAC generation/verification |

**Why Not Simple Truncation for MAC Keys?**

Just like PIN operations, **PBKDF2 derivation is required instead of truncation** for the same security reasons:

- **Domain Separation**: Prevents using the same key material for different purposes (authentication vs. encryption)
- **Forward Security**: Compromising MAC key doesn't reveal TSK master key
- **Brute-Force Protection**: 100,000 iterations add computational cost to attacks
- **Context Binding**: MAC keys are cryptographically bound to bank UUID and MAC purpose

**Example of the Problem:**

```python
# **INSECURE: Truncation means same key for all purposes
tsk_master = bytes.fromhex("3AC63878...")  # 32 bytes
mac_key = tsk_master[:16]  # Just take first 16 bytes
enc_key = tsk_master[:16]  # SAME KEY - security violation!

# **SECURE: PBKDF2 creates different keys per context
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

**WARNING**: **CRITICAL**: Use the **actual database UUID** of the bank (e.g., `48a9e84c-ff57-4483-bf83-b255f34a6466`), **NOT** `"GLOBAL"` or bank code like `"ISS001"`.

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

// **WRONG: Using master key directly
// String mac = calculateAesCmac(hexToBytes(tskMasterKeyHex), message, 8);

// **CORRECT: Derive operational key first
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

# **WRONG: Using master key directly
# mac = calculate_aes_cmac(bytes.fromhex(tsk_master_key_hex), message, 8)

# **CORRECT: Derive operational key first
mac = generate_mac(tsk_master_key_hex, bank_uuid, message)
```

#### Common MAC Key Derivation Mistakes

| **Wrong | **Correct |
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

## Key Rotation Operations

### Overview

The HSM supports secure key rotation with **pending state tracking**. This allows ATM terminals and banking applications to update keys without service interruption.

**Key Rotation States:**
- `PENDING`: Terminal/bank needs to fetch new key
- `DELIVERED`: New key sent to terminal (encrypted)
- `CONFIRMED`: Terminal confirmed key installation
- `FAILED`: Update failed
- `IN_PROGRESS`: Rotation ongoing
- `COMPLETED`: All participants confirmed, old key rotated

### Rotation Workflows

#### Terminal-Initiated Rotation (Scheduled Tasks) - Streamlined

```
1. Terminal Scheduled Task → POST /api/hsm/terminal/{terminalId}/request-rotation
   ↓
2. HSM → Auto-Approve, Generate New Key, Encrypt & Return Immediately (IN_PROGRESS)
   ↓ (Response includes encryptedNewKey)
3. Terminal → Decrypt, Install, Test New Key
   ↓
4. Terminal → POST /api/hsm/terminal/{terminalId}/confirm-key-update
   ↓
5. HSM → Auto-Complete Rotation (revoke old key)
```

**Optimization**: Terminal-initiated rotation delivers the encrypted new key immediately in the initial response, eliminating the need for a separate `get-updated-key` API call. This streamlines the workflow from 7 steps to 5 steps.

#### Admin-Initiated Rotation (Bank-Wide)

```
1. HSM Administrator → POST /api/hsm/key/rotate
   ↓
2. HSM → Generate New Key, Create Rotation Record (IN_PROGRESS)
   ↓
3. HSM → Identify Participants (all terminals/banks affected)
   ↓
4. Terminal/ATM → Request New Key
   ↓
5. HSM → Encrypt New Key under Current Key → Return to Terminal
   ↓
6. Terminal → Decrypt New Key, Install, Test
   ↓
7. Terminal → Confirm Installation to HSM
   ↓
8. HSM → Check if All Participants Confirmed
   ↓
9. HSM → Auto-Complete Rotation (revoke old key)
```

### Client-Side Implementation

#### Java: Terminal Key Rotation Handler

```java
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TerminalKeyRotationHandler {

    private final String hsmBaseUrl;
    private final String terminalId;
    private final HttpClient httpClient;

    public TerminalKeyRotationHandler(String hsmBaseUrl, String terminalId) {
        this.hsmBaseUrl = hsmBaseUrl;
        this.terminalId = terminalId;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Initiate scheduled key rotation (terminal-initiated workflow).
     * Use this when terminal needs to proactively rotate its own keys.
     * Returns encrypted key immediately in single API call.
     */
    public boolean initiateScheduledRotation(String keyType) throws Exception {
        System.out.println("Initiating scheduled rotation for " + keyType + "...");

        // Step 1: Request rotation and receive encrypted key immediately
        String url = hsmBaseUrl + "/api/hsm/terminal/" + terminalId + "/request-rotation";

        String requestBody = String.format("""
            {
              "keyType": "%s",
              "rotationType": "SCHEDULED",
              "description": "Monthly scheduled rotation",
              "gracePeriodHours": 24
            }
            """, keyType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to initiate rotation: " + response.body());
        }

        // Parse response (includes encryptedNewKey immediately)
        KeyRotationResponse rotationResponse = parseKeyRotationResponse(response.body());

        System.out.println("Rotation initiated: " + rotationResponse.getRotationId());
        System.out.println("Grace period ends: " + rotationResponse.getGracePeriodEndsAt());

        // Step 2: Decrypt new key using current master key
        byte[] newKeyData = decryptNewKey(
                rotationResponse.getEncryptedNewKey(),
                getCurrentMasterKey()
        );

        // Step 3: Verify new key checksum
        String computedChecksum = calculateKeyChecksum(newKeyData);
        if (!computedChecksum.equals(rotationResponse.getNewKeyChecksum())) {
            throw new SecurityException("New key checksum mismatch!");
        }

        // Step 4: Install new key (store securely)
        installNewKey(newKeyData, keyType);

        // Step 5: Test new key with HSM (optional but recommended)
        boolean testPassed = testNewKey(newKeyData);
        if (!testPassed) {
            throw new RuntimeException("New key test failed!");
        }

        // Step 6: Confirm successful installation to HSM
        confirmKeyInstallation(rotationResponse.getRotationId());

        System.out.println("Scheduled key rotation completed successfully!");
        return true;
    }

    /**
     * Check for pending key rotation and update keys if available.
     * Use this for admin-initiated rotations where HSM notifies terminals.
     * This should be called periodically (e.g., every hour) or on terminal startup.
     */
    public boolean checkAndUpdateKeys() throws Exception {
        System.out.println("Checking for pending key rotation...");

        // Step 1: Request new key from HSM
        TerminalKeyUpdateResponse response = requestNewKey();

        if (response == null) {
            System.out.println("No pending rotation found.");
            return false;
        }

        System.out.println("New key available for rotation: " + response.getRotationId());
        System.out.println("Key type: " + response.getKeyType());
        System.out.println("Grace period ends: " + response.getGracePeriodEndsAt());

        // Step 2: Decrypt new key using current master key
        byte[] newKeyData = decryptNewKey(
                response.getEncryptedNewKey(),
                getCurrentMasterKey() // Your current TPK/TSK master key
        );

        // Step 3: Verify new key checksum
        String computedChecksum = calculateKeyChecksum(newKeyData);
        if (!computedChecksum.equals(response.getNewKeyChecksum())) {
            throw new SecurityException("New key checksum mismatch!");
        }

        // Step 4: Install new key (store securely)
        installNewKey(newKeyData, response.getKeyType());

        // Step 5: Test new key with HSM (optional but recommended)
        boolean testPassed = testNewKey(newKeyData);
        if (!testPassed) {
            throw new RuntimeException("New key test failed!");
        }

        // Step 6: Confirm successful installation to HSM
        confirmKeyInstallation(response.getRotationId());

        System.out.println("Key rotation completed successfully!");
        return true;
    }

    /**
     * Request new key from HSM during rotation.
     */
    private TerminalKeyUpdateResponse requestNewKey() throws Exception {
        String url = hsmBaseUrl + "/api/hsm/terminal/" + terminalId + "/get-updated-key";

        String requestBody = String.format("""
            {
              "terminalId": "%s",
              "currentKeyChecksum": "%s"
            }
            """, terminalId, getCurrentKeyChecksum());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 400) {
            // No pending rotation or terminal not involved
            return null;
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to request new key: " + response.body());
        }

        // Parse JSON response
        return parseTerminalKeyUpdateResponse(response.body());
    }

    /**
     * Decrypt new key received from HSM.
     * HSM encrypts new key under current terminal key for secure delivery.
     */
    private byte[] decryptNewKey(String encryptedKeyHex, byte[] currentMasterKey) throws Exception {
        // Derive operational key from current master key
        // CRITICAL: Use same derivation context as HSM
        String context = "KEY_DELIVERY:ROTATION";
        byte[] decryptionKey = deriveOperationalKey(currentMasterKey, context, 128);

        // Convert hex to bytes
        byte[] encryptedWithIv = hexToBytes(encryptedKeyHex);

        // Extract IV (first 16 bytes) and ciphertext
        byte[] iv = Arrays.copyOfRange(encryptedWithIv, 0, 16);
        byte[] ciphertext = Arrays.copyOfRange(encryptedWithIv, 16, encryptedWithIv.length);

        // Decrypt using AES-128-CBC
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(decryptionKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(ciphertext);
    }

    /**
     * Install new key in secure storage.
     * In real ATM: Store in tamper-resistant hardware security module.
     */
    private void installNewKey(byte[] newKeyData, String keyType) {
        // Store new key securely
        // Example: Write to encrypted key storage
        SecureKeyStorage.store(keyType, newKeyData);
        System.out.println("New " + keyType + " installed successfully");
    }

    /**
     * Test new key with HSM before confirming.
     * Example: Encrypt a test PIN block and verify with HSM.
     */
    private boolean testNewKey(byte[] newKeyData) throws Exception {
        // Derive operational key from new master key
        String bankUuid = "48a9e84c-ff57-4483-bf83-b255f34a6466";
        String context = "TPK:" + bankUuid + ":PIN";
        byte[] newOperationalKey = deriveOperationalKey(newKeyData, context, 128);

        // Test encryption with new key
        // ... perform test PIN encryption ...

        return true; // Test passed
    }

    /**
     * Confirm successful key installation to HSM.
     */
    private void confirmKeyInstallation(String rotationId) throws Exception {
        String url = hsmBaseUrl + "/api/hsm/terminal/" + terminalId + "/confirm-key-update";

        String requestBody = String.format("""
            {
              "rotationId": "%s",
              "confirmedBy": "TERMINAL_APP_v3.2"
            }
            """, rotationId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to confirm key installation: " + response.body());
        }

        System.out.println("Key installation confirmed with HSM");
    }

    // Helper methods (implement these based on previous examples)
    private byte[] deriveOperationalKey(byte[] masterKey, String context, int outputBits) {
        // Use PBKDF2 derivation (see Client-Side Key Derivation section)
        // ...
    }

    private String getCurrentKeyChecksum() {
        // Return checksum of current active key
        // ...
    }

    private byte[] getCurrentMasterKey() {
        // Retrieve current master key from secure storage
        // ...
    }

    private String calculateKeyChecksum(byte[] keyData) {
        // Calculate SHA-256 checksum and return first 16 hex chars
        // ...
    }
}

// Usage in terminal application
public class TerminalApplication {
    public static void main(String[] args) {
        TerminalKeyRotationHandler rotationHandler = new TerminalKeyRotationHandler(
                "https://hsm.bank.com",
                "TRM-ISS001-ATM-001"
        );

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Option 1: Terminal-initiated scheduled rotation (monthly)
        // Use this when terminal proactively rotates its own keys
        scheduler.scheduleAtFixedRate(() -> {
            try {
                rotationHandler.initiateScheduledRotation("TPK");
                System.out.println("Scheduled TPK rotation completed");
            } catch (Exception e) {
                System.err.println("Scheduled rotation failed: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.DAYS);

        // Option 2: Check for admin-initiated rotation (hourly)
        // Use this to check if HSM administrator started a rotation
        scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean keysUpdated = rotationHandler.checkAndUpdateKeys();
                if (keysUpdated) {
                    System.out.println("Keys rotated successfully");
                }
            } catch (Exception e) {
                System.err.println("Key rotation check failed: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.HOURS);
    }
}
```

#### Python: Terminal Key Rotation Handler

```python
import hashlib
import requests
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
import schedule
import time

class TerminalKeyRotationHandler:
    def __init__(self, hsm_base_url: str, terminal_id: str):
        self.hsm_base_url = hsm_base_url
        self.terminal_id = terminal_id

    def initiate_scheduled_rotation(self, key_type: str) -> bool:
        """
        Initiate scheduled key rotation (terminal-initiated workflow).
        Use this when terminal needs to proactively rotate its own keys.
        Returns encrypted key immediately in single API call.
        """
        print(f"Initiating scheduled rotation for {key_type}...")

        # Step 1: Request rotation and receive encrypted key immediately
        url = f"{self.hsm_base_url}/api/hsm/terminal/{self.terminal_id}/request-rotation"

        payload = {
            "keyType": key_type,
            "rotationType": "SCHEDULED",
            "description": "Monthly scheduled rotation",
            "gracePeriodHours": 24
        }

        response = requests.post(url, json=payload)

        if response.status_code != 200:
            raise RuntimeError(f"Failed to initiate rotation: {response.text}")

        # Parse response (includes encryptedNewKey immediately)
        rotation_response = response.json()

        print(f"Rotation initiated: {rotation_response['rotationId']}")
        print(f"Grace period ends: {rotation_response['gracePeriodEndsAt']}")

        # Step 2: Decrypt new key using current master key
        new_key_data = self.decrypt_new_key(
            rotation_response['encryptedNewKey'],
            self.get_current_master_key()
        )

        # Step 3: Verify new key checksum
        computed_checksum = self.calculate_key_checksum(new_key_data)
        if computed_checksum != rotation_response['newKeyChecksum']:
            raise SecurityError("New key checksum mismatch!")

        # Step 4: Install new key (store securely)
        self.install_new_key(new_key_data, key_type)

        # Step 5: Test new key with HSM (optional but recommended)
        if not self.test_new_key(new_key_data):
            raise RuntimeError("New key test failed!")

        # Step 6: Confirm successful installation to HSM
        self.confirm_key_installation(rotation_response['rotationId'])

        print("Scheduled key rotation completed successfully!")
        return True

    def check_and_update_keys(self) -> bool:
        """
        Check for pending key rotation and update keys if available.
        Use this for admin-initiated rotations where HSM notifies terminals.
        Returns True if keys were rotated, False otherwise.
        """
        print("Checking for pending key rotation...")

        # Step 1: Request new key from HSM
        response = self.request_new_key()

        if response is None:
            print("No pending rotation found.")
            return False

        print(f"New key available for rotation: {response['rotationId']}")
        print(f"Key type: {response['keyType']}")
        print(f"Grace period ends: {response['gracePeriodEndsAt']}")

        # Step 2: Decrypt new key
        new_key_data = self.decrypt_new_key(
            response['encryptedNewKey'],
            self.get_current_master_key()
        )

        # Step 3: Verify checksum
        computed_checksum = self.calculate_key_checksum(new_key_data)
        if computed_checksum != response['newKeyChecksum']:
            raise SecurityError("New key checksum mismatch!")

        # Step 4: Install new key
        self.install_new_key(new_key_data, response['keyType'])

        # Step 5: Test new key
        if not self.test_new_key(new_key_data):
            raise RuntimeError("New key test failed!")

        # Step 6: Confirm installation
        self.confirm_key_installation(response['rotationId'])

        print("Key rotation completed successfully!")
        return True

    def request_new_key(self) -> dict:
        """Request new key from HSM during rotation."""
        url = f"{self.hsm_base_url}/api/hsm/terminal/{self.terminal_id}/get-updated-key"

        payload = {
            "terminalId": self.terminal_id,
            "currentKeyChecksum": self.get_current_key_checksum()
        }

        response = requests.post(url, json=payload)

        if response.status_code == 400:
            return None  # No pending rotation

        if response.status_code != 200:
            raise RuntimeError(f"Failed to request new key: {response.text}")

        return response.json()

    def decrypt_new_key(self, encrypted_key_hex: str, current_master_key: bytes) -> bytes:
        """Decrypt new key received from HSM."""
        # Derive operational key from current master key
        context = "KEY_DELIVERY:ROTATION"
        decryption_key = self.derive_operational_key(current_master_key, context, 128)

        # Convert hex to bytes
        encrypted_with_iv = bytes.fromhex(encrypted_key_hex)

        # Extract IV and ciphertext
        iv = encrypted_with_iv[:16]
        ciphertext = encrypted_with_iv[16:]

        # Decrypt using AES-128-CBC
        cipher = Cipher(
            algorithms.AES(decryption_key),
            modes.CBC(iv),
            backend=default_backend()
        )
        decryptor = cipher.decryptor()
        decrypted = decryptor.update(ciphertext) + decryptor.finalize()

        # Remove PKCS5 padding
        padding_length = decrypted[-1]
        return decrypted[:-padding_length]

    def install_new_key(self, new_key_data: bytes, key_type: str):
        """Install new key in secure storage."""
        # Store new key securely
        SecureKeyStorage.store(key_type, new_key_data)
        print(f"New {key_type} installed successfully")

    def test_new_key(self, new_key_data: bytes) -> bool:
        """Test new key with HSM before confirming."""
        # Derive operational key and test encryption
        # ...
        return True

    def confirm_key_installation(self, rotation_id: str):
        """Confirm successful key installation to HSM."""
        url = f"{self.hsm_base_url}/api/hsm/terminal/{self.terminal_id}/confirm-key-update"

        payload = {
            "rotationId": rotation_id,
            "confirmedBy": "TERMINAL_APP_v3.2"
        }

        response = requests.post(url, json=payload)

        if response.status_code != 200:
            raise RuntimeError(f"Failed to confirm installation: {response.text}")

        print("Key installation confirmed with HSM")

    def derive_operational_key(self, master_key: bytes, context: str, output_bits: int) -> bytes:
        """Derive operational key using PBKDF2 (see previous sections)."""
        # ... PBKDF2 implementation ...
        pass

    def get_current_master_key(self) -> bytes:
        """Retrieve current master key from secure storage."""
        pass

    def get_current_key_checksum(self) -> str:
        """Return checksum of current active key."""
        pass

    def calculate_key_checksum(self, key_data: bytes) -> str:
        """Calculate SHA-256 checksum, return first 16 hex chars."""
        hash_value = hashlib.sha256(key_data).hexdigest()
        return hash_value[:16].upper()

# Usage
def main():
    handler = TerminalKeyRotationHandler(
        "https://hsm.bank.com",
        "TRM-ISS001-ATM-001"
    )

    # Option 1: Terminal-initiated scheduled rotation (monthly)
    # Use this when terminal proactively rotates its own keys
    schedule.every(30).days.do(lambda: handler.initiate_scheduled_rotation("TPK"))

    # Option 2: Check for admin-initiated rotation (hourly)
    # Use this to check if HSM administrator started a rotation
    schedule.every(1).hours.do(handler.check_and_update_keys)

    while True:
        schedule.run_pending()
        time.sleep(60)

if __name__ == "__main__":
    main()
```

### Key Rotation Best Practices

**For Terminal/ATM Developers:**

1. **Periodic Checks**: Check for pending rotations hourly or on startup
2. **Graceful Handling**: Continue operations with old key during grace period
3. **Test Before Confirm**: Always test new key before confirming installation
4. **Secure Storage**: Store master keys in tamper-resistant hardware
5. **Checksum Verification**: Always verify key checksums after decryption
6. **Rollback Support**: Keep old key active until confirmation
7. **Logging**: Log all rotation events for audit trail

**Security Warnings:**

****DO NOT**:
- Store decrypted keys in plaintext memory for extended periods
- Skip checksum verification
- Confirm installation before testing new key
- Ignore rotation requests indefinitely

****DO**:
- Use derived operational keys (never use master keys directly)
- Verify checksums before and after decryption
- Test new keys thoroughly before confirming
- Implement automatic rotation checks
- Support rollback if new key fails

### Key Rotation Troubleshooting

#### Problem: PIN Decryption Fails After Key Rotation

**Symptoms:**
```
javax.crypto.BadPaddingException: Given final block not properly padded
```

**Root Cause Analysis:**

| Check | Command/Action | Expected | Common Issue |
|-------|---------------|----------|--------------|
| **1. Key has bank association** | Query `master_keys` table | `id_bank` is NOT NULL | Missing `idBank` in rotated key |
| **2. Key has terminal association** | Query `master_keys` table | `id_terminal` is NOT NULL | Missing `idTerminal` in rotated key |
| **3. Derivation context** | Check logs for "Deriving new PIN key" | Context uses bank UUID | Context uses "GLOBAL" or terminal ID |
| **4. Bank UUID matches** | Compare old vs new key | Same bank UUID | Different or NULL bank UUID |

**Diagnostic Query:**
```sql
-- Check key associations for a terminal
SELECT
    master_key_id,
    key_type,
    status,
    id_bank,
    id_terminal,
    kdf_salt,
    activated_at
FROM master_keys mk
LEFT JOIN terminals t ON mk.id_terminal = t.id
WHERE t.terminal_id = 'TRM-ISS001-ATM-001'
  AND mk.key_type IN ('TPK', 'TSK')
ORDER BY mk.activated_at DESC;
```

**Expected Output:**
```
master_key_id                | key_type | status | id_bank                              | id_terminal                          | kdf_salt
-----------------------------+----------+--------+--------------------------------------+--------------------------------------+------------------
TPK-TRMISS001ATM001-4B6E3217 | TPK      | ACTIVE | 48a9e84c-ff57-4483-bf83-b255f34a6466 | 7c123abc-...                        | TRM-ISS001-ATM-001
TPK-TRM-ISS001-ATM-001       | TPK      | ROTATED| 48a9e84c-ff57-4483-bf83-b255f34a6466 | 7c123abc-...                        | TRM-ISS001-ATM-001
```

**Fix:**
If `id_bank` or `id_terminal` is NULL:
1. This indicates a bug in the HSM key generation (fixed in v1.1.0+)
2. Update to latest HSM version
3. Re-run key rotation to generate properly associated keys

**Client-Side Fix:**
```java
// Before deriving operational key, verify bank UUID
MasterKey tpkKey = hsmClient.getKey(terminalId, "TPK");

if (tpkKey.getBankId() == null) {
    throw new IllegalStateException(
        "TPK key missing bank association - HSM upgrade required");
}

// Use bank UUID from key metadata
String context = "TPK:" + tpkKey.getBankId() + ":PIN";
byte[] operationalKey = deriveOperationalKey(
    tpkKey.getKeyData(), context, 128);
```

#### Problem: Rotation ID Not Found

**Symptoms:**
```
IllegalArgumentException: Rotation not found: TRM-ISS001-ATM-001-TPK-v2
```

**Root Cause:**
Client sending incorrect rotation ID format (old vs new key ID instead of rotation record ID).

**Solution:**
Use the `rotationId` or `rotationIdString` from the rotation initiation response:

```java
// Step 1: Initiate rotation
RotationResponse response = hsmClient.initiateRotation(terminalId, "TPK");

// CORRECT: Use rotationId from response
UUID rotationId = response.getRotationId();
// OR
String rotationIdString = response.getRotationIdString(); // e.g., "ROT-TPK-93CAFC76"

// Step 2: Confirm with correct ID
hsmClient.confirmKeyUpdate(terminalId, rotationId);  // **Works

// WRONG: Constructing ID from key names
String wrongId = terminalId + "-TPK-v2";  // **Will fail
hsmClient.confirmKeyUpdate(terminalId, wrongId);
```

#### Problem: Derived Key Mismatch Between Client and HSM

**Symptoms:**
- Encryption on client side works
- Decryption on HSM side fails with BadPaddingException
- Or vice versa

**Diagnostic Steps:**

1. **Verify Context String:**
```java
// Enable debug logging
log.debug("Master key UUID: {}", masterKey.getId());
log.debug("Bank UUID: {}", masterKey.getBankId());
log.debug("Derivation context: TPK:{}:PIN", masterKey.getBankId());

// Should output:
// Master key UUID: f9c9a75c-8d02-40c0-a018-dcc55a45b99d
// Bank UUID: 48a9e84c-ff57-4483-bf83-b255f34a6466
// Derivation context: TPK:48a9e84c-ff57-4483-bf83-b255f34a6466:PIN
```

2. **Verify Iteration Count:**
```java
// Must be exactly 100,000
KeySpec spec = new PBEKeySpec(
    context.toCharArray(),
    masterKeyBytes,
    100000,  // **Correct
    outputBits
);
```

3. **Verify Key Size:**
```java
// PIN operations use 128-bit (16 bytes)
byte[] operationalKey = deriveKey(masterKey, context, 128);
assertEquals(16, operationalKey.length);  // **Correct
```

4. **Compare Derived Keys (for testing only):**
```java
// Client side
byte[] clientDerived = deriveOperationalKey(masterKeyBytes, context, 128);
log.debug("Client derived key (hex): {}", bytesToHex(clientDerived));

// HSM side (from logs)
// Look for: "Derived TPK PIN Key (first 8 bytes): 1A2B3C4D..."

// First 16 hex chars (8 bytes) should match
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

****DO:**
- Use Key Ceremony for production master key generation
- Rotate keys according to your security policy
- Store key IDs, not key material, in application databases
- Use unique contexts for key derivation
- Implement proper key lifecycle management (ACTIVE, SUSPENDED, REVOKED)

****DON'T:**
- Hardcode key material in your application
- Reuse the same key for multiple purposes
- Transmit unencrypted key material over the network
- Use sample/test keys in production
- Skip key rotation

### 2. PIN Security

****DO:**
- Always use PVV (PIN Verification Value) for PIN verification
- Use ISO-0 format for PAN-based PIN blocks
- Implement PIN retry limits (3-5 attempts)
- Use TLS for all PIN transmission
- Log PIN verification attempts

****DON'T:**
- Store clear PINs anywhere
- Log PIN values (encrypted or clear)
- Transmit PINs without encryption
- Implement custom PIN encryption (use HSM)
- Skip PAN validation

### 3. MAC Security

****DO:**
- Use AES-CMAC for new implementations
- Include transaction timestamp in MAC calculation
- Implement MAC verification on all critical operations
- Use separate MAC keys for different message types
- Log MAC verification failures

****DON'T:**
- Reuse MACs across different messages
- Skip MAC verification
- Use deprecated algorithms (DES, MD5)
- Truncate MACs unnecessarily
- Implement custom MAC algorithms

### 4. Network Security

****DO:**
- Always use TLS 1.2 or higher
- Implement mutual TLS for production
- Use certificate pinning where possible
- Implement rate limiting
- Monitor for unusual patterns

****DON'T:**
- Expose HSM API directly to the internet
- Skip certificate validation
- Use self-signed certificates in production
- Log sensitive data in plain text

### 5. Error Handling

****DO:**
- Return generic error messages to clients
- Log detailed errors server-side
- Implement retry logic with exponential backoff
- Validate all inputs before sending to HSM
- Handle timeout scenarios gracefully

****DON'T:**
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
