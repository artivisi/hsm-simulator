# HSM Simulator REST API Documentation

## Overview

The HSM Simulator provides REST API endpoints for cryptographic operations including PIN management, MAC generation/verification, and key exchange. These endpoints are designed for integration with payment processing systems.

**Base URL**: `http://localhost:8080`

**Authentication**: Spring Security (Basic Auth or Form Login)

**Content-Type**: `application/json`

---

## PIN Operations

### 1. Encrypt PIN

Encrypts a provided cleartext PIN using specified format and encryption key. Returns both the encrypted PIN block and PVV for database storage.

**Use Case**: Card issuance - When issuing a new card, generate PIN block and PVV for storage.

**Endpoint**: `POST /api/hsm/pin/encrypt`

**Request Body**:
```json
{
  "pin": "1234",
  "accountNumber": "4111111111111111",
  "format": "ISO-0",
  "keyId": "uuid-of-encryption-key"
}
```

**Request Parameters**:
- `pin` (string, required): Cleartext PIN to encrypt (4-12 digits, numeric only)
- `accountNumber` (string, required): Primary Account Number (PAN) 12-19 digits
- `format` (string, required): PIN block format - `ISO-0`, `ISO-1`, `ISO-3`, or `ISO-4`
- `keyId` (string, required): UUID of encryption key (LMK, TPK, or ZPK)

**Response**:
```json
{
  "encryptedPinBlock": "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",
  "format": "ISO-0",
  "pvv": "1234"
}
```

**Response Fields**:
- `encryptedPinBlock` (string): Hex-encoded encrypted PIN block (for storage in database)
- `format` (string): PIN block format used
- `pvv` (string): **PIN Verification Value (4 digits)** - Store this in database for PVV-based verification

**What to Store in Database**:
- **For Method A (PIN Block Comparison)**: Store `encryptedPinBlock` (encrypted under LMK)
- **For Method B (PVV) ⭐ Recommended**: Store `pvv` (plaintext, 4 digits)

**PIN Formats**:
- **ISO-0** (ANSI X9.8): `0L[PIN][F...] XOR [0000][12 PAN digits]`
- **ISO-1**: `1L[PIN][Random padding]` (no PAN required)
- **ISO-3**: `3L[PIN][Random digits] XOR [0000][12 PAN digits]`
- **ISO-4**: `4L[PIN][Random hex] XOR [0000][12 PAN digits]`

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/encrypt \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pin": "1234",
    "accountNumber": "4111111111111111",
    "format": "ISO-0",
    "keyId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Success Response Example**:
```json
{
  "encryptedPinBlock": "8F4A2E1D9C7B5A3E6F8D2C4B7A9E5D3C",
  "format": "ISO-0",
  "pvv": "1234"
}
```

**Database Storage Example**:
```sql
-- For Method B (PVV) - Recommended
INSERT INTO accounts (pan, pvv) VALUES ('4111111111111111', '1234');

-- For Method A (PIN Block Comparison)
INSERT INTO generated_pins (account_number, encrypted_pin_block, encryption_key_id)
VALUES ('4111111111111111', '8F4A2E1D9C7B5A3E6F8D2C4B7A9E5D3C', 'key-uuid');
```

**Error Responses**:
```json
{
  "error": "PIN length must be between 4 and 12 digits"
}
```
```json
{
  "error": "PIN must contain only digits"
}
```
```json
{
  "error": "Invalid key type for PIN encryption. Use LMK (storage), TPK (terminal), or ZPK (zone)."
}
```

---

****WARNING**: IMPORTANT SECURITY NOTE**: Previous versions of this API included a `/api/hsm/pin/verify` endpoint that accepted cleartext PINs. This endpoint has been **removed** as it does not reflect real-world banking security practices. In production HSM systems, cleartext PINs are NEVER transmitted to or processed by the HSM. Always use the secure PIN verification methods below (Method A or Method B).

---

### 2. Generate PIN Block

Generates encrypted PIN block under LMK from plaintext PIN.

**Endpoint**: `POST /api/hsm/pin/generate-pinblock`

**Request Body**:
```json
{
  "pan": "4111111111111111",
  "pin": "1234",
  "format": "ISO-0"
}
```

**Request Parameters**:
- `pan` (string, required): Primary Account Number (PAN) 12-19 digits
- `pin` (string, required): Plaintext PIN (4-12 digits)
- `format` (string, optional): PIN block format - `ISO-0`, `ISO-1`, `ISO-3`, or `ISO-4`, defaults to `ISO-0`

**Response**:
```json
{
  "encryptedPinBlock": "A1B2C3D4E5F6789012345678901234AB",
  "format": "ISO-0",
  "pvv": "1234",
  "keyId": "LMK-SAMPLE-001",
  "keyType": "LMK"
}
```

**Response Fields**:
- `encryptedPinBlock` (string): PIN block encrypted under LMK (hex-encoded)
- `format` (string): PIN block format used
- `pvv` (string): PIN Verification Value (4 digits)
- `keyId` (string): LMK key identifier used
- `keyType` (string): Always "LMK"

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/generate-pinblock \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pan": "4111111111111111",
    "pin": "1234",
    "format": "ISO-0"
  }'
```

**Error Response**:
```json
{
  "error": "PIN length must be between 4 and 12 digits"
}
```

---

### 3. Verify PIN with Translation (Method A)

Verifies PIN by comparing encrypted PIN blocks from terminal and database. This method demonstrates the complete HSM flow where both encrypted PIN blocks are provided.

**Architecture**: Terminal → Core Bank App → HSM

**Endpoint**: `POST /api/hsm/pin/verify-with-translation`

**Request Body**:
```json
{
  "pinBlockUnderLMK": "A1B2C3D4E5F6789012345678901234AB",
  "pinBlockUnderTPK": "1234567890ABCDEF1234567890ABCDEF",
  "terminalId": "TRM-ISS001-ATM-001",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0"
}
```

**Request Parameters**:
- `pinBlockUnderLMK` (string, required): Encrypted PIN block from database (under LMK)
- `pinBlockUnderTPK` (string, required): Encrypted PIN block from terminal (under TPK)
- `terminalId` (string, required): Terminal identifier
- `pan` (string, required): Primary Account Number
- `pinFormat` (string, optional): PIN block format, defaults to `ISO-0`

**Flow**:
1. **Terminal → Core Bank**: Sends PIN block (TPK) + PAN
2. **Core Bank → Database**: Queries stored PIN block (LMK)
3. **Core Bank → HSM**: Sends both PIN blocks + PAN + Terminal ID
4. **HSM Processing**:
   - Decrypts PIN block under TPK → extracts PIN
   - Decrypts PIN block under LMK → extracts PIN
   - Compares both clear PINs

**Response**:
```json
{
  "valid": true,
  "message": "PIN verified successfully",
  "terminalId": "TRM-ISS001-ATM-001",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0",
  "lmkKeyId": "LMK-SAMPLE-001",
  "tpkKeyId": "TPK-SAMPLE-001"
}
```

**Response Fields**:
- `valid` (boolean): True if PINs match, false otherwise
- `message` (string): Verification result message
- `terminalId` (string): Echo of terminal identifier
- `pan` (string): Echo of PAN
- `pinFormat` (string): Echo of PIN format used
- `lmkKeyId` (string): LMK key identifier used
- `tpkKeyId` (string): TPK key identifier used

**Verbose Logging**: This method produces detailed step-by-step logs showing:
- Key retrieval (TPK, LMK)
- Decryption of both PIN blocks
- PIN extraction process
- Final comparison result

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/verify-with-translation \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pinBlockUnderLMK": "A1B2C3D4E5F6789012345678901234AB",
    "pinBlockUnderTPK": "1234567890ABCDEF1234567890ABCDEF",
    "terminalId": "TRM-ISS001-ATM-001",
    "pan": "4111111111111111",
    "pinFormat": "ISO-0"
  }'
```

---

### 4. Verify PIN with PVV (Method B) ⭐ RECOMMENDED

Verifies PIN using PVV (PIN Verification Value) method. This is the most common method in banking systems (ISO 9564 compliant) and offers better security than Method A.

**Architecture**: Terminal → Core Bank App → HSM

**Endpoint**: `POST /api/hsm/pin/verify-with-pvv`

**Request Body**:
```json
{
  "pinBlockUnderTPK": "1234567890ABCDEF1234567890ABCDEF",
  "storedPVV": "1234",
  "terminalId": "TRM-ISS001-ATM-001",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0"
}
```

**Request Parameters**:
- `pinBlockUnderTPK` (string, required): Encrypted PIN block from terminal (under TPK)
- `storedPVV` (string, required): Stored PVV from database (4 digits)
- `terminalId` (string, required): Terminal identifier
- `pan` (string, required): Primary Account Number
- `pinFormat` (string, optional): PIN block format, defaults to `ISO-0`

**Flow**:
1. **Terminal → Core Bank**: Sends PIN block (TPK) + PAN
2. **Core Bank → Database**: Queries stored PVV (4 digits)
3. **Core Bank → HSM**: Sends PIN block (TPK) + stored PVV + PAN
4. **HSM Processing**:
   - Decrypts PIN block under TPK → extracts clear PIN
   - Calculates PVV from clear PIN + PAN using Visa PVV algorithm (AES-128-ECB)
   - Compares calculated PVV with stored PVV

