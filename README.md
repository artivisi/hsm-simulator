# HSM Simulator

Educational Hardware Security Module (HSM) simulator built with Spring Boot 3.5.6, Java 21, PostgreSQL 17, and Tailwind CSS 4.1.

⚠️ **Educational purposes only. Not for production use.**

## Quick Start

```bash
# Setup
git clone <repository-url>
cd hsm-simulator
docker compose up -d postgres

# Build & Run
mvn clean install
mvn spring-boot:run

# Access at http://localhost:8080
```

## Documentation

- **[API Reference](docs/API.md)** - REST API endpoints with examples
- **[Database Schema](docs/DATABASE.md)** - Complete schema and ERD
- **[Integration Guide](docs/CLIENT_INTEGRATION_GUIDE.md)** - Java, Python, cURL examples
- **[Email Testing](docs/EMAIL_TESTING.md)** - Email configuration for key ceremony
- **[User Manuals](docs/user-manual/)** - Step-by-step operation guides
- **[Test Scenarios](docs/test-scenario/)** - Comprehensive testing guides

## Features

### Core HSM Operations
- **Key Ceremony**: Multi-custodian initialization with Shamir's Secret Sharing (2-of-3 threshold)
- **Key Management**: Generate, rotate, and manage cryptographic keys (LMK, TMK, TPK, TSK, ZMK, ZPK, ZSK, KEK)
- **Key Hierarchy**: Parent-child relationships (TMK→TPK/TSK, ZMK→ZPK/ZSK)

### PIN Operations
- **Formats**: ISO-0, ISO-1, ISO-3, ISO-4 (ISO 9564-1:2002)
- **Operations**: Generate, encrypt, verify, translate (TPK→ZPK, ZPK→LMK)
- **PVV**: PIN Verification Value generation

### MAC Operations
- **Algorithms**: AES-CMAC (NIST SP 800-38B), HMAC-SHA256
- **Output**: 64-bit (banking), 128-bit, 256-bit

### Banking Infrastructure
- **Four-Party Model**: ISSUER, ACQUIRER, SWITCH, PROCESSOR
- **Terminals**: ATM, POS, MPOS, E_COMMERCE
- **Inter-bank**: Zone key exchange, PIN translation

