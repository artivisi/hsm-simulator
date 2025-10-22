# HSM Simulator - Database Schema Documentation

## Overview

This document describes the comprehensive database schema for the HSM Simulator, covering:

- **Key Ceremony System**: 2-of-3 threshold scheme using Shamir's Secret Sharing
- **Four-Party Card Processing Model**: Banks (Issuer, Acquirer, Switch, Processor) with terminal management
- **Banking Key Management**: Complete lifecycle for TMK, TPK, TSK, ZMK, ZPK, ZSK keys
- **Key Hierarchy**: Parent-child relationships for key encryption keys
- **Inter-Bank Operations**: Zone key exchange and rotation tracking

## Database Tables Summary

| Table | Purpose | Key Features |
|-------|---------|--------------|
| **Banking Infrastructure** |||
| `banks` | Bank/organization data | Four-party model support |
| `terminals` | Terminal devices | ATM, POS, MPOS, E-commerce |
| **Key Management** |||
| `master_keys` | All cryptographic keys | Unified table for all key types, hierarchy support |
| `zone_key_exchanges` | Inter-bank key exchange | ZMK, ZPK, ZSK distribution |
| `key_rotation_history` | Key rotation audit | Complete rotation tracking |
| **Key Ceremony** |||
| `key_custodians` | Custodian information | Personnel authorized for ceremonies |
| `key_ceremonies` | Ceremony orchestration | Initialization, restoration, rotation |
| `ceremony_custodians` | Custodian assignments | Links custodians to ceremonies |
| `passphrase_contributions` | Entropy contributions | Custodian passphrases for key generation |
| `key_shares` | Shamir's secret shares | 2-of-3 threshold scheme |
| `key_restoration_requests` | Key recovery requests | Master key restoration |
| `restoration_share_submissions` | Share submissions | Restoration process tracking |
| **Monitoring & Audit** |||
| `ceremony_audit_logs` | Audit trail | Comprehensive event logging |
| `ceremony_statistics` | Ceremony metrics | Progress and performance tracking |
| `contribution_reminders` | Reminder tracking | Email notification history |

## Core Tables

### banks

Stores bank/organization information for the four-party card processing model.

**Key Columns:**
- `bank_code` (UNIQUE): Bank identifier
- `bank_type`: ISSUER, ACQUIRER, SWITCH, PROCESSOR
- `status`: ACTIVE, INACTIVE, SUSPENDED

---

### terminals

Terminal devices (ATM, POS, etc.) for terminal key management.

**Key Columns:**
- `terminal_id` (UNIQUE): Terminal identifier
- `id_bank` (FK → banks): Owning bank
- `terminal_type`: ATM, POS, MPOS, VIRTUAL, E_COMMERCE
- `status`: ACTIVE, INACTIVE, SUSPENDED, MAINTENANCE

---

### master_keys

**Unified table storing ALL cryptographic keys** with hierarchy and rotation support.

**Key Columns:**
- `master_key_id` (UNIQUE): Business identifier
- `id_key_ceremony` (FK → key_ceremonies, NULLABLE): For HSM master keys from ceremony
- `id_bank` (FK → banks, NULLABLE): For bank-specific keys
- `id_terminal` (FK → terminals, NULLABLE): For terminal-specific keys
- `parent_key_id` (FK → master_keys, NULLABLE): Parent key in hierarchy (self-reference)
- `key_type`: HSM_MASTER_KEY, TMK, TPK, TSK, ZMK, ZPK, ZSK, LMK, KEK
- `key_usage`: PIN_ENCRYPTION, DATA_ENCRYPTION, KEY_ENCRYPTION, MAC_GENERATION, SESSION, MASTER
- `key_data_encrypted` (BYTEA): Encrypted key material
- `status`: ACTIVE, ROTATED, REVOKED, EXPIRED
- `rotated_from_key_id` (FK → master_keys, NULLABLE): Previous key in rotation chain (self-reference)

**Key Types:**
- **HSM_MASTER_KEY**: Root key generated through key ceremony
- **TMK** (Terminal Master Key): Encrypts key distribution to terminals
- **TPK** (Terminal PIN Key): Encrypts PIN blocks at terminal (child of TMK)
- **TSK** (Terminal Security Key): MAC and authentication (child of TMK)
- **ZMK** (Zone Master Key): Encrypts inter-bank key exchanges
- **ZPK** (Zone PIN Key): Protects PIN data between banks (child of ZMK)
- **ZSK** (Zone Session Key): Encrypts inter-bank transaction data (child of ZMK)
- **LMK** (Local Master Key): Bank-specific master key
- **KEK** (Key Encryption Key): Generic key encryption key

**Key Hierarchy Examples:**
- TMK (parent) → TPK (child): TMK encrypts TPK for terminal distribution
- TMK (parent) → TSK (child): TMK encrypts TSK for terminal distribution
- ZMK (parent) → ZPK (child): ZMK encrypts ZPK for inter-bank exchange
- ZMK (parent) → ZSK (child): ZMK encrypts ZSK for inter-bank exchange

**Rotation Chain:**
- `rotated_from_key_id` links to previous version of the same key
- Old key marked as ROTATED status
- New key becomes ACTIVE

