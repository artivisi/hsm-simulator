# Entity Classes Documentation

## Overview

This document describes the JPA entity classes created based on the Flyway migration schema (V1__create_key_ceremony_tables.sql). These entities represent the HSM Simulator's key ceremony functionality with 2-of-3 threshold cryptography using Shamir's Secret Sharing.

## Entity Classes

### 1. KeyCustodian
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `key_custodians`

Represents individuals who serve as key custodians and participate in key ceremonies.

**Key Fields:**
- `id`: UUID primary key
- `custodianId`: Unique identifier (e.g., "CUST-A-001")
- `fullName`: Custodian's full name
- `email`: Unique email address
- `phone`: Contact phone number
- `department`: Organizational department
- `status`: Enum (ACTIVE, INACTIVE, SUSPENDED)
- `createdAt`, `updatedAt`: Automatic timestamps

**Indexes:**
- Email (for quick lookup)
- Status (for filtering)

---

### 2. KeyCeremony
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `key_ceremonies`

Tracks the lifecycle and configuration of key ceremonies.

**Key Fields:**
- `id`: UUID primary key
- `ceremonyId`: Unique identifier
- `ceremonyName`: Descriptive name
- `purpose`: Ceremony description
- `ceremonyType`: Enum (INITIALIZATION, RESTORATION, KEY_ROTATION)
- `status`: Enum (PENDING, AWAITING_CONTRIBUTIONS, PARTIAL_CONTRIBUTIONS, GENERATING_KEY, COMPLETED, CANCELLED, EXPIRED)
- `numberOfCustodians`: Total custodians (default: 3)
- `threshold`: Minimum shares needed (default: 2)
- `algorithm`: Encryption algorithm (default: "AES-256")
- `keySize`: Key size in bits (default: 256)
- `contributionDeadline`: Deadline for contributions
- `startedAt`, `completedAt`, `cancelledAt`: Timestamps

**Constraints:**
- Threshold must be > 0 and ≤ number of custodians
- Number of custodians must be ≥ 2
- Key size must be 128, 192, or 256

---

### 3. CeremonyCustodian
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `ceremony_custodians`

Links custodians to specific ceremonies with their contribution status.

**Key Fields:**
- `id`: UUID primary key
- `keyCeremony`: ManyToOne relationship
- `keyCustodian`: ManyToOne relationship
- `custodianOrder`: Order in ceremony (1, 2, 3)
- `custodianLabel`: Label (A, B, C)
- `contributionToken`: Unique token for secure contribution
- `contributionLink`: URL for contribution page
- `contributionStatus`: Enum (PENDING, CONTRIBUTED, EXPIRED)
- `contributedAt`: Timestamp when contribution was made
- `shareSentAt`: Timestamp when share was distributed

**Unique Constraints:**
- One custodian per ceremony
- One order per ceremony
- One label per ceremony

---

### 4. PassphraseContribution
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `passphrase_contributions`

Stores custodian passphrase contributions with security metadata.

**Key Fields:**
- `id`: UUID primary key
- `contributionId`: Unique identifier
- `ceremonyCustodian`: ManyToOne relationship
- `passphraseHash`: SHA-256 hash of passphrase
- `passphraseEntropyScore`: Entropy score (0.0-10.0)
- `passphraseStrength`: Enum (WEAK, FAIR, GOOD, STRONG, VERY_STRONG)
- `passphraseLength`: Length of passphrase
- `contributionFingerprint`: Unique fingerprint
- `contributedAt`: Timestamp
- `ipAddress`: Contributor's IP address
- `userAgent`: Browser/client information

**Constraints:**
- Entropy score must be between 0.0 and 10.0
- Passphrase length must be ≥ 16 characters

---

### 5. MasterKey
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `master_keys`

Stores generated HSM master keys with encryption metadata.

