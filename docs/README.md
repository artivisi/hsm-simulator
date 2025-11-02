# HSM Simulator Documentation

Documentation for the HSM (Hardware Security Module) Simulator educational project.

## Quick Reference

### User Guides
- **[HELP.md](HELP.md)** - Getting started, common operations, troubleshooting
- **[EMAIL_TESTING.md](EMAIL_TESTING.md)** - Email configuration for key ceremony
- **[User Manuals](user-manual/)** - Step-by-step operation guides
  - [Key Ceremony](user-manual/key-ceremony.md)
  - [Terminal Keys](user-manual/terminal-key.md)
  - [Zone Keys](user-manual/zone-key.md)
  - [PIN Block Operations](user-manual/pinblock.md)
- **[Test Scenarios](test-scenario/)** - Comprehensive testing guides
  - [Key Ceremony Testing](test-scenario/key-ceremony.md)
  - [Terminal Keys Testing](test-scenario/terminal-keys.md)
  - [Zone Key Testing](test-scenario/zone-key.md)
  - [PIN Block Testing](test-scenario/pinblock.md)

### Developer Reference
- **[API.md](API.md)** - REST API endpoints with request/response examples
- **[DATABASE.md](DATABASE.md)** - Database schema, ERD, and queries
- **[CLIENT_INTEGRATION_GUIDE.md](CLIENT_INTEGRATION_GUIDE.md)** - Integration examples (Java, Python, cURL)

### Technical Reports (Archive)
- **[Cryptographic Refactoring](archive/CRYPTOGRAPHIC_REFACTORING.md)** - Refactoring summary
- **[Key Initialization Audit](archive/KEY_INITIALIZATION_AUDIT.md)** - API compliance audit

## Documentation Structure

```
docs/
├── README.md                        # This file
├── HELP.md                          # Quick start and troubleshooting
├── API.md                           # REST API reference
├── DATABASE.md                      # Database schema
├── CLIENT_INTEGRATION_GUIDE.md      # Integration examples
├── EMAIL_TESTING.md                 # Email setup
├── user-manual/                     # Operation guides
│   ├── key-ceremony.md
│   ├── terminal-key.md
│   ├── zone-key.md
│   └── pinblock.md
├── test-scenario/                   # Testing guides
│   ├── key-ceremony.md
│   ├── terminal-keys.md
│   ├── zone-key.md
│   └── pinblock.md
└── archive/                         # Technical reports
    ├── CRYPTOGRAPHIC_REFACTORING.md
    └── KEY_INITIALIZATION_AUDIT.md
```

## Quick Start

See [../README.md](../README.md) for installation and [HELP.md](HELP.md) for common operations.
