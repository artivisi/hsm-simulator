# HSM Simulator - Database Schema Documentation

## Overview

This document describes the database schema for the HSM Simulator Key Ceremony functionality. The schema supports a 2-of-3 threshold scheme using Shamir's Secret Sharing for master key generation and recovery.

## Database Diagram

```mermaid
erDiagram
    key_custodians ||--o{ ceremony_custodians : "participates in"
    key_ceremonies ||--o{ ceremony_custodians : "has"
    ceremony_custodians ||--o| passphrase_contributions : "contributes"
    ceremony_custodians ||--o| key_shares : "receives"
    ceremony_custodians ||--o{ contribution_reminders : "receives"
    ceremony_custodians ||--o{ restoration_share_submissions : "submits in"
    
    key_ceremonies ||--o| master_keys : "generates"
    key_ceremonies ||--o| ceremony_statistics : "tracks"
    key_ceremonies ||--o{ ceremony_audit_logs : "logs"
    key_ceremonies ||--o{ key_restoration_requests : "may restore"
    
    master_keys ||--o{ key_shares : "split into"
    master_keys ||--o{ key_restoration_requests : "restored from"
    
    key_restoration_requests ||--o{ restoration_share_submissions : "receives"
    key_shares ||--o{ restoration_share_submissions : "used in"

    key_custodians {
        uuid id PK
        varchar custodian_id UK "Business ID"
        varchar full_name
        varchar email UK
        varchar phone
        varchar department
        varchar status "ACTIVE, INACTIVE, SUSPENDED"
        timestamp created_at
        timestamp updated_at
        varchar created_by
    }

    key_ceremonies {
        uuid id PK
        varchar ceremony_id UK "Business ID"
        varchar ceremony_name
        text purpose
        varchar ceremony_type "INITIALIZATION, RESTORATION, KEY_ROTATION"
        varchar status "PENDING, AWAITING_CONTRIBUTIONS, etc"
        integer number_of_custodians "Default: 3"
        integer threshold "Default: 2"
        varchar algorithm "Default: AES-256"
        integer key_size "Default: 256"
        timestamp contribution_deadline
        timestamp started_at
        timestamp completed_at
        timestamp cancelled_at
        text cancellation_reason
        varchar created_by
        varchar last_modified_by
    }

    ceremony_custodians {
        uuid id PK
        uuid id_key_ceremony FK
        uuid id_key_custodian FK
        integer custodian_order "1, 2, 3"
        varchar custodian_label "A, B, C"
        varchar contribution_token UK
        text contribution_link
        timestamp invitation_sent_at
        varchar contribution_status "PENDING, CONTRIBUTED, EXPIRED"
        timestamp contributed_at
        timestamp share_sent_at
        timestamp created_at
    }

    passphrase_contributions {
        uuid id PK
        varchar contribution_id UK
        uuid id_ceremony_custodian FK
        varchar passphrase_hash "SHA-256"
        decimal passphrase_entropy_score "0.0-10.0"
        varchar passphrase_strength "WEAK, FAIR, GOOD, STRONG, VERY_STRONG"
        integer passphrase_length "Min: 16"
        varchar contribution_fingerprint
        timestamp contributed_at
        varchar ip_address "IPv4/IPv6"
        text user_agent
    }

    master_keys {
        uuid id PK
        varchar master_key_id UK
        uuid id_key_ceremony FK
        varchar key_type "HSM_MASTER_KEY"
        varchar algorithm
        integer key_size "128, 192, 256"
        bytea key_data_encrypted
        varchar key_fingerprint
        varchar key_checksum
        varchar combined_entropy_hash
        varchar generation_method "PBKDF2"
        integer kdf_iterations "Default: 100000"
        varchar kdf_salt
        varchar status "ACTIVE, ROTATED, REVOKED, EXPIRED"
        timestamp generated_at
        timestamp activated_at
        timestamp expires_at
        timestamp revoked_at
        text revocation_reason
    }

    key_shares {
        uuid id PK
        varchar share_id UK
        uuid id_master_key FK
        uuid id_ceremony_custodian FK
        integer share_index "1, 2, 3"
        bytea share_data_encrypted
        varchar share_verification_hash
        integer polynomial_degree "threshold - 1"
        text prime_modulus "Educational"
        timestamp generated_at
        timestamp distributed_at
        varchar distribution_method "EMAIL, PHYSICAL, API, MANUAL"
        boolean used_in_restoration
        timestamp last_used_at
    }

    ceremony_audit_logs {
        uuid id PK
        uuid id_key_ceremony FK "Nullable"
        varchar event_type
        varchar event_category "CEREMONY, CONTRIBUTION, KEY_GENERATION, etc"
        text event_description
        varchar actor_type "ADMINISTRATOR, CUSTODIAN, SYSTEM, API"
        varchar actor_id
        varchar actor_name
        varchar target_entity_type
        varchar target_entity_id
        varchar event_status "SUCCESS, FAILURE, WARNING, INFO"
        varchar event_severity "DEBUG, INFO, WARNING, ERROR, CRITICAL"
        varchar ip_address
        text user_agent
        varchar request_id
        varchar session_id
        jsonb event_metadata
        text error_message
        text stack_trace
        timestamp created_at
    }

    ceremony_statistics {
        uuid id PK
        uuid id_key_ceremony FK
        integer total_custodians
        integer contributions_received
        integer contributions_pending
        integer shares_generated
        integer shares_distributed
        integer average_contribution_time_minutes
        integer total_duration_minutes
        decimal ceremony_completion_percentage "0.00-100.00"
        timestamp last_activity_at
        timestamp last_updated_at
    }

    contribution_reminders {
        uuid id PK
        uuid id_ceremony_custodian FK
        varchar reminder_type "INITIAL, FIRST_REMINDER, SECOND_REMINDER, etc"
        timestamp sent_at
        varchar sent_by
        varchar email_status "SENT, DELIVERED, BOUNCED, FAILED, OPENED"
        timestamp delivery_confirmed_at
    }

    key_restoration_requests {
        uuid id PK
        varchar restoration_id UK
        uuid id_key_ceremony_original FK
        uuid id_master_key FK
        text restoration_reason
        varchar requested_by
        timestamp requested_at
        varchar approved_by
        timestamp approved_at
        varchar status "PENDING, APPROVED, IN_PROGRESS, COMPLETED, etc"
        integer shares_required "Min: 2"
        integer shares_submitted
        timestamp restoration_completed_at
        boolean restored_key_verified
    }

    restoration_share_submissions {
        uuid id PK
        uuid id_key_restoration_request FK
        uuid id_key_share FK
        uuid id_ceremony_custodian FK
        timestamp submitted_at
        varchar verification_status "PENDING, VERIFIED, REJECTED"
        timestamp verified_at
        varchar ip_address
    }
```