### REST API
Complete API for workshop integration (compatible with [training-spring-jpos-2025](https://github.com/artivisi/training-spring-jpos-2025)):
- `POST /api/hsm/pin/encrypt` - Encrypt PIN block
- `POST /api/hsm/pin/verify-with-pvv` - Verify PIN with PVV
- `POST /api/hsm/pin/translate/tpk-to-zpk` - Translate PIN (acquirer)
- `POST /api/hsm/pin/translate/zpk-to-lmk` - Translate PIN (issuer)
- `POST /api/hsm/mac/generate` - Generate MAC
- `POST /api/hsm/mac/verify` - Verify MAC
- `POST /api/hsm/keys/initialize` - Initialize complete key set
- `POST /api/hsm/key/generate` - Generate key
- `POST /api/hsm/key/exchange` - Exchange key

See [API.md](docs/API.md) for complete reference.

## Architecture

### Transaction Flow

```
Cardholder → Terminal (encrypt PIN with TPK)
  → Acquirer (validate, translate TPK→ZPK)
  → Network → Issuer (translate ZPK→LMK, verify PIN)
  → Response
```

### Key Types

| Key | Purpose | Parent |
|-----|---------|--------|
| LMK | Local Master Key - PIN storage encryption | - |
| TMK | Terminal Master Key - Key distribution to terminals | - |
| TPK | Terminal PIN Key - PIN encryption at terminal | TMK |
| TSK | Terminal Security Key - MAC for terminal messages | TMK |
| ZMK | Zone Master Key - Inter-bank key exchange | - |
| ZPK | Zone PIN Key - Inter-bank PIN protection | ZMK |
| ZSK | Zone Session Key - Inter-bank message encryption | ZMK |
| KEK | Key Encryption Key - Generic key encryption | - |

## Technology Stack

- **Backend**: Spring Boot 3.5.6, Java 21
- **Database**: PostgreSQL 17, Flyway migrations
- **Frontend**: Thymeleaf, Tailwind CSS 4.1
- **Security**: Spring Security, AES-256, PBKDF2-SHA256
- **Testing**: TestContainer, JUnit 5, Playwright

## Cryptographic Standards

- **Master Keys**: AES-256-GCM, SHA-256 fingerprints
- **Operational Keys**: PBKDF2-SHA256 (100K iterations)
- **PIN Encryption**: AES-128-CBC, random IV
- **MAC**: AES-CMAC (NIST SP 800-38B) or HMAC-SHA256
- **Share Encryption**: AES-256-GCM with random salt

## Development

### Build Commands
```bash
# Full build
mvn clean install

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Run specific test
mvn test -Dtest=HomePageTest
```

### Frontend Development
```bash
# Tailwind CSS watch mode (optional)
npm run build

# Production build (auto-run by Maven)
npm run build-prod
```

### Database
```bash
# Start PostgreSQL
docker compose up -d postgres

# View logs
docker compose logs postgres

# Stop
docker compose down
```

## Database Schema

14 tables with complete audit trail:
- **Core**: banks, terminals, master_keys (unified storage for all key types)
- **Key Ceremony**: key_ceremonies, key_custodians, ceremony_custodians, key_shares, passphrase_contributions
- **Audit**: ceremony_audit_logs, ceremony_statistics
- **Operations**: zone_key_exchanges, key_rotation_history, rotation_participants
- **PIN/MAC**: generated_pins, generated_macs
- **Auth**: users

See [DATABASE.md](docs/DATABASE.md) for ERD and details.

## Sample Keys Guide

The V2 migration (`V2__insert_sample_data.sql`) creates 6 pre-generated keys for immediate testing. These keys demonstrate the complete key hierarchy and enable PIN/MAC operations without running a key ceremony.

### Sample Keys Overview

| Key ID | Type | Purpose | Parent | Storage Location |
|--------|------|---------|--------|------------------|
| LMK-ISS001-SAMPLE | LMK | PIN storage encryption in database | - | HSM/Database only |
| TMK-ISS001-SAMPLE | TMK | Master key for terminal key distribution | - | HSM only |
| TPK-TRM-ISS001-ATM-001 | TPK | PIN encryption at ATM terminal | TMK | ATM Terminal (encrypted under TMK) |
| TSK-TRM-ISS001-ATM-001 | TSK | MAC generation at ATM terminal | TMK | ATM Terminal (encrypted under TMK) |
| ZMK-ISS001-ACQ001 | ZMK | Inter-bank key exchange | - | Both bank HSMs |
| ZPK-ISS001-ACQ001 | ZPK | Inter-bank PIN translation | ZMK | Both bank HSMs |

### Key Generation Details

**Master Keys (LMK, TMK):**
- Algorithm: AES-256
- Generation: `KeyGenerator.getInstance("AES")` with `SecureRandom`
- Storage: Direct binary storage in `master_keys.key_data`
- Fingerprint: SHA-256 hash (24 chars)
- Checksum: SHA-256 derived (16 chars)

**Derived Keys (TPK, TSK):**
- Algorithm: AES-256 derived to operational size
- Generation: PBKDF2-SHA256 with 100,000 iterations
- Context: `"TPK:48a9e84c-ff57-4483-bf83-b255f34a6466:TRM-ISS001-ATM-001"`
- Parent: TMK-ISS001-SAMPLE
- KDF Salt: Terminal ID (e.g., `TRM-ISS001-ATM-001`)

### Testing with Sample Keys

**PIN Encryption Test:**
```bash
curl -X POST http://localhost:8080/api/hsm/pin/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "pan": "4111111111111111",
    "pin": "1234",
    "keyId": "TPK-TRM-ISS001-ATM-001",
    "format": "ISO_0"
  }'
```

**MAC Generation Test:**
```bash
curl -X POST http://localhost:8080/api/hsm/mac/generate \
  -H "Content-Type: application/json" \
  -d '{
    "data": "Test transaction data",
    "keyId": "TSK-TRM-ISS001-ATM-001",
    "algorithm": "AES_CMAC"
  }'
```

**Sample Data Included:**
- Pre-encrypted PIN: PAN `4111111111111111`, PIN `1234`, PVV `0187`
- Pre-generated MAC for message verification
- Ready for immediate API testing

### Regenerating Sample Keys

Sample keys can be regenerated using test classes:
- `SampleKeyGeneratorTest.java` - Generates cryptographic keys
- `SampleDataGeneratorTest.java` - Generates encrypted PINs and MACs

## Key Storage & Distribution Architecture

### ATM Host (Bank HSM)

**Keys Stored:**
- LMK: PIN storage encryption (never leaves HSM)
- TMK: Terminal master key (never leaves HSM)
- ZMK: Zone master key for inter-bank communication
- ZPK: Zone PIN key (derived from ZMK)

**Operations:**
- Generate TPK/TSK from TMK using PBKDF2
- Encrypt TPK/TSK under TMK for terminal distribution
- Store customer PINs encrypted under LMK
- Translate PINs between encryption domains (TPK↔ZPK↔LMK)

**Storage Format:**
```
master_keys table:
├── LMK-ISS001-SAMPLE (key_data: raw AES-256 bytes)
├── TMK-ISS001-SAMPLE (key_data: raw AES-256 bytes)
└── ZMK-ISS001-ACQ001 (key_data: raw AES-256 bytes)
```

### ATM Terminal

**Keys Received (encrypted under TMK):**
- TPK: Decrypted locally, used for PIN block encryption
- TSK: Decrypted locally, used for MAC generation

**Keys NOT Stored:**
- TMK: Never transmitted to or stored in terminal
- LMK: Remains only in bank HSM

**Operations:**
1. **PIN Entry**: Customer enters PIN
2. **PIN Block Creation**: Format PIN with PAN (ISO-0/1/3/4)
3. **Encryption**: Encrypt PIN block using TPK
4. **MAC Generation**: Generate MAC using TSK
5. **Transmission**: Send encrypted PIN block + MAC to host

**Terminal Storage (Secure Crypto Processor):**
```
Terminal Memory (volatile):
├── TPK (AES-128/256, decrypted from TMK-encrypted delivery)
└── TSK (AES-128/256, decrypted from TMK-encrypted delivery)

Terminal Does NOT Store:
├── TMK (only used during key injection, then erased)
├── Clear PINs (never stored)
└── LMK (bank-side only)
```

### Key Distribution Flow

```
Step 1: TMK Injection (Physical/Secure Channel)
  Bank → [TMK encrypted under KEK] → Terminal
  Terminal decrypts and stores TMK temporarily

Step 2: Working Key Distribution (Over Network)
  Bank HSM:
    - Derive TPK from TMK (PBKDF2)
    - Encrypt TPK under TMK
    - Send encrypted TPK to terminal

  Terminal:
    - Decrypt TPK using stored TMK
    - Store TPK in secure memory
    - Erase TMK after key loading complete

Step 3: PIN Transaction
  Terminal:
    - Encrypt PIN block with TPK
    - Generate MAC with TSK
    - Send to bank

  Bank HSM:
    - Verify MAC with TSK
    - Decrypt PIN block with TPK (or translate to LMK)
    - Verify PIN against stored value
```

### Key Hierarchy in Practice

**Bank ISS001 Key Structure:**
```
LMK-ISS001 (PIN storage)
  └── [PINs encrypted at rest]

TMK-ISS001 (Terminal master)
  ├── TPK-TRM-ISS001-ATM-001 (Terminal 1 PIN key)
  ├── TSK-TRM-ISS001-ATM-001 (Terminal 1 security key)
  ├── TPK-TRM-ISS001-ATM-002 (Terminal 2 PIN key)
  └── TSK-TRM-ISS001-ATM-002 (Terminal 2 security key)

ZMK-ISS001-ACQ001 (Inter-bank master)
  └── ZPK-ISS001-ACQ001 (Inter-bank PIN key)
```

**Terminal TRM-ISS001-ATM-001 Key Structure:**
```
Secure Crypto Processor:
├── TPK (for encrypting customer PINs)
└── TSK (for message authentication)

Operations:
├── PIN Entry → Encrypt with TPK → Send to bank
├── Transaction → Generate MAC with TSK → Send to bank
└── Key Update → Receive new TPK/TSK encrypted under TMK
```

## Zone Key Usage

Zone keys enable secure communication between different banks (zones) in the payment network.

### Zone Key Types

**ZMK (Zone Master Key):**
- Purpose: Encrypt key exchange between banks
- Usage: Protect ZPK/ZSK during distribution
- Storage: Both participating banks' HSMs
- Example: `ZMK-ISS001-ACQ001` shared between Issuer and Acquirer

**ZPK (Zone PIN Key):**
- Purpose: Encrypt PIN blocks in inter-bank transactions
- Usage: PIN translation between banks
- Derivation: From ZMK using PBKDF2
- Example: Acquirer translates PIN from TPK to ZPK for forwarding to Issuer

**ZSK (Zone Session Key):**
- Purpose: Encrypt transaction messages between banks
- Usage: Message encryption, MAC generation for inter-bank traffic
- Derivation: From ZMK using PBKDF2
- Example: Settlement data encryption between banks

### Inter-Bank PIN Translation Flow

**Scenario:** Customer uses Issuer Bank card at Acquirer Bank ATM

```
1. PIN Entry at Acquirer ATM:
   Customer → [PIN: 1234] → ATM Terminal
   ATM → Encrypt with TPK-ACQ → [PIN Block A]

2. Acquirer PIN Translation (TPK → ZPK):
   Acquirer HSM:
     - Decrypt PIN block with TPK-ACQ
     - Re-encrypt with ZPK-ACQ-ISS
     - Result: [PIN Block B] (same PIN, different encryption)

3. Network Transmission:
   Acquirer → [PIN Block B + Transaction] → Network → Issuer

4. Issuer PIN Translation (ZPK → LMK):
   Issuer HSM:
     - Decrypt PIN block with ZPK-ACQ-ISS
     - Re-encrypt with LMK-ISS (for comparison)
     - Or: Extract clear PIN and verify against PVV

5. Verification:
   Issuer HSM:
     - Compare with stored encrypted PIN (Method A)
     - Or: Verify against stored PVV (Method B)
     - Return approval/denial
```

### Zone Key Distribution

**Initial Setup:**
```bash
# Step 1: Acquirer generates ZMK
curl -X POST http://localhost:8080/api/hsm/key/generate \
  -d '{"keyType": "ZMK", "bankCode": "ACQ001"}'

# Step 2: Acquirer encrypts ZMK under Issuer's KEK
# (In production: secure key exchange ceremony)

# Step 3: Both banks derive ZPK from shared ZMK
# Context: "ZPK:ACQ001:ISS001"
# Both banks get identical ZPK for PIN translation
```

### Zone Key Usage Examples

**PIN Translation (TPK → ZPK):**
```bash
curl -X POST http://localhost:8080/api/hsm/pin/translate/tpk-to-zpk \
  -d '{
    "encryptedPinBlock": "...",
    "pan": "4111111111111111",
    "sourceKeyId": "TPK-TRM-ACQ001-ATM-001",
    "targetKeyId": "ZPK-ACQ001-ISS001"
  }'
```

**PIN Translation (ZPK → LMK):**
```bash
curl -X POST http://localhost:8080/api/hsm/pin/translate/zpk-to-lmk \
  -d '{
    "encryptedPinBlock": "...",
    "pan": "4111111111111111",
    "sourceKeyId": "ZPK-ACQ001-ISS001",
    "targetKeyId": "LMK-ISS001"
  }'
```

### Zone Key Security

**Key Isolation:**
- Each bank pair has unique ZMK/ZPK
- ZMK-ACQ001-ISS001 ≠ ZMK-ACQ001-SWT001
- Compromise of one zone key doesn't affect others

**Key Rotation:**
- Zone keys rotated periodically (30-90 days)
- Coordination required between both banks
- Old keys retained for grace period

**Access Control:**
- Zone keys never leave HSM boundary
- PIN never in clear during translation
- All operations logged in audit trail

## Project Structure

```
hsm-simulator/
├── src/main/java/com/artivisi/hsm/simulator/
│   ├── entity/          # 22 JPA entities
│   ├── repository/      # 18 Spring Data repositories
│   ├── service/         # 14 services (4,710 lines)
│   ├── web/            # 12 controllers (3,047 lines)
│   ├── config/         # Spring configurations
│   └── dto/            # Data transfer objects
├── src/main/resources/
│   ├── db/migration/   # Flyway migrations (V1, V2, V3)
│   ├── templates/      # 20 Thymeleaf templates
│   └── static/css/     # Tailwind CSS
└── src/test/java/      # 4 test files (minimal coverage)
```

## Configuration

**Database** (compose.yaml):
- URL: `jdbc:postgresql://localhost:5432/hsm_simulator`
- User: `hsm_user`
- Password: `xK9m2pQ8vR5nF7tA1sD3wE6zY`

**Default Admin**:
- Username: `admin`
- Password: `admin123`

## Troubleshooting

### Port 8080 in use
```bash
lsof -i :8080  # Find process
# Change port in application.properties or kill process
```

### Database connection failed
```bash
docker compose ps          # Check status
docker compose restart postgres
```

### Tailwind CSS not compiling
```bash
mvn clean install         # Rebuild
npm install && npm run build  # Manual compile
```

### Frontend changes not visible
- Hard refresh: `Ctrl+Shift+R`
- Check `target/classes/static/css/output.css`
- Run `npm run build` in watch mode

## License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.

## Credits

[![Built with Claude Code](https://img.shields.io/badge/Built%20with-Claude%20Code-5A67D8?logo=anthropic&logoColor=white)](https://claude.ai/code)
[![Assisted by GLM-4.5](https://img.shields.io/badge/Assisted%20by-GLM--4.5-4285F4?logo=ai&logoColor=white)](https://www.zhipuai.cn/)

⚠️ **Disclaimer**: Educational simulator only. Not verified for production use. Contains plaintext credentials and clear PINs for educational purposes.
