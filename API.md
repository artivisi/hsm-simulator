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

### 2. Verify PIN

Verifies a PIN against stored encrypted value.

**Endpoint**: `POST /api/hsm/pin/verify`

**Request Body**:
```json
{
  "accountNumber": "4111111111111111",
  "pin": "1234"
}
```

**Request Parameters**:
- `accountNumber` (string, required): Primary Account Number
- `pin` (string, required): Clear PIN to verify

**Response**:
```json
{
  "valid": true,
  "message": "PIN is valid"
}
```

**Response Fields**:
- `valid` (boolean): True if PIN matches, false otherwise
- `message` (string): Verification result message

**Example**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/verify \
  -H "Content-Type: application/json" \
  -u user:password \
  -d '{
    "accountNumber": "4111111111111111",
    "pin": "1234"
  }'
```

---

### 3. Generate PIN Block

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

### 4. Verify PIN with Translation (Method A)

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

### 5. Verify PIN with PVV (Method B) ⭐ RECOMMENDED

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
   - Calculates PVV from clear PIN + PAN using SHA-256
   - Compares calculated PVV with stored PVV

**PVV Calculation Method**:
```
Input: PIN (1234) + PAN (4111111111111111)
Process: SHA-256(PIN + PAN)
Extract: First 4 decimal digits from hash
Output: PVV (e.g., "1234")
```

**Why PVV is Preferred**:
- ✅ **More secure**: PVV is one-way, cannot be reversed to PIN
- ✅ **Smaller storage**: 4 digits vs 32+ character encrypted PIN block
- ✅ **Industry standard**: ISO 9564 compliant
- ✅ **Production ready**: Used by major banks worldwide

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

## MAC Operations

### 6. Generate MAC

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
- `algorithm` (string, optional): MAC algorithm, defaults to `ISO9797-ALG3`

**Supported Algorithms**:
- `ISO9797-ALG3` - Retail MAC (ISO 9797-1 Algorithm 3, compatible with ANSI X9.19)
- `HMAC-SHA256` - HMAC with SHA-256
- `CBC-MAC` - DES-based CBC-MAC

**Response**:
```json
{
  "macValue": "A1B2C3D4E5F6G7H8",
  "algorithm": "ISO9797-ALG3",
  "messageLength": 256
}
```

**Response Fields**:
- `macValue` (string): 16-character hexadecimal MAC value
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
    "algorithm": "ISO9797-ALG3"
  }'
```

---

### 7. Verify MAC

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
- `mac` (string, required): MAC value to verify (16 hex characters)
- `keyId` (string, required): UUID of MAC key (same key used for generation)
- `algorithm` (string, optional): MAC algorithm, defaults to `ISO9797-ALG3`

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
    "algorithm": "ISO9797-ALG3"
  }'
```

---

## Key Management

### 8. Generate Key

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

### 9. Exchange Key

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

## Key Types Reference

| Key Type | Description | Usage |
|----------|-------------|-------|
| **LMK** | Local Master Key | PIN storage encryption in HSM database |
| **TMK** | Terminal Master Key | Encrypts key distribution to terminals |
| **TPK** | Terminal PIN Key | Encrypts PIN blocks at terminal (child of TMK) |
| **TSK** | Terminal Security Key | MAC for terminal messages (child of TMK) |
| **ZMK** | Zone Master Key | Encrypts inter-bank key exchanges |
| **ZPK** | Zone PIN Key | Protects PIN data between banks (child of ZMK) |
| **ZSK** | Zone Session Key | Encrypts inter-bank messages (child of ZMK) |
| **ZAK** | Zone Authentication Key | Workshop alias for ZSK (MAC operations) |
| **TEK** | Traffic Encryption Key | Workshop alias for ZSK (message encryption) |

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

⚠️ **IMPORTANT**: This is an educational HSM simulator. In production HSM:

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
  -d '{"message":"Test message","keyId":"YOUR-KEY-ID","algorithm":"ISO9797-ALG3"}'

# Verify MAC
curl -X POST http://localhost:8080/api/hsm/mac/verify \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{"message":"Test message","mac":"YOUR-MAC-VALUE","keyId":"YOUR-KEY-ID","algorithm":"ISO9797-ALG3"}'
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
