# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Database Setup
```bash
# Start PostgreSQL container
docker compose up -d postgres

# Check database status
docker compose ps

# View database logs
docker compose logs postgres

# Stop database
docker compose down
```

### Application Build & Run
```bash
# Full build (includes npm install and Tailwind CSS compilation)
mvn clean install

# Run application
mvn spring-boot:run

# Application accessible at http://localhost:8080
```

### Frontend Development
```bash
# Install Node.js dependencies (handled by Maven, but can run manually)
npm install

# Watch mode for Tailwind CSS development (auto-recompile on changes)
npm run build

# Production build with minification
npm run build-prod
```

### Testing
```bash
# Run all tests (includes TestContainer and Playwright E2E tests)
mvn test

# Run specific Playwright test
mvn test -Dtest=HomePageTest

# Run specific test method
mvn test -Dtest=HomePageTest#shouldLoadHomepageWithCorrectTitle
```

## Architecture Overview

### Technology Stack
- **Backend**: Spring Boot 3.5.6 with Java 21
- **Frontend**: Thymeleaf with Layout Dialect + Tailwind CSS 4.1
- **Database**: PostgreSQL 17 with Flyway migrations
- **Testing**: TestContainer (PostgreSQL), Playwright (E2E), JUnit 5
- **Build**: Maven with exec-maven-plugin for npm integration

### Package Structure
```
com.artivisi.hsm.simulator/
├── config/          # JPA auditing and other Spring configurations
├── entity/          # JPA entities with Spring Data auditing
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic layer
└── web/            # Spring MVC controllers
```

### Key Domain Concepts

This is an HSM (Hardware Security Module) simulator for educational purposes focusing on cryptographic key management operations in banking systems.

**Key Ceremony System**: Multi-custodian key initialization using Shamir's Secret Sharing with 2-of-3 threshold scheme. Key entities:
- `KeyCeremony`: Orchestrates the key initialization/restoration process
- `KeyCustodian`: Represents individuals authorized to hold key shares
- `CeremonyCustodian`: Links custodians to specific ceremonies
- `MasterKey`: Unified table storing ALL cryptographic keys (HSM master keys, TMK, TPK, TSK, ZMK, ZPK, ZSK)
- `KeyShare`: Individual shares distributed to custodians
- `PassphraseContribution`: Custodian entropy contributions during initialization

**Four-Party Card Processing Model**:
- `Bank`: Represents organizations (ISSUER, ACQUIRER, SWITCH, PROCESSOR)
- `Terminal`: ATM, POS, MPOS, or E_COMMERCE terminals associated with banks
- `ZoneKeyExchange`: Tracks inter-bank key exchanges for ZMK, ZPK, ZSK
- `KeyRotationHistory`: Complete audit trail of key rotation activities

**Key Types in Banking Context** (stored in `master_keys` table with `key_type` field):
- **HSM_MASTER_KEY**: Root key generated through key ceremony
- **TMK** (Terminal Master Key): Encrypts key distribution to terminals
- **TPK** (Terminal PIN Key): Encrypts PIN blocks at terminal level (child of TMK)
- **TSK** (Terminal Security Key): Provides MAC and authentication (child of TMK)
- **ZMK** (Zone Master Key): Encrypts inter-bank key exchanges
- **ZPK** (Zone PIN Key): Protects PIN data between banks (child of ZMK)
- **ZSK** (Zone Session Key): Encrypts inter-bank transaction data (child of ZMK)
- **LMK** (Local Master Key): Bank-specific master key
- **KEK** (Key Encryption Key): Generic key encryption key

**Key Hierarchy**: The `master_keys` table supports parent-child relationships through `parent_key_id`:
- TMK → TPK/TSK (terminal key hierarchy)
- ZMK → ZPK/ZSK (zone key hierarchy)
- Tracks rotation history via `rotated_from_key_id`

### Database Migrations
Located in `src/main/resources/db/migration/`:
- **V1**: Creates comprehensive schema including:
  - Key ceremony tables (key_ceremonies, ceremony_custodians, key_custodians, etc.)
  - Banking infrastructure (banks, terminals)
  - Unified master_keys table supporting all key types (HSM_MASTER_KEY, TMK, TPK, TSK, ZMK, ZPK, ZSK, LMK, KEK)
  - Key hierarchy support (parent_key_id, rotated_from_key_id)
  - Zone key exchange tracking (zone_key_exchanges)
  - Key rotation history (key_rotation_history)
  - Comprehensive audit logging (ceremony_audit_logs)

- **V2**: Inserts four-party model sample data:
  - 4 banks (2 Issuers, 1 Acquirer, 1 Switch)
  - 5 terminals (2 ATMs, 2 POS, 1 MPOS)
  - 3 key custodians
  - 6 sample keys demonstrating hierarchy (TMK, TPK, ZMK, ZPK)
  - 1 zone key exchange example