**Key Fields:**
- `id`: UUID primary key
- `masterKeyId`: Unique identifier
- `keyCeremony`: ManyToOne relationship
- `keyType`: Type of key (default: "HSM_MASTER_KEY")
- `algorithm`: Encryption algorithm
- `keySize`: Key size in bits
- `keyDataEncrypted`: Encrypted master key (BYTEA)
- `keyFingerprint`: Key fingerprint for verification
- `keyChecksum`: Checksum for integrity
- `combinedEntropyHash`: Hash of combined contributions
- `generationMethod`: Method used (default: "PBKDF2")
- `kdfIterations`: KDF iterations (default: 100,000)
- `kdfSalt`: Salt used in KDF
- `status`: Enum (ACTIVE, ROTATED, REVOKED, EXPIRED)
- `generatedAt`, `activatedAt`, `expiresAt`, `revokedAt`: Timestamps

---

### 6. KeyShare
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `key_shares`

Stores Shamir's Secret Sharing key shares for master key recovery.

**Key Fields:**
- `id`: UUID primary key
- `shareId`: Unique identifier
- `masterKey`: ManyToOne relationship
- `ceremonyCustodian`: ManyToOne relationship
- `shareIndex`: x-value in Shamir's scheme (1, 2, 3)
- `shareDataEncrypted`: Encrypted share data (BYTEA)
- `shareVerificationHash`: Hash for verification
- `polynomialDegree`: Degree of polynomial (threshold - 1)
- `primeModulus`: Prime number used (for educational purposes)
- `generatedAt`, `distributedAt`: Timestamps
- `distributionMethod`: Enum (EMAIL, PHYSICAL, API, MANUAL)
- `usedInRestoration`: Boolean flag
- `lastUsedAt`: Last usage timestamp

**Unique Constraints:**
- One share index per master key
- One share per custodian per master key

---

### 7. CeremonyAuditLog
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `ceremony_audit_logs`

Comprehensive audit trail for all ceremony-related activities.

**Key Fields:**
- `id`: UUID primary key
- `keyCeremony`: ManyToOne relationship (nullable)
- `eventType`: Type of event (e.g., "CEREMONY_CREATED")
- `eventCategory`: Enum (CEREMONY, CONTRIBUTION, KEY_GENERATION, DISTRIBUTION, SECURITY, SYSTEM)
- `eventDescription`: Detailed description
- `actorType`: Enum (ADMINISTRATOR, CUSTODIAN, SYSTEM, API)
- `actorId`, `actorName`: Actor identification
- `targetEntityType`, `targetEntityId`: Target entity information
- `eventStatus`: Enum (SUCCESS, FAILURE, WARNING, INFO)
- `eventSeverity`: Enum (DEBUG, INFO, WARNING, ERROR, CRITICAL)
- `ipAddress`, `userAgent`: Request metadata
- `requestId`, `sessionId`: Session tracking
- `eventMetadata`: JSONB for flexible additional data
- `errorMessage`, `stackTrace`: Error details
- `createdAt`: Timestamp

**Special Features:**
- JSONB support for flexible metadata storage
- GIN index on event_metadata for efficient querying

---

### 8. CeremonyStatistics
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `ceremony_statistics`

Aggregated statistics for ceremony monitoring and reporting.

**Key Fields:**
- `id`: UUID primary key
- `keyCeremony`: OneToOne relationship
- `totalCustodians`: Total number of custodians
- `contributionsReceived`: Count of received contributions
- `contributionsPending`: Count of pending contributions
- `sharesGenerated`: Count of generated shares
- `sharesDistributed`: Count of distributed shares
- `averageContributionTimeMinutes`: Average time for contributions
- `totalDurationMinutes`: Total ceremony duration
- `ceremonyCompletionPercentage`: Completion percentage (0.00-100.00)
- `lastActivityAt`: Last activity timestamp
- `lastUpdatedAt`: Auto-updated timestamp

---

### 9. ContributionReminder
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `contribution_reminders`

Tracks reminder emails sent to custodians.

**Key Fields:**
- `id`: UUID primary key
- `ceremonyCustodian`: ManyToOne relationship
- `reminderType`: Enum (INITIAL, FIRST_REMINDER, SECOND_REMINDER, URGENT, DEADLINE_APPROACHING)
- `sentAt`: When reminder was sent
- `sentBy`: Who sent the reminder
- `emailStatus`: Enum (SENT, DELIVERED, BOUNCED, FAILED, OPENED)
- `deliveryConfirmedAt`: When delivery was confirmed