**PVV Calculation Method (Visa PVV, AES adaptation)**:
```
1. Build TSP: 11 rightmost PAN digits (excl. check digit) + PVKI("1") + PIN rightmost digit
2. Encrypt TSP (zero-padded to 16 bytes) with PVK using AES-128-ECB
3. Decimalize ciphertext hex: scan for digits 0-9, then map A-F → 0-5, collect 4 digits
Output: PVV (e.g., "0236")
```

**Why PVV is Preferred**:
- ****More secure**: PVV is one-way, cannot be reversed to PIN
- ****Smaller storage**: 4 digits vs 32+ character encrypted PIN block
- ****Industry standard**: ISO 9564 compliant
- ****Production ready**: Used by major banks worldwide

**Response**:
```json
{
  "valid": true,
  "message": "PIN verified successfully using PVV",
  "method": "PVV (PIN Verification Value)",
  "terminalId": "TRM-ISS001-ATM-001",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0",
  "tpkKeyId": "TPK-SAMPLE-001",
  "pvkKeyId": "LMK-SAMPLE-001",
  "storedPVV": "1234"
}
```

**Response Fields**:
- `valid` (boolean): True if PVV matches, false otherwise
- `message` (string): Verification result message
- `method` (string): Verification method used
- `terminalId` (string): Echo of terminal identifier
- `pan` (string): Echo of PAN
- `pinFormat` (string): Echo of PIN format used
- `tpkKeyId` (string): TPK key identifier for PIN decryption
- `pvkKeyId` (string): PVK key identifier for PVV calculation (uses LMK)
- `storedPVV` (string): Echo of stored PVV

**Verbose Logging**: This method produces detailed step-by-step logs showing:
- Key retrieval (TPK, PVK)
- PIN block decryption
- PIN extraction
- PVV calculation process (with algorithm details)
- Final comparison result

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/verify-with-pvv \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pinBlockUnderTPK": "1234567890ABCDEF1234567890ABCDEF",
    "storedPVV": "1234",
    "terminalId": "TRM-ISS001-ATM-001",
    "pan": "4111111111111111",
    "pinFormat": "ISO-0"
  }'
```

**Error Response**:
```json
{
  "error": "PIN verification with PVV failed: No active TPK key found"
}
```

---

## PIN Verification Methods Comparison

| Aspect | Method A: PIN Block Comparison | Method B: PVV ⭐ |
|--------|-------------------------------|-----------------|
| **Endpoint** | `/pin/verify-with-translation` | `/pin/verify-with-pvv` |
| **Storage** | Full encrypted PIN block (32+ chars) | 4-digit PVV |
| **Security** | Reversible with key | One-way function |
| **Industry Usage** | Educational/legacy systems | **Most common** (ISO 9564) |
| **Database Size** | Larger | **Smaller** |
| **HSM Operations** | 2 decryptions + compare | 1 decryption + PVV calc |
| **Compliance** | N/A | **ISO 9564** |
| **Best For** | Demonstration, teaching | **Production systems** |

**Recommendation**: Use **Method B (PVV)** for production systems. Method A is included for educational purposes to demonstrate alternative PIN verification approaches.

---

## Zone PIN Translation

### 5. Translate PIN Block: TPK → ZPK (Acquirer Side)

Translates PIN block from Terminal PIN Key (TPK) to Zone PIN Key (ZPK) for inter-bank transmission.

**Use Case**: Acquirer receives PIN encrypted under TPK from terminal, then re-encrypts under ZPK to forward to issuer for authorization.

**Endpoint**: `POST /api/hsm/pin/translate/tpk-to-zpk`

**Request Body**:
```json
{
  "pinBlockUnderTPK": "1A2B3C4D5E6F7890A1B2C3D4E5F67890",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0",
  "tpkKeyId": "uuid-of-tpk-key",
  "zpkKeyId": "uuid-of-zpk-key"
}
```

**Request Parameters**:
- `pinBlockUnderTPK` (string, required): PIN block encrypted under TPK (hex format, 32 characters)
- `pan` (string, required): Primary Account Number (10-19 digits)
- `pinFormat` (string, required): PIN block format - `ISO-0`, `ISO-1`, `ISO-3`, or `ISO-4`
- `tpkKeyId` (string, required): UUID of Terminal PIN Key (must be KeyType.TPK)
- `zpkKeyId` (string, required): UUID of Zone PIN Key (must be KeyType.ZPK)

**Response**:
```json
{
  "success": true,
  "message": "PIN block translated from TPK to ZPK successfully",
  "pinBlockUnderZPK": "9F8E7D6C5B4A39281F0E1D2C3B4A5968",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0",
  "tpkKeyId": "a1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890",
  "zpkKeyId": "f6e5d4c3-b2a1-9807-f6e5-d4c3b2a19807"
}
```

**Response Fields**:
- `success` (boolean): Operation success status
- `message` (string): Success message
- `pinBlockUnderZPK` (string): PIN block encrypted under ZPK (hex format, 32 characters)
- `pan` (string): Primary Account Number (echoed)
- `pinFormat` (string): PIN block format (echoed)
- `tpkKeyId` (string): TPK key UUID (echoed)
- `zpkKeyId` (string): ZPK key UUID (echoed)

**Transaction Flow**:
```
1. Terminal → Acquirer: PIN Block under TPK
2. Acquirer HSM: Decrypt TPK → Extract PIN → Re-encrypt ZPK
3. Acquirer → Issuer: PIN Block under ZPK (for authorization)
```

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/translate/tpk-to-zpk \
  -H "Content-Type: application/json" \
  -d '{
    "pinBlockUnderTPK": "1A2B3C4D5E6F7890A1B2C3D4E5F67890",
    "pan": "4111111111111111",
    "pinFormat": "ISO-0",
    "tpkKeyId": "a1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890",
    "zpkKeyId": "f6e5d4c3-b2a1-9807-f6e5-d4c3b2a19807"
  }'
```

**Verbose Logging**: This endpoint produces detailed step-by-step logs:
```
========================================
PIN TRANSLATION: TPK → ZPK (Acquirer)
========================================
PAN: 411111******1111
PIN Format: ISO-0
TPK Key: TPK-TRM-ACQ001-ATM-001-ABC123 (TPK)
ZPK Key: ZPK-ACQ001-SHARED-DEF456 (ZPK)
Step 1: Decrypting PIN block under TPK
Clear PIN block: 041234FFFFFFFFFF
Step 2: Extracting PIN from clear PIN block
Extracted PIN: 1***4
Step 3: Creating PIN block for ZPK encryption
New PIN block: 041234FFFFFFFFFF
Step 4: Encrypting PIN block under ZPK
Encrypted PIN block under ZPK: 9F8E7D6C5B4A39281F0E1D2C3B4A5968
========================================
TRANSLATION COMPLETE: TPK → ZPK
========================================
```

---

### 6. Translate PIN Block: ZPK → LMK (Issuer Side)

Translates PIN block from Zone PIN Key (ZPK) to Local Master Key (LMK) for verification.

**Use Case**: Issuer receives PIN encrypted under ZPK from acquirer, then re-encrypts under LMK to verify against stored PIN.

**Endpoint**: `POST /api/hsm/pin/translate/zpk-to-lmk`

**Request Body**:
```json
{
  "pinBlockUnderZPK": "9F8E7D6C5B4A39281F0E1D2C3B4A5968",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0",
  "zpkKeyId": "uuid-of-zpk-key",
  "lmkKeyId": "uuid-of-lmk-key"
}
```

**Request Parameters**:
- `pinBlockUnderZPK` (string, required): PIN block encrypted under ZPK (hex format, 32 characters)
- `pan` (string, required): Primary Account Number (10-19 digits)
- `pinFormat` (string, required): PIN block format - `ISO-0`, `ISO-1`, `ISO-3`, or `ISO-4`
- `zpkKeyId` (string, required): UUID of Zone PIN Key (must be KeyType.ZPK)
- `lmkKeyId` (string, required): UUID of Local Master Key (must be KeyType.LMK)

**Response**:
```json
{
  "success": true,
  "message": "PIN block translated from ZPK to LMK successfully",
  "pinBlockUnderLMK": "5A4B3C2D1E0F98765A4B3C2D1E0F9876",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0",
  "zpkKeyId": "f6e5d4c3-b2a1-9807-f6e5-d4c3b2a19807",
  "lmkKeyId": "1f2e3d4c-5b6a-7980-1f2e-3d4c5b6a7980"
}
```

**Response Fields**:
- `success` (boolean): Operation success status
- `message` (string): Success message
- `pinBlockUnderLMK` (string): PIN block encrypted under LMK (hex format, 32 characters)
- `pan` (string): Primary Account Number (echoed)
- `pinFormat` (string): PIN block format (echoed)
- `zpkKeyId` (string): ZPK key UUID (echoed)
- `lmkKeyId` (string): LMK key UUID (echoed)

