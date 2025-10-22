# Entity Classes Documentation

## Overview

This document describes the JPA entity classes for the HSM Simulator, covering both Key Ceremony functionality and Banking Key Management operations. All entities use:

- **Lombok** annotations (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- **UUID** primary keys with `@GeneratedValue(strategy = GenerationType.UUID)`
- **Spring Data JPA Auditing** for automatic timestamp management
- **Jakarta Validation** constraints where appropriate

## Banking Infrastructure Entities

### Bank
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `banks`

Represents banks/organizations in the four-party card processing model.

**Key Fields:**
- `id`: UUID primary key
- `bankCode`: Unique bank identifier
- `bankName`: Bank name
- `bankType`: Enum (ISSUER, ACQUIRER, SWITCH, PROCESSOR)
- `countryCode`: ISO country code
- `status`: Enum (ACTIVE, INACTIVE, SUSPENDED)
- `createdAt`, `updatedAt`: Automatic timestamps

**Relationships:**
- OneToMany → `Terminal`: Terminals owned by bank
- OneToMany → `MasterKey`: Keys managed by bank

---

### Terminal
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `terminals`

Represents terminal devices (ATM, POS, MPOS, E-commerce).

**Key Fields:**
- `id`: UUID primary key
- `terminalId`: Unique terminal identifier
- `terminalName`: Terminal name
- `bank`: ManyToOne → Bank
- `terminalType`: Enum (ATM, POS, MPOS, VIRTUAL, E_COMMERCE)
- `location`: Physical location
- `status`: Enum (ACTIVE, INACTIVE, SUSPENDED, MAINTENANCE)
- `createdAt`, `updatedAt`: Automatic timestamps

**Relationships:**
- ManyToOne → `Bank`: Owning bank
- OneToMany → `MasterKey`: Terminal-specific keys (TMK, TPK, TSK)

---

## Key Management Entities

### MasterKey
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `master_keys`

**Unified entity storing ALL cryptographic keys** (HSM master keys, TMK, TPK, TSK, ZMK, ZPK, ZSK, LMK, KEK) with hierarchy and rotation support.

**Key Fields:**
- `id`: UUID primary key
- `masterKeyId`: Unique business identifier
- `keyCeremony`: ManyToOne → KeyCeremony (nullable, for HSM master keys)
- `bank`: ManyToOne → Bank (nullable, for bank-specific keys)
- `terminal`: ManyToOne → Terminal (nullable, for terminal keys)
- `parentKey`: ManyToOne → MasterKey (nullable, self-reference for hierarchy)
- `keyType`: Enum (HSM_MASTER_KEY, TMK, TPK, TSK, ZMK, ZPK, ZSK, LMK, KEK)
- `keyUsage`: Enum (PIN_ENCRYPTION, DATA_ENCRYPTION, KEY_ENCRYPTION, MAC_GENERATION, SESSION, MASTER)
- `algorithm`: Encryption algorithm (e.g., "AES-256")
- `keySize`: Key size in bits (128, 192, 256)
- `keyDataEncrypted`: byte[] encrypted key material
- `keyFingerprint`: SHA-256 fingerprint
- `keyChecksum`: Checksum for verification
- `combinedEntropyHash`: Hash of ceremony contributions (nullable)
- `generationMethod`: PBKDF2, SECURE_RANDOM, DERIVED, KEY_CEREMONY
- `kdfIterations`: KDF iterations (default: 100000)
- `kdfSalt`: KDF salt value
- `status`: Enum (ACTIVE, ROTATED, REVOKED, EXPIRED)
- `generatedAt`, `activatedAt`, `expiresAt`, `revokedAt`: Timestamps
- `revocationReason`: Reason for revocation
- `rotatedFromKey`: ManyToOne → MasterKey (nullable, self-reference for rotation chain)
- `rotationReason`: Reason for rotation

**Relationships:**
- ManyToOne → `KeyCeremony`: Optional ceremony that generated this key
- ManyToOne → `Bank`: Optional bank owning this key
- ManyToOne → `Terminal`: Optional terminal owning this key
- ManyToOne → `MasterKey` (parent): Parent key in hierarchy
- OneToMany → `MasterKey` (children): Child keys encrypted under this key
- ManyToOne → `MasterKey` (rotatedFrom): Previous version in rotation chain
- OneToMany → `MasterKey` (rotatedTo): Keys that replaced this one
- OneToMany → `KeyShare`: Shamir shares (for HSM master keys only)
- OneToMany → `ZoneKeyExchange`: Keys involved in inter-bank exchanges
- OneToMany → `KeyRotationHistory`: Rotation history

**Key Type Usage Examples:**
- **HSM_MASTER_KEY**: Root key from key ceremony
- **TMK**: Encrypts TPK and TSK for terminal distribution
- **TPK** (parent=TMK): Encrypts PIN blocks at terminal
- **TSK** (parent=TMK): MAC generation and authentication
- **ZMK**: Encrypts ZPK and ZSK for inter-bank exchange
- **ZPK** (parent=ZMK): Encrypts PIN data between banks
- **ZSK** (parent=ZMK): Encrypts transaction data between banks

---

### ZoneKeyExchange
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `zone_key_exchanges`

Tracks inter-bank key exchanges for the four-party model.

**Key Fields:**
- `id`: UUID primary key
- `exchangeId`: Unique identifier
- `sourceBank`: ManyToOne → Bank
- `destinationBank`: ManyToOne → Bank
- `zmk`: ManyToOne → MasterKey (ZMK used for this exchange)
- `exchangedKey`: ManyToOne → MasterKey (ZPK or ZSK being exchanged)
- `exchangeType`: Enum (INITIAL, RENEWAL, EMERGENCY, ROTATION)
- `keyTransportMethod`: Enum (ENCRYPTED_UNDER_ZMK, MANUAL, COURIER, SECURE_CHANNEL)
- `transportKeyFingerprint`: Fingerprint for verification
- `exchangeStatus`: Enum (INITIATED, IN_TRANSIT, ACKNOWLEDGED, ACTIVATED, REJECTED, EXPIRED)
- `initiatedAt`, `acknowledgedAt`, `activatedAt`, `expiresAt`: Timestamps
- `initiatedBy`, `acknowledgedBy`: User identifiers

**Relationships:**
- ManyToOne → `Bank` (source): Sending bank
- ManyToOne → `Bank` (destination): Receiving bank
- ManyToOne → `MasterKey` (ZMK): Zone master key
- ManyToOne → `MasterKey` (exchanged): Key being exchanged

---

### KeyRotationHistory
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `key_rotation_history`

Complete audit trail of key rotation activities for compliance.

**Key Fields:**
- `id`: UUID primary key
- `rotationId`: Unique identifier
- `oldKey`: ManyToOne → MasterKey
- `newKey`: ManyToOne → MasterKey
- `rotationType`: Enum (SCHEDULED, EMERGENCY, COMPLIANCE, COMPROMISE, EXPIRATION)
- `rotationReason`: Text description
- `rotationInitiatedBy`, `rotationApprovedBy`: User identifiers
- `rotationStartedAt`, `rotationCompletedAt`: Timestamps
- `rotationStatus`: Enum (IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK, CANCELLED)
- `affectedTerminalsCount`, `affectedBanksCount`: Impact metrics
- `rollbackRequired`: Boolean flag
- `rollbackCompletedAt`: Timestamp
- `notes`: Additional notes

**Relationships:**
- ManyToOne → `MasterKey` (old): Key being rotated
- ManyToOne → `MasterKey` (new): Replacement key

---

## Key Ceremony Entities

### KeyCustodian
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `key_custodians`

Individuals authorized to participate in key ceremonies.

**Key Fields:**
- `id`: UUID primary key
- `custodianId`: Unique identifier (e.g., "CUST-A-001")
- `fullName`, `email`, `phone`, `department`
- `status`: Enum (ACTIVE, INACTIVE, SUSPENDED)
- `createdAt`, `updatedAt`: Automatic timestamps

**Relationships:**
- OneToMany → `CeremonyCustodian`: Ceremony participations

---

### KeyCeremony
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `key_ceremonies`

Orchestrates key initialization/restoration using Shamir's Secret Sharing.

**Key Fields:**
- `id`: UUID primary key
- `ceremonyId`: Unique identifier
- `ceremonyName`, `purpose`: Description
- `ceremonyType`: Enum (INITIALIZATION, RESTORATION, KEY_ROTATION)
- `status`: Enum (PENDING, AWAITING_CONTRIBUTIONS, PARTIAL_CONTRIBUTIONS, GENERATING_KEY, COMPLETED, CANCELLED, EXPIRED)
- `numberOfCustodians`: Total custodians (default: 3)
- `threshold`: Minimum shares (default: 2, constraint: ≤ numberOfCustodians)
- `algorithm`: e.g., "AES-256"
- `keySize`: 128, 192, or 256 bits
- `contributionDeadline`: Deadline timestamp
- `startedAt`, `completedAt`, `cancelledAt`: Timestamps

**Relationships:**
- OneToMany → `CeremonyCustodian`: Custodian assignments
- OneToOne → `MasterKey`: Generated key
- OneToOne → `CeremonyStatistics`: Statistics
- OneToMany → `CeremonyAuditLog`: Audit logs

---

### CeremonyCustodian
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `ceremony_custodians`

Links custodians to ceremonies with contribution tracking.

**Key Fields:**
- `id`: UUID primary key
- `keyCeremony`: ManyToOne → KeyCeremony
- `keyCustodian`: ManyToOne → KeyCustodian
- `custodianOrder`: 1, 2, 3...
- `custodianLabel`: A, B, C...
- `contributionToken`: Unique secure token
- `contributionLink`: URL for contribution page
- `contributionStatus`: Enum (PENDING, CONTRIBUTED, EXPIRED)
- `contributedAt`, `shareSentAt`: Timestamps

**Relationships:**
- ManyToOne → `KeyCeremony`, `KeyCustodian`
- OneToOne → `PassphraseContribution`
- OneToOne → `KeyShare`
- OneToMany → `ContributionReminder`

---

### PassphraseContribution
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `passphrase_contributions`

Stores custodian entropy contributions (never stores plaintext passphrases).

**Key Fields:**
- `id`: UUID primary key
- `contributionId`: Unique identifier
- `ceremonyCustodian`: ManyToOne → CeremonyCustodian
- `passphraseHash`: SHA-256 hash
- `passphraseEntropyScore`: 0.0-10.0
- `passphraseStrength`: Enum (WEAK, FAIR, GOOD, STRONG, VERY_STRONG)
- `passphraseLength`: Character count
- `contributionFingerprint`: Unique fingerprint
- `contributedAt`: Timestamp
- `ipAddress`, `userAgent`: Audit metadata

---

### KeyShare
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `key_shares`

Shamir's Secret Sharing key shares for master key recovery.

**Key Fields:**
- `id`: UUID primary key
- `shareId`: Unique identifier
- `masterKey`: ManyToOne → MasterKey
- `ceremonyCustodian`: ManyToOne → CeremonyCustodian
- `shareIndex`: x value (1, 2, 3...)
- `shareDataEncrypted`: byte[] encrypted share
- `shareVerificationHash`: Verification hash
- `polynomialDegree`: threshold - 1
- `primeModulus`: Prime number (educational)
- `generatedAt`, `distributedAt`, `lastUsedAt`: Timestamps
- `distributionMethod`: Enum (EMAIL, PHYSICAL, API, MANUAL)
- `usedInRestoration`: Boolean flag

---

### KeyRestorationRequest
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `key_restoration_requests`

Tracks master key restoration requests.

**Key Fields:**
- `id`: UUID primary key
- `restorationId`: Unique identifier
- `originalCeremony`: ManyToOne → KeyCeremony
- `masterKey`: ManyToOne → MasterKey
- `restorationReason`: Text description
- `requestedBy`, `approvedBy`: User identifiers
- `requestedAt`, `approvedAt`, `restorationCompletedAt`: Timestamps
- `status`: Enum (PENDING, APPROVED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED)
- `sharesRequired`, `sharesSubmitted`: Counts
- `restoredKeyVerified`: Boolean flag

**Relationships:**
- ManyToOne → `KeyCeremony`, `MasterKey`
- OneToMany → `RestorationShareSubmission`

---

### RestorationShareSubmission
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `restoration_share_submissions`

Tracks share submissions during restoration.

**Key Fields:**
- `id`: UUID primary key
- `restorationRequest`: ManyToOne → KeyRestorationRequest
- `keyShare`: ManyToOne → KeyShare
- `ceremonyCustodian`: ManyToOne → CeremonyCustodian
- `submittedAt`, `verifiedAt`: Timestamps
- `verificationStatus`: Enum (PENDING, VERIFIED, REJECTED)
- `ipAddress`: Audit metadata

---

## Monitoring & Audit Entities

### CeremonyAuditLog
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `ceremony_audit_logs`

Comprehensive audit trail for all activities.

**Key Fields:**
- `id`: UUID primary key
- `keyCeremony`: ManyToOne → KeyCeremony (nullable)
- `eventType`: Specific event identifier
- `eventCategory`: Enum (CEREMONY, CONTRIBUTION, KEY_GENERATION, DISTRIBUTION, SECURITY, SYSTEM)
- `eventDescription`: Text description
- `actorType`: Enum (ADMINISTRATOR, CUSTODIAN, SYSTEM, API)
- `actorId`, `actorName`: Actor identification
- `targetEntityType`, `targetEntityId`: Target identification
- `eventStatus`: Enum (SUCCESS, FAILURE, WARNING, INFO)
- `eventSeverity`: Enum (DEBUG, INFO, WARNING, ERROR, CRITICAL)
- `ipAddress`, `userAgent`, `requestId`, `sessionId`: Audit metadata
- `eventMetadata`: JSONB for flexible data
- `errorMessage`, `stackTrace`: Error details
- `createdAt`: Timestamp

---

### CeremonyStatistics
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `ceremony_statistics`

Aggregated ceremony metrics.

**Key Fields:**
- `id`: UUID primary key
- `keyCeremony`: OneToOne → KeyCeremony
- `totalCustodians`, `contributionsReceived`, `contributionsPending`
- `sharesGenerated`, `sharesDistributed`
- `averageContributionTimeMinutes`, `totalDurationMinutes`
- `ceremonyCompletionPercentage`: 0.00-100.00
- `lastActivityAt`, `lastUpdatedAt`: Timestamps

---

### ContributionReminder
**Package:** `com.artivisi.hsm.simulator.entity`
**Table:** `contribution_reminders`

Tracks reminder emails sent to custodians.

**Key Fields:**
- `id`: UUID primary key
- `ceremonyCustodian`: ManyToOne → CeremonyCustodian
- `reminderType`: Enum (INITIAL, FIRST_REMINDER, SECOND_REMINDER, URGENT, DEADLINE_APPROACHING)
- `sentAt`, `deliveryConfirmedAt`: Timestamps
- `sentBy`: User identifier
- `emailStatus`: Enum (SENT, DELIVERED, BOUNCED, FAILED, OPENED)

---

## Technical Implementation

### Lombok Integration
All entities use Lombok annotations:
- `@Data`: Generates getters, setters, toString, equals, hashCode
- `@Builder`: Provides builder pattern
- `@NoArgsConstructor`, `@AllArgsConstructor`: Constructor generation

### Spring Data JPA Auditing
- `@EntityListeners(AuditingEntityListener.class)`: Enables auditing
- `@CreatedDate`: Auto-populates creation timestamp
- Application must have `@EnableJpaAuditing` in configuration

### JPA Best Practices
- All entities use `@Table(name="table_name")` to explicitly map to database tables
- Foreign keys use explicit `@JoinColumn(name="id_tablename")` naming convention
- Indexes defined with `@Table(indexes={...})` matching database schema
- Enums use `@Enumerated(EnumType.STRING)` for readability

### Database Constraints
Entity-level validation mirrors database constraints:
- `@Column(nullable=false)` for NOT NULL constraints
- `@Column(unique=true)` for unique constraints
- Length limits: `@Column(length=n)`
- Check constraints documented in entity JavaDoc

---

*Last Updated: October 22, 2025*
*Version: 2.0 - Added Banking Key Management Entities*