Flyway automatically runs migrations on application startup.

### Frontend Integration
- Tailwind CSS source: `src/main/resources/static/css/input.css`
- Compiled output: `src/main/resources/static/css/output.css` (auto-generated)
- Maven automatically runs `npm install` and `npm run build-prod` during build
- For development with hot reload, run `npm run build` separately

### Testing Strategy
- **Unit Tests**: Standard JUnit 5 tests
- **Integration Tests**: Use TestContainer with PostgreSQL for repository/service layer tests
- **E2E Tests**: Playwright with Page Object pattern in `src/test/java/com/artivisi/hsm/simulator/playwright/`
  - `pages/`: Page object classes (e.g., `HomePage`, `BasePage`)
  - `tests/`: Test classes (e.g., `HomePageTest`)

## Important Configuration

### Database Connection
Configured in `src/main/resources/application.properties`:
- URL: `jdbc:postgresql://localhost:5432/hsm_simulator`
- Username: `hsm_user`
- Password: `xK9m2pQ8vR5nF7tA1sD3wE6zY`

### JPA Configuration
- Hibernate DDL mode: `validate` (schema managed by Flyway)
- SQL logging enabled in development
- JPA auditing enabled via `@EnableJpaAuditing`

### Thymeleaf
- Template caching disabled for development
- Layout Dialect enabled for template composition
- Templates in `src/main/resources/templates/`

## Development Workflows

### Adding New Features
1. Create database migration in `src/main/resources/db/migration/V{n}__description.sql`
2. Create/update JPA entities in `entity/` package
3. Create Spring Data repositories in `repository/` package
4. Implement business logic in `service/` package
5. Add controller endpoints in `web/` package
6. Create Thymeleaf templates in `src/main/resources/templates/`
7. Add Tailwind classes as needed in templates
8. Write tests in appropriate test package

### Modifying Frontend Styles
1. Edit Thymeleaf templates and add Tailwind utility classes
2. If custom CSS needed, edit `src/main/resources/static/css/input.css`
3. Run `npm run build` for development or let Maven handle it
4. Refresh browser to see changes (Thymeleaf cache disabled in dev)

### Database Schema Changes
1. Create new Flyway migration with next version number
2. Update corresponding JPA entities
3. Test with `mvn clean install` to ensure migration runs successfully

## Cryptographic Standards

### Master Key Storage
- **Algorithm**: AES-256
- **Mode**: GCM with random IV for key wrapping/share encryption
- **Fingerprints**: SHA-256 (not MD5)
- **Checksums**: SHA-256 derived (not MD5)

### Operational Key Derivation
- **Method**: PBKDF2-SHA256 with context
- **Iterations**: 100,000
- **Context Format**: `"KEY_TYPE:BANK_ID:IDENTIFIER"`
- **Key Sizes**:
  - PIN operations: 128-bit (16 bytes)
  - MAC operations: 128-bit or 256-bit
  - Zone operations: 256-bit (32 bytes)
- **No Truncation**: Keys are properly derived, not truncated from master keys

### PIN Operations
- **Encryption**: AES-128-CBC with random IV and PKCS5 padding
- **IV Handling**: Random 16-byte IV prepended to ciphertext
- **PIN Formats**: ISO-0, ISO-1, ISO-3, ISO-4 (ISO 9564-1:2002)
- **PVV Calculation**: SHA-256(PIN + PAN), first 4 decimal digits

### MAC Operations
- **Default Algorithm**: AES-CMAC (NIST SP 800-38B)
- **Output Lengths**: 64-bit (banking standard), 128-bit, 256-bit
- **Alternative**: HMAC-SHA256 with configurable output
- **Key Derivation**: Context-based PBKDF2 from TSK/ZSK master keys

### Share Encryption (Key Ceremony)
- **Algorithm**: AES-256-GCM
- **Salt**: Random 32-byte salt per share (not fixed)
- **Salt Storage**: Prepended to encrypted share data for offline recovery
- **Format**: `[32-byte salt][encrypted share data]`

### Key Hierarchy
```
Master Keys (AES-256) → Operational Keys (PBKDF2-derived)
  ├─ LMK → PIN Storage Keys (AES-128)
  ├─ TMK → TPK/TSK (AES-128)
  └─ ZMK → ZPK/ZSK (AES-256)
```

### Key Caching
- Derived operational keys cached in ConcurrentHashMap
- Cache key format: `keyId:context`
- Avoids re-derivation overhead for repeated operations

## Security Notes

This is an educational simulator, not for production use:
- Database credentials are in plaintext configuration
- Clear PINs stored for educational purposes (never in production)
- Cryptographic operations follow banking standards but lack hardware security boundary
- Key material stored in software (production HSM uses hardware protection)