**Transaction Flow**:
```
1. Acquirer → Issuer: PIN Block under ZPK
2. Issuer HSM: Decrypt ZPK → Extract PIN → Re-encrypt LMK
3. Issuer: Compare with stored PIN Block under LMK (verification)
```

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/translate/zpk-to-lmk \
  -H "Content-Type: application/json" \
  -d '{
    "pinBlockUnderZPK": "9F8E7D6C5B4A39281F0E1D2C3B4A5968",
    "pan": "4111111111111111",
    "pinFormat": "ISO-0",
    "zpkKeyId": "f6e5d4c3-b2a1-9807-f6e5-d4c3b2a19807",
    "lmkKeyId": "1f2e3d4c-5b6a-7980-1f2e-3d4c5b6a7980"
  }'
```

**Verbose Logging**: This endpoint produces detailed step-by-step logs:
```
========================================
PIN TRANSLATION: ZPK → LMK (Issuer)
========================================
PAN: 411111******1111
PIN Format: ISO-0
ZPK Key: ZPK-ISS001-GHI789 (ZPK)
LMK Key: LMK-ISS001-JKL012 (LMK)
Step 1: Decrypting PIN block under ZPK
Clear PIN block: 041234FFFFFFFFFF
Step 2: Extracting PIN from clear PIN block
Extracted PIN: 1***4
Step 3: Creating PIN block for LMK encryption
New PIN block: 041234FFFFFFFFFF
Step 4: Encrypting PIN block under LMK
Encrypted PIN block under LMK: 5A4B3C2D1E0F98765A4B3C2D1E0F9876
========================================
TRANSLATION COMPLETE: ZPK → LMK
========================================
```

**Complete Inter-Bank Transaction Example**:
```bash
# Scenario: Cardholder uses ISS001 card at ACQ001 ATM

# 1. Terminal encrypts PIN with TPK
# (Happens automatically at ATM)

# 2. Acquirer translates TPK → ZPK
curl -X POST http://acquirer-hsm:8080/api/hsm/pin/translate/tpk-to-zpk \
  -d '{
    "pinBlockUnderTPK": "1A2B3C...",
    "pan": "4111111111111111",
    "pinFormat": "ISO-0",
    "tpkKeyId": "tpk-acq001-atm-uuid",
    "zpkKeyId": "zpk-acq001-shared-uuid"
  }'
# Returns: {"pinBlockUnderZPK": "9F8E7D..."}

# 3. Acquirer forwards to Issuer (via network)
# (Application layer - not HSM)

# 4. Issuer translates ZPK → LMK
curl -X POST http://issuer-hsm:8080/api/hsm/pin/translate/zpk-to-lmk \
  -d '{
    "pinBlockUnderZPK": "9F8E7D...",
    "pan": "4111111111111111",
    "pinFormat": "ISO-0",
    "zpkKeyId": "zpk-iss001-uuid",
    "lmkKeyId": "lmk-iss001-uuid"
  }'
# Returns: {"pinBlockUnderLMK": "5A4B3C..."}

# 5. Issuer verifies against database
# (Compare with stored PIN block under LMK)
```

**Error Responses**:
```json
{
  "error": "Source key must be TPK, got: TMK"
}
```

```json
{
  "error": "Missing required parameters: pinBlockUnderTPK, pan, pinFormat, tpkKeyId, zpkKeyId"
}
```

---

## MAC Operations

### 7. Generate MAC

Generates Message Authentication Code for message integrity.

**Endpoint**: `POST /api/hsm/mac/generate`

**Request Body**:
```json
{
  "message": "ISO8583 message data or transaction payload",
  "keyId": "uuid-of-mac-key",
  "algorithm": "ISO9797-ALG3"
}
```

**Request Parameters**:
- `message` (string, required): Message to authenticate
- `keyId` (string, required): UUID of MAC key (TSK or ZSK)
- `algorithm` (string, optional): MAC algorithm, defaults to `AES-CMAC`

**Supported Algorithms**:
- `AES-CMAC` - AES-CMAC with 64-bit output (NIST SP 800-38B, banking standard)
- `AES-CMAC-128` - AES-CMAC with 128-bit output (full MAC)
- `AES-CMAC-256` - AES-CMAC with 256-bit output (double key size)
- `AES-CMAC-64` - AES-CMAC with 64-bit output (explicit, same as AES-CMAC)
- `HMAC-SHA256` - HMAC with SHA-256, 64-bit output (banking compatible)
- `HMAC-SHA256-FULL` - HMAC with SHA-256, 256-bit output (full hash)
- `HMAC-SHA256-64` - HMAC with SHA-256, 64-bit output (explicit)

**Response**:
```json
{
  "macValue": "A1B2C3D4E5F6G7H8",
  "algorithm": "ISO9797-ALG3",
  "messageLength": 256
}
```

**Response Fields**:
- `macValue` (string): Hexadecimal MAC value (16 chars for 64-bit, 32 chars for 128-bit, 64 chars for 256-bit)
- `algorithm` (string): Algorithm used
- `messageLength` (integer): Message length in bytes

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/mac/generate \
  -H "Content-Type: application/json" \
  -u user:password \
  -d '{
    "message": "0800822000000000000004000000000000000000001234567890123456",
    "keyId": "223e4567-e89b-12d3-a456-426614174000",
    "algorithm": "AES-CMAC"
  }'
```

---

### 8. Verify MAC

Verifies MAC authenticity for received message.

**Endpoint**: `POST /api/hsm/mac/verify`

**Request Body**:
```json
{
  "message": "ISO8583 message data or transaction payload",
  "mac": "A1B2C3D4E5F6G7H8",
  "keyId": "uuid-of-mac-key",
  "algorithm": "ISO9797-ALG3"
}
```

**Request Parameters**:
- `message` (string, required): Original message
- `mac` (string, required): MAC value to verify (16, 32, or 64 hex characters depending on algorithm)
- `keyId` (string, required): UUID of MAC key (same key used for generation)
- `algorithm` (string, optional): MAC algorithm, defaults to `AES-CMAC`

**Response**:
```json
{
  "valid": true,
  "message": "MAC is valid"
}
```

**Response Fields**:
- `valid` (boolean): True if MAC matches, false if tampered
- `message` (string): Verification result

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/mac/verify \
  -H "Content-Type: application/json" \
  -u user:password \
  -d '{
    "message": "0800822000000000000004000000000000000000001234567890123456",
    "mac": "A1B2C3D4E5F6G7H8",
    "keyId": "223e4567-e89b-12d3-a456-426614174000",
    "algorithm": "AES-CMAC"
  }'
```

---

## Key Management

### 9. Generate Key

Generates new cryptographic key (ZMK or TMK).

**Endpoint**: `POST /api/hsm/key/generate`

**Request Body**:
```json
{
  "keyType": "ZMK",
  "keySize": 256,
  "description": "Production Zone Master Key",
  "bankId": "uuid-of-bank"
}
```

**Request Parameters**:
- `keyType` (string, required): Key type - `ZMK`, `TMK`, `ZAK`, or `TEK`
  - `ZMK` - Zone Master Key (for inter-bank communication)
  - `TMK` - Terminal Master Key (for terminal communication)
  - `ZAK` - Zone Authentication Key (maps to ZSK internally)
  - `TEK` - Traffic Encryption Key (maps to ZSK internally)
- `keySize` (integer, optional): Key size in bits (128, 192, or 256), defaults to 256
- `description` (string, optional): Key description
- `bankId` (string, optional): UUID of bank, uses first active bank if not specified

**Note**: Only `ZMK` and `TMK` can be generated directly. Other key types (TPK, TSK, ZPK, ZSK) must be derived from parent keys via UI.

**Response**:
```json
{
  "keyId": "323e4567-e89b-12d3-a456-426614174000",
  "masterKeyId": "ZMK-BANK001-A1B2C3D4",
  "keyType": "ZMK",
  "keyChecksum": "ABCD1234EFGH5678",
  "keyFingerprint": "A1B2C3D4E5F6G7H8"
}
```

**Response Fields**:
- `keyId` (string): UUID of generated key
- `masterKeyId` (string): Human-readable key identifier
- `keyType` (string): Type of key generated
- `keyChecksum` (string): MD5 checksum for key verification
- `keyFingerprint` (string): First 16 chars of SHA-256 fingerprint

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/key/generate \
  -H "Content-Type: application/json" \
  -u user:password \
  -d '{
    "keyType": "ZMK",
    "keySize": 256,
    "description": "Workshop ZMK"
  }'
```

---

### 10. Initialize Complete Key Set ⚡ NEW

Initialize complete key hierarchy for all banks. This endpoint clears existing sample keys and creates a comprehensive key set including HSM Master Key, LMK, TMK, TPK, TSK, ZMK, ZPK, and ZSK for all banks and terminals.

**Use Case**: Quickly set up multiple HSM instances for testing zone translation between acquirer and issuer banks.

**Endpoint**: `POST /api/hsm/keys/initialize`

**Request Body** (optional):
```json
{
  "bankCode": "ISS001",
  "clearExisting": true,
  "keySize": 256
}
```