## Key Features

- **UUID Primary Keys**: All tables use UUID as primary keys for better distribution and security
- **FK Naming Convention**: Foreign keys follow the pattern `id_tablename`
- **Audit Trail**: Comprehensive logging of all ceremony activities
- **Statistics Tracking**: Real-time ceremony progress monitoring
- **Restoration Support**: Key restoration using threshold scheme

## Ceremony Workflow Diagram

```mermaid
sequenceDiagram
    participant Admin as Administrator
    participant KC as key_ceremonies
    participant CC as ceremony_custodians
    participant Cust as key_custodians
    participant PC as passphrase_contributions
    participant MK as master_keys
    participant KS as key_shares
    participant CS as ceremony_statistics
    participant CAL as ceremony_audit_logs

    Note over Admin,CAL: Phase 1: Ceremony Setup
    Admin->>KC: Create ceremony record
    KC->>CAL: Log CEREMONY_CREATED
    Admin->>CC: Register custodians (A, B, C)
    CC->>Cust: Link to custodians
    CC->>CAL: Log CUSTODIAN_ADDED
    Admin->>CC: Generate contribution tokens
    CC->>Cust: Send invitation emails
    CC->>CAL: Log INVITATION_SENT
    KC->>CS: Initialize statistics

    Note over Admin,CAL: Phase 2: Contributions
    Cust->>CC: Access contribution link
    Cust->>PC: Submit passphrase
    PC->>CAL: Log CONTRIBUTION_RECEIVED
    PC->>CC: Update contribution_status
    CC->>CS: Update contribution count
    
    Note over Admin,CAL: Phase 3: Key Generation (after all contributions)
    KC->>MK: Generate master key from combined passphrases
    MK->>CAL: Log MASTER_KEY_GENERATED
    MK->>KS: Generate Shamir shares (2-of-3)
    KS->>CC: Assign shares to custodians
    KS->>CAL: Log KEY_SHARE_GENERATED
    KS->>Cust: Send shares via email
    KS->>CAL: Log KEY_SHARE_DISTRIBUTED
    KC->>CS: Update ceremony completion

    Note over Admin,CAL: Phase 4: Completion
    KC->>KC: Update status to COMPLETED
    KC->>CAL: Log CEREMONY_COMPLETED
    CS->>CS: Finalize statistics
```