---

### zone_key_exchanges

Tracks inter-bank zone key exchanges (ZMK, ZPK, ZSK) for the four-party model.

**Key Columns:**
- `id_source_bank` (FK → banks): Sending bank
- `id_destination_bank` (FK → banks): Receiving bank
- `id_zmk` (FK → master_keys): Zone Master Key used for exchange
- `id_exchanged_key` (FK → master_keys): The ZPK or ZSK being exchanged
- `exchange_type`: INITIAL, RENEWAL, EMERGENCY, ROTATION
- `key_transport_method`: ENCRYPTED_UNDER_ZMK, MANUAL, COURIER, SECURE_CHANNEL
- `exchange_status`: INITIATED, IN_TRANSIT, ACKNOWLEDGED, ACTIVATED, REJECTED, EXPIRED

---

### key_rotation_history

Complete audit trail of key rotation activities for compliance.

**Key Columns:**
- `id_old_key` (FK → master_keys): Key being rotated
- `id_new_key` (FK → master_keys): Replacement key
- `rotation_type`: SCHEDULED, EMERGENCY, COMPLIANCE, COMPROMISE, EXPIRATION
- `rotation_status`: IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK, CANCELLED
- `affected_terminals_count`: Number of terminals updated
- `affected_banks_count`: Number of banks affected

---

### key_custodians

Individuals authorized to hold key shares in ceremonies.

**Key Columns:**
- `custodian_id` (UNIQUE): Business identifier
- `full_name`, `email`, `phone`, `department`
- `status`: ACTIVE, INACTIVE, SUSPENDED

---

### key_ceremonies

Orchestrates key initialization/restoration process using Shamir's Secret Sharing.

**Key Columns:**
- `ceremony_type`: INITIALIZATION, RESTORATION, KEY_ROTATION
- `status`: PENDING, AWAITING_CONTRIBUTIONS, PARTIAL_CONTRIBUTIONS, GENERATING_KEY, COMPLETED, CANCELLED, EXPIRED
- `number_of_custodians`: Total custodians (default: 3)
- `threshold`: Minimum shares needed (default: 2)
- `algorithm`: Encryption algorithm (default: AES-256)

---

### ceremony_custodians

Links custodians to specific ceremonies with their contribution status.

**Key Columns:**
- `id_key_ceremony` (FK → key_ceremonies)
- `id_key_custodian` (FK → key_custodians)
- `custodian_order`: Order in ceremony (1, 2, 3)
- `custodian_label`: Label (A, B, C)
- `contribution_token` (UNIQUE): Secure contribution link token
- `contribution_status`: PENDING, CONTRIBUTED, EXPIRED

---

### passphrase_contributions

Stores custodian passphrase contributions with security metadata.

**Key Columns:**
- `id_ceremony_custodian` (FK → ceremony_custodians)
- `passphrase_hash`: SHA-256 hash (never stores plaintext)
- `passphrase_entropy_score`: 0.0-10.0
- `passphrase_strength`: WEAK, FAIR, GOOD, STRONG, VERY_STRONG

---

### key_shares

Shamir's Secret Sharing key shares for master key recovery.

**Key Columns:**
- `id_master_key` (FK → master_keys)
- `id_ceremony_custodian` (FK → ceremony_custodians)
- `share_index`: x value in Shamir's scheme (1, 2, 3)
- `share_data_encrypted` (BYTEA): Encrypted share
- `polynomial_degree`: threshold - 1
- `distribution_method`: EMAIL, PHYSICAL, API, MANUAL

---

### key_restoration_requests

Tracks requests to restore master keys using key shares.

**Key Columns:**
- `id_key_ceremony_original` (FK → key_ceremonies): Original ceremony
- `id_master_key` (FK → master_keys): Key to restore
- `status`: PENDING, APPROVED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED
- `shares_required`: Minimum shares needed (≥2)
- `shares_submitted`: Count of submitted shares

---

### restoration_share_submissions

Tracks share submissions during restoration ceremonies.

**Key Columns:**
- `id_key_restoration_request` (FK → key_restoration_requests)
- `id_key_share` (FK → key_shares)
- `id_ceremony_custodian` (FK → ceremony_custodians)
- `verification_status`: PENDING, VERIFIED, REJECTED

---

### ceremony_audit_logs

Comprehensive audit trail for all activities.

**Key Columns:**
- `event_type`: Specific event identifier
- `event_category`: CEREMONY, CONTRIBUTION, KEY_GENERATION, DISTRIBUTION, SECURITY, SYSTEM
- `actor_type`: ADMINISTRATOR, CUSTODIAN, SYSTEM, API
- `event_status`: SUCCESS, FAILURE, WARNING, INFO
- `event_severity`: DEBUG, INFO, WARNING, ERROR, CRITICAL
- `event_metadata` (JSONB): Additional flexible metadata

---

### ceremony_statistics

Aggregated statistics for monitoring and reporting.

**Key Columns:**
- `id_key_ceremony` (FK → key_ceremonies, UNIQUE)
- `contributions_received`, `contributions_pending`
- `shares_generated`, `shares_distributed`
- `ceremony_completion_percentage`: 0.00-100.00