**Request Parameters**:
- `bankCode` (string, optional): Specific bank code to initialize keys for (e.g., "ISS001", "ACQ001"). If omitted, initializes keys for ALL banks in the database.
- `shareZoneKeysWith` (string, optional): Bank code to copy zone keys from (enables shared HSM mode). Only valid when `bankCode` is specified.
- `clearExisting` (boolean, optional): Clear existing keys before initialization, defaults to `true`
  - If `bankCode` specified: Deletes only that bank's keys (LMK, TMK, ZMK, ZPK, ZSK, TPK, TSK).
  - If `bankCode` omitted: Deletes ALL keys in database.
- `keySize` (integer, optional): Key size in bits (128, 192, or 256), defaults to `256`

**Key Clearing Behavior**:

| Scenario | Keys Cleared | Bank LMKs | Use Case |
|----------|--------------|-----------|----------|
| `bankCode="ISS001"`, `clearExisting=true` | Only ISS001's keys (LMK, TMK, ZMK, ZPK, ZSK, TPK, TSK) | ISS001's LMK deleted & regenerated | Regenerate single bank's keys |
| `bankCode` omitted, `clearExisting=true` | ALL keys in database | ALL LMKs deleted & regenerated | Complete reset |
| `clearExisting=false` | None | N/A | Add keys without clearing |

****WARNING**: Important**: Each bank gets its **own LMK** (Local Master Key) for PIN storage isolation. This means:
- ISS001's PIN encrypted under `LMK-ISS001-ABC123` produces different ciphertext than ACQ001's same PIN under `LMK-ACQ001-DEF456`
- Banks cannot decrypt each other's stored PINs (complete isolation)
- Realistic simulation of multi-tenant HSM environments

---

## Two Setup Approaches

### Comparison Table

| Feature | Shared HSM (⭐ Workshops) | Multi-HSM (Production Sim) |
|---------|---------------------------|----------------------------|
| **Instances** | 1 HSM instance | 2+ HSM instances |
| **Database** | Single database | Separate databases |
| **Zone Key Setup** | Automatic (via `shareZoneKeysWith`) | Manual SQL copy |
| **Resources** | Low (1 process) | High (2+ processes) |
| **Complexity** | Simple | Complex |
| **Realism** | Medium | High |
| **Workshop Ready** | **Yes | **No (too complex) |
| **Use Case** | Training, demos, testing | Production simulation |

---

### Approach 1: Shared HSM (⭐ Recommended for Workshops)

**Use Case**: Single HSM instance serving both issuer and acquirer banks with automatic zone key sharing.

**Advantages**:
- **Single application instance (minimal resources)
- **Automatic zone key sharing (no manual copy needed)
- **Same database
- **Easier workshop setup

**How It Works**:
1. Both banks exist in the same database
2. Initialize issuer first (creates **LMK-ISS001**, TMK, ZMK, TPK, TSK, ZPK, ZSK)
3. Initialize acquirer with `shareZoneKeysWith` parameter
4. Acquirer gets:
   - **Own LMK-ACQ001** (different from ISS001)
   - Own TMK, TPK, TSK
   - **Shared zone keys** (ZMK, ZPK, ZSK with same key material as ISS001)

**Example Workflow**:
```bash
# 1. Start single HSM instance
mvn spring-boot:run

# 2. Initialize Issuer bank (creates new zone keys)
curl -X POST http://localhost:8080/api/hsm/keys/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "ISS001",
    "keySize": 256
  }'

# Response shows created keys including:
# - LMK-ISS001-ABC123  ← Bank-specific LMK for ISS001
# - TMK-ISS001-DEF456
# - ZMK-ISS001-GHI789  ← Zone keys for ISS001
# - ZPK-ISS001-JKL012
# - ZSK-ISS001-MNO345

# 3. Initialize Acquirer bank (shares zone keys with ISS001)
curl -X POST http://localhost:8080/api/hsm/keys/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "ACQ001",
    "shareZoneKeysWith": "ISS001"
  }'

# Response shows created keys including:
# - LMK-ACQ001-PQR678  ← Bank-specific LMK for ACQ001 (different from ISS001!)
# - TMK-ACQ001-STU901  ← New TMK for ACQ001
# - ZMK-ACQ001-SHARED-VWX234  ← Copied from ISS001 (SAME key material)
# - ZPK-ACQ001-SHARED-YZA567  ← Copied from ISS001 (SAME key material)
# - ZSK-ACQ001-SHARED-BCD890  ← Copied from ISS001 (SAME key material)

# 4. Done! Both banks can now communicate
# - Issuer encrypts PIN with ZPK-ISS001-GHI789
# - Acquirer decrypts with ZPK-ACQ001-SHARED-VWX234
# - Works because both have identical key_data
```

**Key Points**:
- 🔑 **Each bank gets its own LMK**: `LMK-ISS001-ABC123` ≠ `LMK-ACQ001-PQR678`
  - Same PIN encrypted with different LMKs produces **different ciphertexts**
  - Banks cannot decrypt each other's stored PINs
- ****Zone keys are shared**: Different IDs but **identical key material** (same `key_data` bytes)
  - `ZPK-ISS001-JKL012` and `ZPK-ACQ001-SHARED-YZA567` can decrypt each other's data
- 🔄 **Regeneration**: Running initialize again with `clearExisting=true` regenerates that bank's keys with new random values
- **WARNING**: **Key sync**: To regenerate ISS001's keys, re-run step 2, then re-run step 3 to update ACQ001's shared zone keys

---

### Approach 2: Multi-HSM (Production Simulation)

**Use Case**: Separate HSM instances for each bank (simulates real production environment).

**Advantages**:
- **Realistic production simulation
- **True separation of concerns
- **Each bank has completely isolated HSM

**Disadvantages**:
- **Requires more resources (2 instances, 2 databases)
- **Manual zone key synchronization required
- **More complex setup

**Example Workflow**:
```bash
# 1. Start Issuer HSM (port 8080, database: hsm_issuer)
mvn spring-boot:run -Dserver.port=8080 -Dspring.datasource.url=jdbc:postgresql://localhost/hsm_issuer

# 2. Initialize Issuer keys
curl -X POST http://localhost:8080/api/hsm/keys/initialize \
  -H "Content-Type: application/json" \
  -d '{"bankCode": "ISS001", "keySize": 256}'

# 3. Export zone keys from Issuer database
psql -h localhost -U hsm_user hsm_issuer \
  -c "SELECT master_key_id, key_type, encode(key_data, 'hex') as key_hex
      FROM master_keys
      WHERE key_type IN ('ZMK', 'ZPK', 'ZSK')
        AND id_bank = (SELECT id FROM banks WHERE bank_code = 'ISS001');"

# 4. Start Acquirer HSM (port 8081, database: hsm_acquirer)
mvn spring-boot:run -Dserver.port=8081 -Dspring.datasource.url=jdbc:postgresql://localhost/hsm_acquirer

# 5. Initialize Acquirer keys
curl -X POST http://localhost:8081/api/hsm/keys/initialize \
  -H "Content-Type: application/json" \
  -d '{"bankCode": "ACQ001", "keySize": 256}'

# 6. Manually copy zone key material from Issuer to Acquirer database
# See "Multi-HSM Zone Key Exchange" section below for SQL examples
```

**Response**:
```json
{
  "success": true,
  "totalKeysCreated": 23,
  "banksConfigured": 4,
  "clearedKeys": [
    "LMK-SAMPLE-001",
    "TPK-SAMPLE-001"
  ],
  "createdKeys": [
    "HSM-MASTER-A1B2C3D4",
    "LMK-E5F6G7H8",
    "TMK-ISS001-I9J0K1L2",
    "TPK-TRM-ISS001-ATM-001-M3N4O5P6",
    "TSK-TRM-ISS001-ATM-001-Q7R8S9T0",
    "ZMK-ISS001-U1V2W3X4",
    "ZPK-ISS001-Y5Z6A7B8",
    "ZSK-ISS001-C9D0E1F2"
  ],
  "keysByBank": {
    "ISS001": [
      "TMK-ISS001-I9J0K1L2",
      "ZMK-ISS001-U1V2W3X4",
      "TPK-TRM-ISS001-ATM-001-M3N4O5P6",
      "TSK-TRM-ISS001-ATM-001-Q7R8S9T0",
      "ZPK-ISS001-Y5Z6A7B8",
      "ZSK-ISS001-C9D0E1F2"
    ],
    "ACQ001": [...],
    "SWT001": [...],
    "ISS002": [...]
  },
  "keyHierarchy": {
    "totalKeys": 23,
    "keysByType": {
      "HSM_MASTER_KEY": 1,
      "LMK": 1,
      "TMK": 4,
      "TPK": 5,
      "TSK": 5,
      "ZMK": 4,
      "ZPK": 4,
      "ZSK": 4
    },
    "parentChildRelations": [
      "TPK-TRM-ISS001-ATM-001-M3N4O5P6 → TMK-ISS001-I9J0K1L2",
      "TSK-TRM-ISS001-ATM-001-Q7R8S9T0 → TMK-ISS001-I9J0K1L2",
      "ZPK-ISS001-Y5Z6A7B8 → ZMK-ISS001-U1V2W3X4",
      "ZSK-ISS001-C9D0E1F2 → ZMK-ISS001-U1V2W3X4"
    ]
  }
}
```