## Database Tables

### 1. key_custodians

Stores information about individuals who serve as key custodians.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `custodian_id` (VARCHAR(50), UNIQUE): Business identifier
- `full_name` (VARCHAR(255)): Full name
- `email` (VARCHAR(255), UNIQUE): Email address
- `phone` (VARCHAR(50)): Phone number
- `department` (VARCHAR(100)): Department
- `status` (VARCHAR(20)): Status (ACTIVE, INACTIVE, SUSPENDED)
- `created_at` (TIMESTAMP): Creation timestamp
- `updated_at` (TIMESTAMP): Last update timestamp
- `created_by` (VARCHAR(100)): Creator identifier

**Indexes:**
- `idx_custodian_email` on `email`
- `idx_custodian_status` on `status`

---

### 2. key_ceremonies

Tracks the lifecycle and configuration of key ceremonies.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `ceremony_id` (VARCHAR(50), UNIQUE): Business identifier
- `ceremony_name` (VARCHAR(255)): Ceremony name
- `purpose` (TEXT): Purpose description
- `ceremony_type` (VARCHAR(50)): Type (INITIALIZATION, RESTORATION, KEY_ROTATION)
- `status` (VARCHAR(50)): Current status
- `number_of_custodians` (INTEGER): Total custodians (default: 3)
- `threshold` (INTEGER): Minimum shares needed (default: 2)
- `algorithm` (VARCHAR(50)): Encryption algorithm (default: AES-256)
- `key_size` (INTEGER): Key size in bits (default: 256)
- `contribution_deadline` (TIMESTAMP): Deadline for contributions
- `started_at` (TIMESTAMP): Start time
- `completed_at` (TIMESTAMP): Completion time
- `cancelled_at` (TIMESTAMP): Cancellation time
- `cancellation_reason` (TEXT): Reason for cancellation
- `created_by` (VARCHAR(100)): Creator
- `last_modified_by` (VARCHAR(100)): Last modifier

**Status Values:**
- PENDING
- AWAITING_CONTRIBUTIONS
- PARTIAL_CONTRIBUTIONS
- GENERATING_KEY
- COMPLETED
- CANCELLED
- EXPIRED

**Indexes:**
- `idx_ceremony_status` on `status`
- `idx_ceremony_type` on `ceremony_type`
- `idx_ceremony_created_at` on `started_at`

---

### 3. ceremony_custodians

Links custodians to ceremonies with their specific roles and contribution status.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `id_key_ceremony` (UUID, FK): References key_ceremonies
- `id_key_custodian` (UUID, FK): References key_custodians
- `custodian_order` (INTEGER): Order in ceremony (1, 2, 3)
- `custodian_label` (VARCHAR(10)): Label (A, B, C)
- `contribution_token` (VARCHAR(255), UNIQUE): Unique contribution token
- `contribution_link` (TEXT): Contribution URL
- `invitation_sent_at` (TIMESTAMP): Invitation sent time
- `contribution_status` (VARCHAR(50)): Status (PENDING, CONTRIBUTED, EXPIRED)
- `contributed_at` (TIMESTAMP): Contribution time
- `share_sent_at` (TIMESTAMP): Share distribution time
- `created_at` (TIMESTAMP): Creation time

**Unique Constraints:**
- `(id_key_ceremony, id_key_custodian)`
- `(id_key_ceremony, custodian_order)`
- `(id_key_ceremony, custodian_label)`

**Indexes:**
- `idx_ceremony_custodians_ceremony` on `id_key_ceremony`
- `idx_ceremony_custodians_custodian` on `id_key_custodian`
- `idx_ceremony_custodians_token` on `contribution_token`
- `idx_ceremony_custodians_status` on `contribution_status`

---

### 4. passphrase_contributions