---

### contribution_reminders

Tracks reminder emails sent to custodians.

**Key Columns:**
- `id_ceremony_custodian` (FK → ceremony_custodians)
- `reminder_type`: INITIAL, FIRST_REMINDER, SECOND_REMINDER, URGENT, DEADLINE_APPROACHING
- `email_status`: SENT, DELIVERED, BOUNCED, FAILED, OPENED

---

## Entity Relationships

### Banking Key Management Flow

```
banks (1) → (N) terminals
banks (1) → (N) master_keys
terminals (1) → (N) master_keys

master_keys (self-reference):
  - parent_key_id: Key hierarchy (TMK→TPK/TSK, ZMK→ZPK/ZSK)
  - rotated_from_key_id: Rotation chain

zone_key_exchanges:
  - id_source_bank → banks
  - id_destination_bank → banks
  - id_zmk → master_keys (ZMK)
  - id_exchanged_key → master_keys (ZPK/ZSK)

key_rotation_history:
  - id_old_key → master_keys
  - id_new_key → master_keys
```

### Key Ceremony Flow

```
key_ceremonies (1) → (N) ceremony_custodians
key_custodians (1) → (N) ceremony_custodians
ceremony_custodians (1) → (1) passphrase_contributions
key_ceremonies (1) → (1) master_keys
master_keys (1) → (N) key_shares
key_shares (1) ← (1) ceremony_custodians
```

### Restoration Flow

```
key_ceremonies (1) → (N) key_restoration_requests
master_keys (1) → (N) key_restoration_requests
key_restoration_requests (1) → (N) restoration_share_submissions
key_shares (1) → (N) restoration_share_submissions
ceremony_custodians (1) → (N) restoration_share_submissions
```

## Sample Data (V2 Migration)

### Four-Party Model

| bank_code | bank_name | bank_type |
|-----------|-----------|-----------|
| ISS001 | National Issuer Bank | ISSUER |
| ISS002 | Regional Issuer Bank | ISSUER |
| ACQ001 | Merchant Acquirer Bank | ACQUIRER |
| SWT001 | National Payment Switch | SWITCH |

### Terminals

| terminal_id | terminal_type | bank | location |
|-------------|---------------|------|----------|
| TRM-ISS001-ATM-001 | ATM | ISS001 | Jakarta Pusat |
| TRM-ISS001-ATM-002 | ATM | ISS001 | Surabaya |
| TRM-ACQ001-POS-001 | POS | ACQ001 | Jakarta Selatan |
| TRM-ACQ001-POS-002 | POS | ACQ001 | Bandung |
| TRM-ACQ001-MPOS-001 | MPOS | ACQ001 | Jakarta |

### Sample Keys with Hierarchy

| master_key_id | key_type | parent_key | bank | terminal | usage |
|---------------|----------|------------|------|----------|-------|
| TMK-ISS001-ATM001-2024 | TMK | - | ISS001 | TRM-ISS001-ATM-001 | KEY_ENCRYPTION |
| TPK-ISS001-ATM001-2024 | TPK | TMK-ISS001-ATM001-2024 | ISS001 | TRM-ISS001-ATM-001 | PIN_ENCRYPTION |
| TMK-ACQ001-POS001-2024 | TMK | - | ACQ001 | TRM-ACQ001-POS-001 | KEY_ENCRYPTION |
| ZMK-ISS001-ACQ001-2024 | ZMK | - | ISS001 | - | KEY_ENCRYPTION |
| ZPK-ISS001-ACQ001-2024 | ZPK | ZMK-ISS001-ACQ001-2024 | ISS001 | - | PIN_ENCRYPTION |
| ZMK-SWT001-ISS001-2024 | ZMK | - | SWT001 | - | KEY_ENCRYPTION |

### Key Custodians

| custodian_id | full_name | email | department |
|--------------|-----------|-------|------------|
| CUST-A-001 | Alice Johnson | alice.johnson@example.com | IT Security |
| CUST-B-002 | Bob Williams | bob.williams@example.com | IT Security |
| CUST-C-003 | Carol Martinez | carol.martinez@example.com | IT Security |

## Foreign Key Naming Convention

All foreign keys follow the pattern `id_tablename`:
- `id_key_ceremony` → `key_ceremonies.id`
- `id_bank` → `banks.id`
- `id_terminal` → `terminals.id`
- `id_master_key` → `master_keys.id`
- `id_key_custodian` → `key_custodians.id`
- `id_ceremony_custodian` → `ceremony_custodians.id`

## UUID Primary Keys

All tables use UUID as primary keys for:
- Better distribution across sharded databases
- No sequential key guessing attacks
- Globally unique identifiers

## Migration Files

1. **V1__create_key_ceremony_tables.sql**: Creates all 17 tables with constraints, indexes, and relationships
2. **V2__insert_sample_key_custodians.sql**: Inserts four-party model sample data (4 banks, 5 terminals, 3 custodians, 6 keys, 1 key exchange)

---

*Last Updated: October 22, 2025*
*Version: 2.0 - Added Banking Key Management*