**Response Fields**:
- `success` (boolean): Initialization success status
- `totalKeysCreated` (integer): Total number of keys created
- `banksConfigured` (integer): Number of banks configured with keys
- `clearedKeys` (array): List of sample keys that were deleted
- `createdKeys` (array): List of all newly created key IDs
- `keysByBank` (object): Keys grouped by bank code
- `keyHierarchy` (object): Summary of key hierarchy including counts by type and parent-child relationships

**Key Hierarchy Created**:
```
Per Bank (e.g., ISS001):
├── LMK-ISS001 (PIN storage - bank-specific)
├── TMK-ISS001 (Terminal Master Key)
│   ├── TPK (Terminal PIN Key) for each terminal
│   └── TSK (Terminal Security Key) for each terminal
└── ZMK-ISS001 (Zone Master Key)
    ├── ZPK-ISS001 (Zone PIN Key)
    └── ZSK-ISS001 (Zone Session Key)

Per Bank (e.g., ACQ001 with shareZoneKeysWith="ISS001"):
├── LMK-ACQ001 (PIN storage - bank-specific, different from ISS001)
├── TMK-ACQ001 (Terminal Master Key)
│   ├── TPK (Terminal PIN Key) for each terminal
│   └── TSK (Terminal Security Key) for each terminal
└── ZMK-ACQ001-SHARED (copied from ISS001, same key material)
    ├── ZPK-ACQ001-SHARED (copied from ISS001, same key material)
    └── ZSK-ACQ001-SHARED (copied from ISS001, same key material)
```

**Quick Examples**:
```bash
# Example 1: Shared HSM - Issuer (creates new zone keys)
curl -X POST http://localhost:8080/api/hsm/keys/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "ISS001",
    "keySize": 256
  }'

# Example 2: Shared HSM - Acquirer (shares zone keys with ISS001)
curl -X POST http://localhost:8080/api/hsm/keys/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "ACQ001",
    "shareZoneKeysWith": "ISS001"
  }'

# Example 3: Multi-HSM - Separate instance (creates independent keys)
curl -X POST http://localhost:8081/api/hsm/keys/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "ACQ001",
    "keySize": 256
  }'

# Example 4: Initialize ALL banks at once (testing only)
curl -X POST http://localhost:8080/api/hsm/keys/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "keySize": 256
  }'
```

**Verbose Logging**: This endpoint produces detailed step-by-step logs showing:
- **Step 1**: Keys cleared (with details on what was deleted)
  - Example: `Deleting key: LMK-ISS001-ABC123 (LMK)`
  - Example: `Deleting key: TMK-ISS001-DEF456 (TMK)`
  - Example: `Cleared 7 keys for bank ISS001` (includes LMK)
- **Step 2**: Bank-specific key creation
  - Example: `Processing bank: Bank Issuer (ISS001)`
  - Example: `Created LMK for ISS001: LMK-ISS001-GHI789` ← Bank-specific LMK
  - Example: `Created: TMK-ISS001-JKL012`
  - Example: `Copying ZMK from source bank` (when using `shareZoneKeysWith`)
  - Example: `Copied zone key ZMK-ACQ001-SHARED-MNO345 from ZMK-ISS001-PQR678 with same key material`
- **Summary**: Final counts and hierarchy
  - Example: `Total Keys Created: 15`
  - Example: `Banks Configured: 2`

**Error Response**:
```json
{
  "error": "Key initialization failed: Bank not found"
}
```

---

### 11. Exchange Key

Exchanges cryptographic key between encryption domains.

**Endpoint**: `POST /api/hsm/key/exchange`

**Request Body**:
```json
{
  "sourceKeyId": "uuid-of-source-zmk",
  "targetKeyId": "uuid-of-target-zmk",
  "keyType": "ZPK",
  "keyData": "optional-encrypted-key-to-translate"
}
```

**Request Parameters**:
- `sourceKeyId` (string, required): UUID of source ZMK (decryption key)
- `targetKeyId` (string, required): UUID of target ZMK (encryption key)
- `keyType` (string, required): Type of key being exchanged - `ZMK`, `ZPK`, `ZAK`, or `TEK`
- `keyData` (string, optional): Encrypted key to translate (if not provided, generates new session key)

**Response**:
```json
{
  "encryptedKey": "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",
  "keyCheckValue": "ABC123",
  "keyType": "ZPK"
}
```

**Response Fields**:
- `encryptedKey` (string): Session key encrypted under target ZMK (32 hex characters)
- `keyCheckValue` (string): KCV for verification (6 hex characters)
- `keyType` (string): Type of key exchanged

**Key Check Value (KCV)**:
KCV is calculated by encrypting 16 zero bytes with the session key and taking first 6 hex characters.

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/key/exchange \
  -H "Content-Type: application/json" \
  -u user:password \
  -d '{
    "sourceKeyId": "423e4567-e89b-12d3-a456-426614174000",
    "targetKeyId": "523e4567-e89b-12d3-a456-426614174000",
    "keyType": "ZPK"
  }'
```

---

### 12. Key Rotation Operations

The HSM Simulator provides comprehensive key rotation functionality with pending state tracking. This allows terminals and banks to update their keys in a controlled manner without service interruption.

#### 12.1 Terminal-Initiated Key Rotation (Auto-Approved)

Terminal requests rotation of its own keys as part of scheduled maintenance. This is **automatically approved** and immediately starts the rotation process.

**Use Case**: Terminals running scheduled rotation tasks (e.g., monthly key rotation) can initiate their own key rotation without administrator intervention.

**Endpoint**: `POST /api/hsm/terminal/{terminalId}/request-rotation`

**Request Body**:
```json
{
  "terminalId": "TRM-ISS001-ATM-001",
  "keyType": "TPK",
  "rotationType": "SCHEDULED",
  "description": "Monthly scheduled rotation",
  "gracePeriodHours": 24
}
```

**Request Parameters**:
- `terminalId` (path, required): Terminal identifier
- `keyType` (body, required): Type of key to rotate - `TPK` or `TSK`
- `rotationType` (body, optional): Type of rotation (default: `SCHEDULED`)
- `description` (body, optional): Description of rotation reason
- `gracePeriodHours` (body, optional): Hours before old key is revoked (default: 24)

**Response** (includes encrypted new key immediately):
```json
{
  "rotationId": "550e8400-e29b-41d4-a716-446655440000",
  "rotationIdString": "ROT-TPK-ABC12345",
  "oldKeyId": "TPK-TRM-ISS001-ATM-001-OLD",
  "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
  "rotationType": "SCHEDULED",
  "rotationStatus": "IN_PROGRESS",
  "rotationStartedAt": "2025-10-31T10:00:00",
  "totalParticipants": 1,
  "pendingParticipants": 0,
  "confirmedParticipants": 0,
  "failedParticipants": 0,
  "message": "Terminal-initiated rotation started. New key delivered immediately. Please install and confirm.",
  "encryptedNewKey": "1A2B3C4D5E6F7890ABCDEF0123456789...",
  "newKeyChecksum": "A8FC6D4EEB35",
  "keyType": "TPK",
  "gracePeriodEndsAt": "2025-11-01T10:00:00"
}
```

**Key Delivery**: For terminal-initiated rotation, the encrypted new key is **delivered immediately in the response** to streamline the workflow. The key is encrypted under the current terminal key using AES-128-CBC with derived operational key (derivation context: `KEY_DELIVERY:ROTATION`).

**Streamlined Terminal Workflow** (2 steps):
```bash
# 1. Terminal initiates rotation and receives encrypted key immediately
curl -X POST http://localhost:8080/api/hsm/terminal/TRM-ISS001-ATM-001/request-rotation \
  -H "Content-Type: application/json" \
  -d '{
    "keyType": "TPK",
    "rotationType": "SCHEDULED",
    "description": "Monthly scheduled rotation"
  }'

# Response includes rotationId AND encryptedNewKey
# {
#   "rotationId": "550e8400-...",
#   "encryptedNewKey": "1A2B3C4D5E6F...",
#   "newKeyChecksum": "A8FC6D4EEB35",
#   ...
# }

# 2. Terminal decrypts, installs new key, and confirms
curl -X POST http://localhost:8080/api/hsm/terminal/TRM-ISS001-ATM-001/confirm-key-update \
  -H "Content-Type: application/json" \
  -d '{
    "rotationId": "550e8400-...",
    "confirmedBy": "TERMINAL_SCHEDULED_TASK"
  }'

# Rotation auto-completes, old key is ROTATED
```

**Security Notes**:
- Terminal can only rotate its own keys (TPK/TSK)
- Cannot rotate bank-wide keys (TMK/ZMK)
- Rotation is immediately approved (no admin intervention)
- Old key remains active during grace period
- Ideal for automated scheduled maintenance tasks

**Implementation Notes**:

**WARNING**: **CRITICAL**: New rotated keys automatically inherit bank and terminal associations from the old key:

```
Old Key Attributes:
  - masterKeyId: "TPK-TRM-ISS001-ATM-001"
  - idBank: 48a9e84c-ff57-4483-bf83-b255f34a6466 ✓
  - idTerminal: 7c123abc-4567-... ✓
  - kdfSalt: "TRM-ISS001-ATM-001" ✓
  - status: ROTATED (after rotation completes)