Stores custodian passphrase contributions with security metadata.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `contribution_id` (VARCHAR(50), UNIQUE): Business identifier
- `id_ceremony_custodian` (UUID, FK): References ceremony_custodians
- `passphrase_hash` (VARCHAR(255)): SHA-256 hash
- `passphrase_entropy_score` (DECIMAL(3,1)): Score 0.0-10.0
- `passphrase_strength` (VARCHAR(20)): Strength level
- `passphrase_length` (INTEGER): Length in characters (min: 16)
- `contribution_fingerprint` (VARCHAR(255)): Fingerprint
- `contributed_at` (TIMESTAMP): Contribution time
- `ip_address` (VARCHAR(45)): IP address (IPv4/IPv6)
- `user_agent` (TEXT): User agent string

**Strength Values:**
- WEAK
- FAIR
- GOOD
- STRONG
- VERY_STRONG

**Indexes:**
- `idx_contributions_ceremony_custodian` on `id_ceremony_custodian`
- `idx_contributions_timestamp` on `contributed_at`

---

### 5. master_keys

Stores generated HSM master keys with metadata.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `master_key_id` (VARCHAR(50), UNIQUE): Business identifier
- `id_key_ceremony` (UUID, FK): References key_ceremonies
- `key_type` (VARCHAR(50)): Key type (default: HSM_MASTER_KEY)
- `algorithm` (VARCHAR(50)): Encryption algorithm
- `key_size` (INTEGER): Key size in bits
- `key_data_encrypted` (BYTEA): Encrypted master key data
- `key_fingerprint` (VARCHAR(255)): Key fingerprint
- `key_checksum` (VARCHAR(255)): Key checksum
- `combined_entropy_hash` (VARCHAR(255)): Hash of combined contributions
- `generation_method` (VARCHAR(50)): Method (default: PBKDF2)
- `kdf_iterations` (INTEGER): KDF iterations (default: 100000)
- `kdf_salt` (VARCHAR(255)): KDF salt
- `status` (VARCHAR(20)): Status (ACTIVE, ROTATED, REVOKED, EXPIRED)
- `generated_at` (TIMESTAMP): Generation time
- `activated_at` (TIMESTAMP): Activation time
- `expires_at` (TIMESTAMP): Expiration time
- `revoked_at` (TIMESTAMP): Revocation time
- `revocation_reason` (TEXT): Revocation reason

**Indexes:**
- `idx_master_keys_ceremony` on `id_key_ceremony`
- `idx_master_keys_status` on `status`
- `idx_master_keys_fingerprint` on `key_fingerprint`

---

### 6. key_shares

Stores Shamir's Secret Sharing key shares for master key recovery.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `share_id` (VARCHAR(50), UNIQUE): Business identifier
- `id_master_key` (UUID, FK): References master_keys
- `id_ceremony_custodian` (UUID, FK): References ceremony_custodians
- `share_index` (INTEGER): x value in Shamir's scheme (1, 2, 3)
- `share_data_encrypted` (BYTEA): Encrypted share data
- `share_verification_hash` (VARCHAR(255)): Verification hash
- `polynomial_degree` (INTEGER): Polynomial degree (threshold - 1)
- `prime_modulus` (TEXT): Prime number for educational purposes
- `generated_at` (TIMESTAMP): Generation time
- `distributed_at` (TIMESTAMP): Distribution time
- `distribution_method` (VARCHAR(50)): Method (EMAIL, PHYSICAL, API, MANUAL)
- `used_in_restoration` (BOOLEAN): Used in restoration flag
- `last_used_at` (TIMESTAMP): Last use time

**Unique Constraints:**
- `(id_master_key, share_index)`
- `(id_master_key, id_ceremony_custodian)`

**Indexes:**
- `idx_key_shares_master_key` on `id_master_key`
- `idx_key_shares_custodian` on `id_ceremony_custodian`
- `idx_key_shares_used` on `used_in_restoration`

---

### 7. ceremony_audit_logs

Comprehensive audit trail for all ceremony-related activities.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `id_key_ceremony` (UUID, FK, NULLABLE): References key_ceremonies
- `event_type` (VARCHAR(100)): Event type
- `event_category` (VARCHAR(50)): Category
- `event_description` (TEXT): Description
- `actor_type` (VARCHAR(50)): Actor type
- `actor_id` (VARCHAR(100)): Actor identifier
- `actor_name` (VARCHAR(255)): Actor name
- `target_entity_type` (VARCHAR(50)): Target type
- `target_entity_id` (VARCHAR(100)): Target identifier
- `event_status` (VARCHAR(20)): Status
- `event_severity` (VARCHAR(20)): Severity level
- `ip_address` (VARCHAR(45)): IP address
- `user_agent` (TEXT): User agent
- `request_id` (VARCHAR(100)): Request ID
- `session_id` (VARCHAR(100)): Session ID
- `event_metadata` (JSONB): Additional metadata
- `error_message` (TEXT): Error message
- `stack_trace` (TEXT): Stack trace
- `created_at` (TIMESTAMP): Creation time