---

### 10. KeyRestorationRequest
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `key_restoration_requests`

Tracks requests to restore master keys using key shares.

**Key Fields:**
- `id`: UUID primary key
- `restorationId`: Unique identifier
- `keyCeremonyOriginal`: ManyToOne to original ceremony
- `masterKey`: ManyToOne to master key
- `restorationReason`: Reason for restoration
- `requestedBy`: Who requested restoration
- `requestedAt`: When requested
- `approvedBy`, `approvedAt`: Approval information
- `status`: Enum (PENDING, APPROVED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED)
- `sharesRequired`: Number of shares needed
- `sharesSubmitted`: Number of shares submitted
- `restorationCompletedAt`: Completion timestamp
- `restoredKeyVerified`: Boolean verification flag

**Constraints:**
- Shares required must be ≥ 2

---

### 11. RestorationShareSubmission
**Package:** `com.artivisi.hsm.simulator.entity`  
**Table:** `restoration_share_submissions`

Tracks individual share submissions during restoration ceremonies.

**Key Fields:**
- `id`: UUID primary key
- `keyRestorationRequest`: ManyToOne relationship
- `keyShare`: ManyToOne relationship
- `ceremonyCustodian`: ManyToOne relationship
- `submittedAt`: Submission timestamp
- `verificationStatus`: Enum (PENDING, VERIFIED, REJECTED)
- `verifiedAt`: Verification timestamp
- `ipAddress`: Submitter's IP address

**Unique Constraints:**
- One share per restoration request

---

## Key Features

### Lombok Integration
All entities use Lombok annotations:
- `@Data`: Generates getters, setters, toString, equals, and hashCode
- `@Builder`: Provides builder pattern
- `@NoArgsConstructor`, `@AllArgsConstructor`: Constructor generation

### Automatic Timestamps
- `@CreationTimestamp`: Automatically sets creation time
- `@UpdateTimestamp`: Automatically updates modification time

### JPA Best Practices
- UUID primary keys for distributed systems
- Proper indexes for performance
- Foreign key constraints with cascade rules
- Enum types for fixed value sets
- Proper fetch strategies (LAZY for relationships)
- Unique constraints for business rules

### Database Constraints Mirrored
All database constraints from the Flyway migration are properly represented:
- Check constraints documented in comments
- Unique constraints as JPA annotations
- Foreign key relationships properly defined
- Default values using `@Builder.Default`

## Usage Example

```java
// Create a new key custodian
KeyCustodian custodian = KeyCustodian.builder()
    .custodianId("CUST-A-001")
    .fullName("Alice Johnson")
    .email("alice.johnson@example.com")
    .phone("+62-812-3456-7890")
    .department("IT Security")
    .status(KeyCustodian.CustodianStatus.ACTIVE)
    .createdBy("system")
    .build();

// Create a new key ceremony
KeyCeremony ceremony = KeyCeremony.builder()
    .ceremonyId("CEREMONY-001")
    .ceremonyName("Initial HSM Key Setup")
    .purpose("Generate initial HSM master key")
    .ceremonyType(KeyCeremony.CeremonyType.INITIALIZATION)
    .numberOfCustodians(3)
    .threshold(2)
    .algorithm("AES-256")
    .keySize(256)
    .createdBy("admin")
    .build();
```

## Testing

These entities are designed to work with:
- **Testcontainers**: For integration testing with PostgreSQL
- **H2**: For fast unit testing (with PostgreSQL compatibility mode)
- **Flyway**: Automatic schema migration and validation

## Next Steps

1. **Create Repository interfaces** for each entity
2. **Create Service layer** for business logic
3. **Create REST Controllers** for API endpoints
4. **Add validation annotations** (e.g., `@NotNull`, `@Size`)
5. **Implement security** for sensitive data handling
6. **Add audit listeners** for automatic audit logging

## Notes

- All sensitive data (passphrases, keys, shares) are stored as hashes or encrypted
- JSONB support in `CeremonyAuditLog` allows flexible event metadata
- Proper cascading ensures data integrity during deletions
- Indexes are strategically placed for query performance