New Key Attributes (automatically set):
  - masterKeyId: "TPK-TRMISS001ATM001-4B6E3217" (new ID)
  - idBank: 48a9e84c-ff57-4483-bf83-b255f34a6466 ✓ (COPIED from old key)
  - idTerminal: 7c123abc-4567-... ✓ (COPIED from old key)
  - kdfSalt: "TRM-ISS001-ATM-001" ✓ (same terminal ID)
  - status: ACTIVE
```

**Client Derivation Context (unchanged after rotation)**:
```java
// BEFORE rotation
String context = "TPK:48a9e84c-ff57-4483-bf83-b255f34a6466:PIN";
byte[] oldOperationalKey = deriveKey(oldMasterKey, context, 128);

// AFTER rotation - SAME CONTEXT STRING, different master key
String context = "TPK:48a9e84c-ff57-4483-bf83-b255f34a6466:PIN";
byte[] newOperationalKey = deriveKey(newMasterKey, context, 128);

// Result: Different operational keys, but same bank UUID in context
// Client code doesn't need to change - just use the new master key
```

**Why Bank UUID is Used (not Terminal ID)**:

The derivation context uses **bank UUID** rather than terminal ID to enable:
1. **Consistent context across all terminals in same bank**
2. **Simpler key management** (one bank UUID, many terminals)
3. **Operational key uniqueness** comes from different master keys per terminal

Common mistake:
```java
// **WRONG: Using terminal ID in context
String wrongContext = "TPK:" + terminalId + ":PIN";  // "TPK:TRM-ISS001-ATM-001:PIN"
// Will cause BadPaddingException!

// **CORRECT: Using bank UUID from key metadata
String correctContext = "TPK:" + masterKey.getBankId() + ":PIN";
```

**Rotation ID Formats**:

The API accepts both UUID and human-readable rotation IDs:
```bash
# Option 1: UUID format (recommended)
curl ... -d '{"rotationId": "550e8400-e29b-41d4-a716-446655440000"}'

# Option 2: Human-readable format (also works)
curl ... -d '{"rotationId": "ROT-TPK-ABC12345"}'
```

Always use `rotationId` or `rotationIdString` from the rotation response - never construct rotation IDs manually.

#### 12.2 Admin-Initiated Key Rotation

Administrator starts rotation process for any master key. Creates new key and tracks all participants that need to update.

**Endpoint**: `POST /api/hsm/key/rotate`

**Request Body**:
```json
{
  "keyId": "uuid-of-key-to-rotate",
  "rotationType": "SCHEDULED",
  "reason": "Regular 3-month rotation schedule",
  "gracePeriodHours": 24,
  "autoComplete": true
}
```

**Request Parameters**:
- `keyId` (string, required): UUID of key to rotate (must be ACTIVE)
- `rotationType` (string, required): Type of rotation - `SCHEDULED`, `EMERGENCY`, `COMPLIANCE`, `COMPROMISE`, or `EXPIRATION`
- `reason` (string, required): Reason for rotation (audit trail)
- `gracePeriodHours` (integer, optional): Hours before old key is revoked (default: 24)
- `autoComplete` (boolean, optional): Auto-complete when all participants confirm (default: true)

**Response**:
```json
{
  "rotationId": "550e8400-e29b-41d4-a716-446655440000",
  "rotationIdString": "ROT-TMK-ABC12345",
  "oldKeyId": "TMK-ISS001-OLD",
  "newKeyId": "TMK-ISS001-NEW",
  "rotationType": "SCHEDULED",
  "rotationStatus": "IN_PROGRESS",
  "rotationStartedAt": "2025-10-31T10:00:00",
  "totalParticipants": 5,
  "pendingParticipants": 5,
  "confirmedParticipants": 0,
  "failedParticipants": 0,
  "message": "Rotation in progress. 5 of 5 participants pending."
}
```

**Rotation Status Values**:
- `IN_PROGRESS`: Rotation ongoing, participants updating keys
- `COMPLETED`: All participants confirmed, old key revoked
- `FAILED`: Rotation failed
- `ROLLED_BACK`: Rotation cancelled, old key active
- `CANCELLED`: Rotation cancelled by admin

#### 12.3 Terminal Requests Updated Key

ATM/Terminal requests new key during rotation. Returns encrypted key for secure delivery.

**Endpoint**: `POST /api/hsm/terminal/{terminalId}/get-updated-key`

**Request Body**:
```json
{
  "terminalId": "TRM-ISS001-ATM-001",
  "rotationId": "uuid-optional",
  "currentKeyChecksum": "A8FC6D4EEB350415"
}
```

**Request Parameters**:
- `terminalId` (path, required): Terminal identifier
- `rotationId` (body, optional): Specific rotation UUID (if not provided, returns latest pending)
- `currentKeyChecksum` (body, optional): Current key checksum for verification

**Response**:
```json
{
  "rotationId": "550e8400-e29b-41d4-a716-446655440000",
  "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
  "keyType": "TPK",
  "encryptedNewKey": "A1B2C3D4...F0E1D2C3",
  "newKeyChecksum": "B7A53E2FC461526",
  "gracePeriodEndsAt": "2025-11-01T10:00:00",
  "message": "New key delivered successfully. Please install and confirm."
}
```

**Response Fields**:
- `encryptedNewKey` (string): New key encrypted under current terminal key (hex with IV prepended)
- `newKeyChecksum` (string): Checksum of new key for verification after installation
- `gracePeriodEndsAt` (string): When old key will be revoked

**Key Encryption for Delivery**:
- New key is encrypted using AES-128-CBC with current terminal key
- Derivation context: `"KEY_DELIVERY:ROTATION"`
- Random IV prepended to encrypted data
- Terminal must decrypt using operational key derived from current master key

#### 12.4 Terminal Confirms Key Installation

Terminal confirms successful installation of new key.

**Endpoint**: `POST /api/hsm/terminal/{terminalId}/confirm-key-update`

**Request Body**:
```json
{
  "rotationId": "550e8400-e29b-41d4-a716-446655440000",
  "confirmedBy": "TERMINAL_APP_v2.1"
}
```

**Request Parameters**:
- `terminalId` (path, required): Terminal identifier
- `rotationId` (body, required): Rotation UUID
- `confirmedBy` (body, optional): Confirmation source (default: terminalId)

**Response**:
```json
{
  "rotationId": "550e8400-e29b-41d4-a716-446655440000",
  "rotationIdString": "ROT-TPK-ABC12345",
  "oldKeyId": "TPK-TRM-ISS001-ATM-001-OLD",
  "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
  "rotationType": "SCHEDULED",
  "rotationStatus": "COMPLETED",
  "totalParticipants": 1,
  "pendingParticipants": 0,
  "confirmedParticipants": 1,
  "failedParticipants": 0,
  "message": "All participants confirmed. Rotation ready to complete."
}
```

**Auto-Completion**:
If `autoComplete` was set to `true` and all participants have confirmed, the rotation will automatically complete and the old key will be marked as `ROTATED`.

#### 12.5 Get Rotation Status

Check current status of rotation with participant details.

**Endpoint**: `GET /api/hsm/rotation/{rotationId}/status`

**Response**:
```json
{
  "rotationId": "550e8400-e29b-41d4-a716-446655440000",
  "rotationIdString": "ROT-TMK-ABC12345",
  "oldKeyId": "TMK-ISS001-OLD",
  "newKeyId": "TMK-ISS001-NEW",
  "rotationType": "SCHEDULED",
  "rotationStatus": "IN_PROGRESS",
  "rotationStartedAt": "2025-10-31T10:00:00",
  "totalParticipants": 5,
  "pendingParticipants": 2,
  "confirmedParticipants": 3,
  "failedParticipants": 0,
  "message": "Rotation in progress. 2 of 5 participants pending."
}
```

#### 12.6 Complete Rotation Manually

Manually complete rotation and revoke old key. Use after grace period or when all participants confirmed.

**Endpoint**: `POST /api/hsm/rotation/{rotationId}/complete`

**Request Body** (optional):
```json
{
  "completedBy": "admin"
}
```

**Response**:
```json
{
  "rotationId": "550e8400-e29b-41d4-a716-446655440000",
  "rotationIdString": "ROT-TPK-ABC12345",
  "oldKeyId": "TPK-TRM-ISS001-ATM-001-OLD",
  "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
  "rotationType": "SCHEDULED",
  "rotationStatus": "COMPLETED",
  "message": "Rotation completed successfully. Old key has been rotated."
}
```

**Effect**:
- Old key status changed to `ROTATED`
- Old key revocation timestamp recorded
- Rotation status changed to `COMPLETED`

#### 12.7 Rollback Rotation

Cancel rotation and revert to old key. New key is revoked.

**Endpoint**: `POST /api/hsm/rotation/{rotationId}/rollback`

**Request Body**:
```json
{
  "reason": "Terminal installation failed on 3 devices",
  "rolledBackBy": "admin"
}
```

**Request Parameters**:
- `reason` (string, required): Reason for rollback (audit trail)
- `rolledBackBy` (string, optional): Who initiated rollback

**Response**:
```json
{
  "rotationId": "550e8400-e29b-41d4-a716-446655440000",
  "rotationIdString": "ROT-TPK-ABC12345",
  "oldKeyId": "TPK-TRM-ISS001-ATM-001-OLD",
  "newKeyId": "TPK-TRM-ISS001-ATM-001-NEW",
  "rotationType": "EMERGENCY",
  "rotationStatus": "ROLLED_BACK",
  "message": "Rotation rolled back. Old key remains active, new key revoked."
}
```

**Effect**:
- New key status changed to `REVOKED`
- Old key remains `ACTIVE`
- Rotation status changed to `ROLLED_BACK`

### Key Rotation Workflow Example

**Scenario**: Rotating TPK for a single ATM terminal

```bash
# 1. Administrator initiates rotation
curl -X POST http://localhost:8080/api/hsm/key/rotate \
  -H "Content-Type: application/json" \
  -u admin:password \
  -d '{
    "keyId": "uuid-of-old-tpk",
    "rotationType": "SCHEDULED",
    "reason": "Regular 3-month rotation",
    "gracePeriodHours": 24
  }'