**Event Categories:**
- CEREMONY
- CONTRIBUTION
- KEY_GENERATION
- DISTRIBUTION
- SECURITY
- SYSTEM

**Indexes:**
- `idx_audit_ceremony` on `id_key_ceremony`
- `idx_audit_event_type` on `event_type`
- `idx_audit_event_category` on `event_category`
- `idx_audit_created_at` on `created_at`
- `idx_audit_actor` on `actor_id`
- `idx_audit_event_status` on `event_status`
- `idx_audit_event_severity` on `event_severity`
- `idx_audit_metadata` on `event_metadata` (GIN index)

---

### 8. ceremony_statistics

Stores aggregated statistics for monitoring and reporting.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `id_key_ceremony` (UUID, FK, UNIQUE): References key_ceremonies
- `total_custodians` (INTEGER): Total custodians
- `contributions_received` (INTEGER): Received contributions
- `contributions_pending` (INTEGER): Pending contributions
- `shares_generated` (INTEGER): Generated shares
- `shares_distributed` (INTEGER): Distributed shares
- `average_contribution_time_minutes` (INTEGER): Average response time
- `total_duration_minutes` (INTEGER): Total duration
- `ceremony_completion_percentage` (DECIMAL(5,2)): Completion percentage
- `last_activity_at` (TIMESTAMP): Last activity time
- `last_updated_at` (TIMESTAMP): Last update time

**Indexes:**
- `idx_stats_ceremony` (UNIQUE) on `id_key_ceremony`

---

### 9. contribution_reminders

Tracks reminder emails sent to custodians.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `id_ceremony_custodian` (UUID, FK): References ceremony_custodians
- `reminder_type` (VARCHAR(50)): Reminder type
- `sent_at` (TIMESTAMP): Sent time
- `sent_by` (VARCHAR(100)): Sender identifier
- `email_status` (VARCHAR(20)): Email status
- `delivery_confirmed_at` (TIMESTAMP): Delivery confirmation time

**Reminder Types:**
- INITIAL
- FIRST_REMINDER
- SECOND_REMINDER
- URGENT
- DEADLINE_APPROACHING

**Indexes:**
- `idx_reminders_ceremony_custodian` on `id_ceremony_custodian`
- `idx_reminders_sent_at` on `sent_at`

---

### 10. key_restoration_requests

Tracks requests to restore master keys using key shares.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `restoration_id` (VARCHAR(50), UNIQUE): Business identifier
- `id_key_ceremony_original` (UUID, FK): References original ceremony
- `id_master_key` (UUID, FK): References master key
- `restoration_reason` (TEXT): Reason for restoration
- `requested_by` (VARCHAR(100)): Requester identifier
- `requested_at` (TIMESTAMP): Request time
- `approved_by` (VARCHAR(100)): Approver identifier
- `approved_at` (TIMESTAMP): Approval time
- `status` (VARCHAR(50)): Status
- `shares_required` (INTEGER): Minimum shares needed (≥2)
- `shares_submitted` (INTEGER): Shares submitted
- `restoration_completed_at` (TIMESTAMP): Completion time
- `restored_key_verified` (BOOLEAN): Verification flag

**Status Values:**
- PENDING
- APPROVED
- IN_PROGRESS
- COMPLETED
- REJECTED
- CANCELLED

**Indexes:**
- `idx_restoration_original_ceremony` on `id_key_ceremony_original`
- `idx_restoration_master_key` on `id_master_key`
- `idx_restoration_status` on `status`

---

### 11. restoration_share_submissions

Tracks share submissions during restoration ceremonies.

**Columns:**
- `id` (UUID, PK): Unique identifier
- `id_key_restoration_request` (UUID, FK): References restoration request
- `id_key_share` (UUID, FK): References key share
- `id_ceremony_custodian` (UUID, FK): References custodian
- `submitted_at` (TIMESTAMP): Submission time
- `verification_status` (VARCHAR(20)): Verification status
- `verified_at` (TIMESTAMP): Verification time
- `ip_address` (VARCHAR(45)): IP address

