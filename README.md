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

MIT License

## Credits

Developed with assistance from GLM-4.5 by Z.ai and Claude Code by Anthropic.

⚠️ **Disclaimer**: Educational simulator only. Not verified for production use. Contains plaintext credentials and clear PINs for educational purposes.