# Response: rotationId = "550e8400-..."

# 2. ATM terminal requests new key
curl -X POST http://localhost:8080/api/hsm/terminal/TRM-ISS001-ATM-001/get-updated-key \
  -H "Content-Type: application/json" \
  -d '{
    "rotationId": "550e8400-...",
    "currentKeyChecksum": "A8FC6D4E"
  }'

# Response: encryptedNewKey = "hex-encoded-encrypted-key"

# 3. Terminal installs new key and confirms
curl -X POST http://localhost:8080/api/hsm/terminal/TRM-ISS001-ATM-001/confirm-key-update \
  -H "Content-Type: application/json" \
  -d '{
    "rotationId": "550e8400-...",
    "confirmedBy": "ATM_SOFTWARE_v3.2"
  }'

# Response: rotationStatus = "COMPLETED" (auto-completed)
# Old key is now ROTATED, new key is ACTIVE
```

---

## Key Types Reference

### Keys Generated During Initialization

When you call `/api/hsm/keys/initialize`, the following keys are automatically created:

#### Root Keys (1 key)
- **LMK (Local Master Key)**: Root key for the HSM instance, used for PIN storage encryption

#### Per-Bank Keys (4 keys per bank)
For each bank in the system, the following keys are created:
- **TMK (Terminal Master Key)**: Master key for all terminals belonging to the bank
- **ZMK (Zone Master Key)**: Master key for inter-bank communications
- **ZPK (Zone PIN Key)**: Derived from ZMK, protects PIN data in inter-bank transactions
- **ZSK (Zone Session Key)**: Derived from ZMK, encrypts message data between banks

#### Per-Terminal Keys (2 keys per terminal)
For each terminal in the system, the following keys are created:
- **TPK (Terminal PIN Key)**: Derived from TMK, encrypts PIN blocks at the terminal
- **TSK (Terminal Security Key)**: Derived from TMK, generates MAC for terminal messages

### Complete Key Type Reference

| Key Type | Level | Parent | Description | Primary Usage | ISO 9797 Standard |
|----------|-------|--------|-------------|---------------|-------------------|
| **LMK** | Root | None | Local Master Key | Encrypts PINs stored in HSM database. All PINs at rest are encrypted under LMK. Used by core banking systems for PIN storage. | ISO 11568-2 |
| **TMK** | Bank | LMK | Terminal Master Key | Master key for a bank's terminal network. Used to distribute and protect TPK and TSK keys to terminals. Never leaves HSM. | ISO 11568-2 |
| **TPK** | Terminal | TMK | Terminal PIN Key | Encrypts PIN blocks at terminal level. Terminal encrypts cardholder PIN with TPK before transmission. Core bank decrypts with TPK to re-encrypt under LMK. | ISO 9564 |
| **TSK** | Terminal | TMK | Terminal Security Key | Generates MAC for messages between terminal and acquirer. Ensures message integrity and authenticity. Protects against tampering. | ISO 9797-1 |
| **ZMK** | Bank | LMK | Zone Master Key | Master key for inter-bank key exchange. Used to securely distribute ZPK and ZSK between different banks/zones. Never transmitted. | ISO 11568-4 |
| **ZPK** | Zone | ZMK | Zone PIN Key | Protects PIN blocks during inter-bank transactions. Issuer encrypts PIN under ZPK for transmission to acquirer. Used in ATM withdrawals at foreign banks. | ISO 9564 |
| **ZSK** | Zone | ZMK | Zone Session Key | Encrypts message data between banks and generates MAC for inter-bank messages. Used for ISO 8583 message protection. | ISO 9797-1 |
| **ZAK** | Zone | ZMK | Zone Auth Key | Alternative name for ZSK when used specifically for MAC operations in workshop scenarios. | ISO 9797-1 |
| **TEK** | Zone | ZMK | Traffic Encryption Key | Alternative name for ZSK when used specifically for message encryption in workshop scenarios. | ISO 8732 |
| **KEK** | Generic | Varies | Key Encryption Key | Generic key-encrypting-key for protecting other keys during storage or transmission. | ISO 11568-2 |

### Multi-HSM Zone Key Exchange

**Problem**: Each HSM instance generates its own random keys. For inter-bank communication, both HSMs need to share the same zone keys (ZMK, ZPK, ZSK).

**Why This Happens**:
- Issuer HSM generates: `ZMK-ISS001-ABC123` with random key material
- Acquirer HSM generates: `ZMK-ACQ001-XYZ789` with different random key material
- These are DIFFERENT keys and cannot decrypt each other's data

**Solution Options**:

**Option 1: Manual Database Copy (Simplest for Testing)**
```sql
-- 1. Export zone keys from Issuer HSM database
SELECT id, master_key_id, key_type, algorithm, key_size,
       encode(key_data, 'hex') as key_hex,
       key_fingerprint, key_checksum, id_bank
FROM master_keys
WHERE key_type IN ('ZMK', 'ZPK', 'ZSK')
  AND id_bank = (SELECT id FROM banks WHERE bank_code = 'ISS001');

-- 2. Insert into Acquirer HSM database
-- Replace the acquirer's zone keys with issuer's zone keys
-- Keep same key_data (hex decoded)
-- Update id_bank to point to acquirer's bank
INSERT INTO master_keys (
    master_key_id, key_type, algorithm, key_size,
    key_data, key_fingerprint, key_checksum,
    id_bank, generation_method, kdf_iterations, kdf_salt,
    status, activated_at
) VALUES (
    'ZMK-SHARED-ABC123',  -- New unique ID
    'ZMK',
    'AES',
    256,
    decode('COPY_HEX_FROM_ISSUER', 'hex'),  -- Same key material
    'COPY_FROM_ISSUER',
    'COPY_FROM_ISSUER',
    (SELECT id FROM banks WHERE bank_code = 'ACQ001'),
    'KEY_EXCHANGE',
    0,
    'N/A',
    'ACTIVE',
    NOW()
);
```

**Option 2: Export/Import API (Future Enhancement)**
```bash
# Export zone keys from Issuer HSM
curl -X GET http://issuer-hsm:8080/api/hsm/keys/export/zone \
  -o issuer-zone-keys.json

# Import to Acquirer HSM
curl -X POST http://acquirer-hsm:8081/api/hsm/keys/import/zone \
  -H "Content-Type: application/json" \
  -d @issuer-zone-keys.json
```

**Option 3: Secure Key Exchange Ceremony (Production Approach)**
- Use ZMK from one HSM to encrypt ZPK/ZSK
- Transmit encrypted keys with Key Check Value (KCV)
- Receiving HSM decrypts and verifies KCV
- This is the ISO 11568-4 standard approach

**Key Point**: For your multi-HSM simulator testing, **Option 1 (manual database copy)** is the fastest approach. Just ensure:
- Both HSMs have the **same** `key_data` value for zone keys
- Each HSM still has its **own** LMK, TMK, TPK, TSK (these should be different)

### Key Usage Scenarios

#### Scenario 1: Card Issuance (PIN Generation)
```
1. Bank generates random PIN
2. HSM encrypts PIN with LMK → Stores in database
3. PVV calculated from PIN for verification
4. PIN mailer printed for cardholder
```
**Keys Used**: LMK

#### Scenario 2: ATM PIN Entry (Own Bank)
```
Terminal (ATM)                    Core Banking              HSM
1. Cardholder enters PIN
2. Encrypt PIN with TPK ─────────>
3.                                Query DB for PIN under LMK
4.                                Decrypt TPK PIN ─────────> [Decrypt with TPK]
5.                                Decrypt LMK PIN ─────────> [Decrypt with LMK]
6.                                                           [Compare PINs]
7.                                PIN Verified <──────────── [Return result]
```
**Keys Used**: TPK (terminal to bank), LMK (stored PIN)

#### Scenario 3: ATM Withdrawal (Foreign Bank - Zone Translation)
```
Terminal (Foreign ATM)            Acquirer                  Issuer
1. Cardholder enters PIN
2. Encrypt PIN with TPK ────────>
3.                                Decrypt with TPK ───────> [Decrypt to clear PIN]
4.                                Re-encrypt with ZPK ────> [Encrypt with ZPK]
5.                                Send to issuer ─────────────────────────────────>
6.                                                          Decrypt ZPK PIN ──────> [Decrypt with ZPK]
7.                                                          Re-encrypt LMK PIN ───> [Encrypt with LMK]
8.                                                          Verify against DB
```
**Keys Used**: TPK (terminal), ZPK (inter-bank), LMK (verification)

#### Scenario 4: Inter-Bank Message Security
```
Bank A                            Network                   Bank B
1. Prepare ISO 8583 message
2. Generate MAC with ZSK ────────>
3.                                Forward message ────────────────────────────────>
4.                                                          Verify MAC with ZSK
5.                                                          Process if valid
```
**Keys Used**: ZSK (message authentication)

#### Scenario 5: Key Exchange Between Banks
```
Bank A HSM                        Secure Channel            Bank B HSM
1. Generate session ZPK
2. Encrypt ZPK with Bank B's ZMK ──────────────────────────>
3.                                                          Decrypt with own ZMK
4.                                                          Store ZPK for transactions
5. Both banks now share ZPK for PIN translation
```
**Keys Used**: ZMK (key encryption), ZPK (shared session key)

### Key Hierarchy Visualization

```
Bank ISS001:                                Bank ACQ001:
LMK-ISS001 (PIN Storage)                    LMK-ACQ001 (PIN Storage)
    │                                           │
    └─ (Protects ISS001 PINs)                   └─ (Protects ACQ001 PINs)