**Verification Status:**
- PENDING
- VERIFIED
- REJECTED

**Unique Constraints:**
- `(id_key_restoration_request, id_key_share)`

**Indexes:**
- `idx_restoration_submissions` on `id_key_restoration_request`
- `idx_restoration_submissions_share` on `id_key_share`
- `idx_restoration_submissions_custodian` on `id_ceremony_custodian`

---

## Entity Relationships

### Core Ceremony Flow

```mermaid
graph TB
    subgraph "Ceremony Setup"
        KC[key_ceremonies]
        KCUST[key_custodians]
        CC[ceremony_custodians]
        
        KC -->|has custodians| CC
        KCUST -->|participates in| CC
    end
    
    subgraph "Contribution Phase"
        CC -->|contributes| PC[passphrase_contributions]
        CC -->|receives reminders| CR[contribution_reminders]
    end
    
    subgraph "Key Generation"
        KC -->|generates| MK[master_keys]
        MK -->|split into| KS[key_shares]
        KS -->|assigned to| CC
    end
    
    subgraph "Monitoring & Audit"
        KC -->|tracks| CS[ceremony_statistics]
        KC -->|logs events| CAL[ceremony_audit_logs]
    end
    
    subgraph "Key Restoration"
        KC -->|may need| KRR[key_restoration_requests]
        MK -->|restored via| KRR
        KRR -->|receives| RSS[restoration_share_submissions]
        KS -->|used in| RSS
        CC -->|submits to| RSS
    end

    style KC fill:#e1f5fe
    style MK fill:#f3e5f5
    style KS fill:#e8f5e9
    style PC fill:#fff3e0
    style KRR fill:#fce4ec
```

### Detailed Relationships

| Parent Table | Relationship | Child Table | FK Column |
|--------------|-------------|-------------|-----------|
| key_custodians | 1:N | ceremony_custodians | id_key_custodian |
| key_ceremonies | 1:N | ceremony_custodians | id_key_ceremony |
| ceremony_custodians | 1:1 | passphrase_contributions | id_ceremony_custodian |
| ceremony_custodians | 1:1 | key_shares | id_ceremony_custodian |
| ceremony_custodians | 1:N | contribution_reminders | id_ceremony_custodian |
| ceremony_custodians | 1:N | restoration_share_submissions | id_ceremony_custodian |
| key_ceremonies | 1:1 | master_keys | id_key_ceremony |
| key_ceremonies | 1:1 | ceremony_statistics | id_key_ceremony |
| key_ceremonies | 1:N | ceremony_audit_logs | id_key_ceremony |
| key_ceremonies | 1:N | key_restoration_requests | id_key_ceremony_original |
| master_keys | 1:N | key_shares | id_master_key |
| master_keys | 1:N | key_restoration_requests | id_master_key |
| key_restoration_requests | 1:N | restoration_share_submissions | id_key_restoration_request |
| key_shares | 1:N | restoration_share_submissions | id_key_share |

## Foreign Key Naming Convention

All foreign keys follow the pattern `id_tablename`:
- `id_key_ceremony` → references `key_ceremonies.id`
- `id_key_custodian` → references `key_custodians.id`
- `id_ceremony_custodian` → references `ceremony_custodians.id`
- `id_master_key` → references `master_keys.id`
- `id_key_share` → references `key_shares.id`
- `id_key_restoration_request` → references `key_restoration_requests.id`
- `id_key_ceremony_original` → references original `key_ceremonies.id`

## Sample Data

The migration includes 3 sample custodians for testing:

| custodian_id | full_name       | email                        | department   |
|--------------|-----------------|------------------------------|--------------|
| CUST-A-001   | Alice Johnson   | alice.johnson@example.com    | IT Security  |
| CUST-B-002   | Bob Williams    | bob.williams@example.com     | IT Security  |
| CUST-C-003   | Carol Martinez  | carol.martinez@example.com   | IT Security  |

## Business Logic

All business logic (triggers, validations, statistics calculations) will be handled in Java code. The database schema provides the data structure only.

## Migration Files

1. **V1__create_key_ceremony_tables.sql**: Creates all tables with proper constraints
2. **V2__insert_sample_key_custodians.sql**: Inserts sample test data

---

*Last Updated: October 14, 2025*
*Version: 1.0*