TMK-ISS001                                  TMK-ACQ001
    │                                           │
    ├── TPK (Terminal)                          ├── TPK (Terminal)
    └── TSK (Terminal)                          └── TSK (Terminal)

ZMK-ISS001                                  ZMK-ACQ001-SHARED ★
    │                                           │
    ├── ZPK-ISS001 ──────[same key]─────────── ZPK-ACQ001-SHARED ★
    │                                           │
    └── ZSK-ISS001 ──────[same key]─────────── ZSK-ACQ001-SHARED ★

★ = Shared zone keys with identical key material for inter-bank communication
```

**Key Isolation**:
- Each bank has its **own LMK** → Different PIN encryption
- Terminal keys (TMK/TPK/TSK) are **bank-specific** → Cannot be used across banks
- Zone keys (ZMK/ZPK/ZSK) are **shared** → Enable inter-bank transactions

### Key Rotation Strategy

Keys should be rotated according to security policies:

| Key Type | Rotation Frequency | Impact | Method |
|----------|-------------------|--------|---------|
| **LMK** | Every 2-3 years | High - All stored PINs | Re-encrypt all PINs with new LMK |
| **TMK** | Every 1-2 years | Medium - All terminals in bank | Update all terminal keys |
| **TPK/TSK** | Every 3-6 months | Low - Single terminal | Update individual terminal |
| **ZMK** | Every 1-2 years | High - All inter-bank traffic | Coordinate with partner banks |
| **ZPK/ZSK** | Every 1-3 months | Low - Session keys | Exchange new session keys |

### Security Considerations

1. **Key Material Protection**:
   - LMK: Never exported, stored in HSM only
   - TMK/ZMK: Exchanged using secure key ceremony
   - TPK/ZPK: Encrypted under parent key during distribution
   - TSK/ZSK: Symmetric keys shared between parties

2. **Key Component Loading**:
   - Critical keys (LMK, TMK, ZMK) loaded using 2-of-3 or 3-of-5 key custodian ceremony
   - Each custodian holds one share (Shamir's Secret Sharing)
   - Minimum threshold required to reconstruct key

3. **Dual Control**:
   - Key generation requires multiple authorized custodians
   - No single person can access complete key material
   - All key operations logged with custodian identities

4. **Key Verification**:
   - KCV (Key Check Value) calculated for each key
   - First 6 hex digits of encrypted zero block
   - Used to verify key was transmitted/received correctly

---

## Error Handling

All endpoints return appropriate HTTP status codes:

- **200 OK**: Successful operation
- **400 Bad Request**: Invalid parameters or validation errors
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found (key, PIN, MAC)
- **500 Internal Server Error**: Server-side processing error

**Error Response Format**:
```json
{
  "error": "Detailed error message"
}
```

---

## Security Considerations

### Production vs. Simulation

**WARNING**: **IMPORTANT**: This is an educational HSM simulator. In production HSM:

1. **Clear PINs**: Never stored or exposed (stored in simulator for educational purposes)
2. **Key Material**: Protected by hardware security boundary (simulated in software)
3. **Audit Logs**: Comprehensive tamper-proof logging required
4. **Access Control**: Strict role-based access with dual control
5. **Key Ceremony**: Multi-custodian initialization with Shamir's Secret Sharing

### Best Practices

- **Key Rotation**: Regularly rotate cryptographic keys
- **MAC Verification**: Always verify MAC before processing messages
- **PIN Verification**: Limit verification attempts (3 failures = block)
- **Secure Transport**: Use TLS for all API communications
- **Authentication**: Implement strong authentication mechanisms

---

## Workshop Integration

### Acquirer Service Integration

```java
// Generate PIN for card issuance
PinEncryptRequest request = new PinEncryptRequest();
request.setPin("1234");
request.setAccountNumber("4111111111111111");
request.setFormat("ISO-0");
request.setKeyId(lmkKeyId);

PinEncryptResponse response = restTemplate.postForObject(
    "http://hsm:8080/api/hsm/pin/encrypt",
    request,
    PinEncryptResponse.class
);
```

### Gateway Service Integration

```java
// Verify incoming message MAC
Map<String, String> request = Map.of(
    "message", iso8583Message,
    "mac", receivedMac,
    "keyId", zskKeyId,
    "algorithm", "ISO9797-ALG3"
);

Map<String, Object> response = restTemplate.postForObject(
    "http://hsm:8080/api/hsm/mac/verify",
    request,
    Map.class
);

boolean isValid = (Boolean) response.get("valid");
```

### Key Exchange Example

```java
// Exchange ZPK between banks
KeyExchangeRequest request = new KeyExchangeRequest();
request.setSourceKeyId(bank1ZmkId);
request.setTargetKeyId(bank2ZmkId);
request.setKeyType("ZPK");

KeyExchangeResponse response = restTemplate.postForObject(
    "http://hsm:8080/api/hsm/key/exchange",
    request,
    KeyExchangeResponse.class
);

String encryptedZpk = response.getEncryptedKey();
String kcv = response.getKeyCheckValue();
```

---

## Testing

### Using cURL

```bash
# Generate PIN Block under LMK
curl -X POST http://localhost:8080/api/hsm/pin/generate-pinblock \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{"pan":"4111111111111111","pin":"1234","format":"ISO-0"}'

# Verify PIN - Method A (PIN Block Comparison)
curl -X POST http://localhost:8080/api/hsm/pin/verify-with-translation \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{"pinBlockUnderLMK":"YOUR-LMK-PINBLOCK","pinBlockUnderTPK":"YOUR-TPK-PINBLOCK","terminalId":"TRM-001","pan":"4111111111111111","pinFormat":"ISO-0"}'

# Verify PIN - Method B (PVV) ⭐ Recommended
curl -X POST http://localhost:8080/api/hsm/pin/verify-with-pvv \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{"pinBlockUnderTPK":"YOUR-TPK-PINBLOCK","storedPVV":"1234","terminalId":"TRM-001","pan":"4111111111111111","pinFormat":"ISO-0"}'

# Encrypt PIN (Legacy)
curl -X POST http://localhost:8080/api/hsm/pin/encrypt \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{"pin":"1234","accountNumber":"4111111111111111","format":"ISO-0","keyId":"YOUR-KEY-ID"}'

# Generate MAC
curl -X POST http://localhost:8080/api/hsm/mac/generate \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{"message":"Test message","keyId":"YOUR-KEY-ID","algorithm":"AES-CMAC"}'

# Verify MAC
curl -X POST http://localhost:8080/api/hsm/mac/verify \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{"message":"Test message","mac":"YOUR-MAC-VALUE","keyId":"YOUR-KEY-ID","algorithm":"AES-CMAC"}'
```

### Using Postman

1. Import collection from provided JSON
2. Set base URL: `http://localhost:8080`
3. Configure Basic Auth with username/password
4. Test each endpoint with sample data

---

## Additional Resources

- **Web UI**: `http://localhost:8080` - Full-featured educational interface
- **Key Ceremony**: `/hsm/ceremony` - Multi-custodian key initialization
- **Key Hierarchy**: `/keys/hierarchy` - Visualize key relationships
- **PIN Management**: `/pins` - PIN generation and visualization
- **MAC Management**: `/macs` - MAC generation and verification

---

## Version Information

- **API Version**: 1.0
- **HSM Simulator Version**: 0.0.1-SNAPSHOT
- **Spring Boot**: 3.5.6
- **Java**: 21

---

## Support

For issues or questions:
- GitHub: https://github.com/artivisi/hsm-simulator
- Workshop: training-spring-jpos-2025

---

**Last Updated**: October 29, 2025
