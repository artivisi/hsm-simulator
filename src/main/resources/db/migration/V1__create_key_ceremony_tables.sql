-- ============================================================================
-- HSM Simulator - Key Ceremony Database Schema
-- Version: 1.0
-- Purpose: Create tables for Key Ceremony functionality with 2-of-3 threshold
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Table: key_custodians
-- Purpose: Store information about key custodians who participate in ceremonies
-- ============================================================================
CREATE TABLE key_custodians (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    custodian_id VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    department VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT chk_custodian_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_custodian_email ON key_custodians(email);
CREATE INDEX idx_custodian_status ON key_custodians(status);

-- ============================================================================
-- Table: key_ceremonies
-- Purpose: Track key ceremony lifecycle and configuration
-- ============================================================================
CREATE TABLE key_ceremonies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ceremony_id VARCHAR(50) UNIQUE NOT NULL,
    ceremony_name VARCHAR(255) NOT NULL,
    purpose TEXT NOT NULL,
    ceremony_type VARCHAR(50) NOT NULL, -- INITIALIZATION, RESTORATION, KEY_ROTATION
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, AWAITING_CONTRIBUTIONS, PARTIAL_CONTRIBUTIONS, GENERATING_KEY, COMPLETED, CANCELLED, EXPIRED
    number_of_custodians INTEGER NOT NULL DEFAULT 3,
    threshold INTEGER NOT NULL DEFAULT 2,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256',
    key_size INTEGER NOT NULL DEFAULT 256,
    contribution_deadline TIMESTAMP,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_by VARCHAR(100) NOT NULL,
    last_modified_by VARCHAR(100),
    CONSTRAINT chk_ceremony_type CHECK (ceremony_type IN ('INITIALIZATION', 'RESTORATION', 'KEY_ROTATION')),
    CONSTRAINT chk_ceremony_status CHECK (status IN ('PENDING', 'AWAITING_CONTRIBUTIONS', 'PARTIAL_CONTRIBUTIONS', 'GENERATING_KEY', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_threshold_valid CHECK (threshold > 0 AND threshold <= number_of_custodians),
    CONSTRAINT chk_custodians_min CHECK (number_of_custodians >= 2),
    CONSTRAINT chk_key_size CHECK (key_size IN (128, 192, 256))
);

CREATE INDEX idx_ceremony_status ON key_ceremonies(status);
CREATE INDEX idx_ceremony_type ON key_ceremonies(ceremony_type);
CREATE INDEX idx_ceremony_created_at ON key_ceremonies(started_at);

-- ============================================================================
-- Table: ceremony_custodians
-- Purpose: Link custodians to ceremonies with their specific roles
-- ============================================================================
CREATE TABLE ceremony_custodians (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_key_ceremony UUID NOT NULL,
    id_key_custodian UUID NOT NULL,
    custodian_order INTEGER NOT NULL, -- Order in the ceremony (1, 2, 3, etc.)
    custodian_label VARCHAR(50) NOT NULL, -- Custodian A, Custodian B, etc.
    contribution_token VARCHAR(255) UNIQUE NOT NULL,
    contribution_link TEXT,
    invitation_sent_at TIMESTAMP,
    contribution_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, CONTRIBUTED, EXPIRED
    contributed_at TIMESTAMP,
    share_sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_key_ceremony) REFERENCES key_ceremonies(id) ON DELETE CASCADE,
    FOREIGN KEY (id_key_custodian) REFERENCES key_custodians(id) ON DELETE RESTRICT,
    UNIQUE(id_key_ceremony, id_key_custodian),
    UNIQUE(id_key_ceremony, custodian_order),
    UNIQUE(id_key_ceremony, custodian_label),
    CONSTRAINT chk_contribution_status CHECK (contribution_status IN ('PENDING', 'CONTRIBUTED', 'EXPIRED'))
);

CREATE INDEX idx_ceremony_custodians_ceremony ON ceremony_custodians(id_key_ceremony);
CREATE INDEX idx_ceremony_custodians_custodian ON ceremony_custodians(id_key_custodian);
CREATE INDEX idx_ceremony_custodians_token ON ceremony_custodians(contribution_token);
CREATE INDEX idx_ceremony_custodians_status ON ceremony_custodians(contribution_status);

-- ============================================================================
-- Table: passphrase_contributions
-- Purpose: Store custodian passphrase contributions with security metadata
-- ============================================================================
CREATE TABLE passphrase_contributions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    contribution_id VARCHAR(50) UNIQUE NOT NULL,
    id_ceremony_custodian UUID NOT NULL,
    passphrase_hash VARCHAR(255) NOT NULL, -- SHA-256 hash of passphrase
    passphrase_entropy_score DECIMAL(3,1) NOT NULL, -- 0.0 to 10.0
    passphrase_strength VARCHAR(20) NOT NULL, -- WEAK, FAIR, GOOD, STRONG, VERY_STRONG
    passphrase_length INTEGER NOT NULL,
    contribution_fingerprint VARCHAR(255) NOT NULL,
    contributed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45), -- Support IPv6
    user_agent TEXT,
    FOREIGN KEY (id_ceremony_custodian) REFERENCES ceremony_custodians(id) ON DELETE CASCADE,
    CONSTRAINT chk_passphrase_strength CHECK (passphrase_strength IN ('WEAK', 'FAIR', 'GOOD', 'STRONG', 'VERY_STRONG')),
    CONSTRAINT chk_entropy_score CHECK (passphrase_entropy_score >= 0.0 AND passphrase_entropy_score <= 10.0),
    CONSTRAINT chk_passphrase_length CHECK (passphrase_length >= 16)
);

CREATE INDEX idx_contributions_ceremony_custodian ON passphrase_contributions(id_ceremony_custodian);
CREATE INDEX idx_contributions_timestamp ON passphrase_contributions(contributed_at);

-- ============================================================================
-- Table: banks
-- Purpose: Store bank/organization information for multi-party model
-- ============================================================================
CREATE TABLE banks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bank_code VARCHAR(20) UNIQUE NOT NULL,
    bank_name VARCHAR(255) NOT NULL,
    bank_type VARCHAR(50) NOT NULL, -- ISSUER, ACQUIRER, SWITCH, PROCESSOR
    country_code VARCHAR(3),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bank_type CHECK (bank_type IN ('ISSUER', 'ACQUIRER', 'SWITCH', 'PROCESSOR')),
    CONSTRAINT chk_bank_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_banks_type ON banks(bank_type);
CREATE INDEX idx_banks_status ON banks(status);

-- ============================================================================
-- Table: terminals
-- Purpose: Store terminal information for terminal key management
-- ============================================================================
CREATE TABLE terminals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    terminal_id VARCHAR(50) UNIQUE NOT NULL,
    terminal_name VARCHAR(255) NOT NULL,
    id_bank UUID NOT NULL,
    terminal_type VARCHAR(50) NOT NULL, -- ATM, POS, MPOS, VIRTUAL
    location VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_bank) REFERENCES banks(id) ON DELETE RESTRICT,
    CONSTRAINT chk_terminal_type CHECK (terminal_type IN ('ATM', 'POS', 'MPOS', 'VIRTUAL', 'E_COMMERCE')),
    CONSTRAINT chk_terminal_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'MAINTENANCE'))
);

CREATE INDEX idx_terminals_bank ON terminals(id_bank);
CREATE INDEX idx_terminals_status ON terminals(status);
CREATE INDEX idx_terminals_type ON terminals(terminal_type);

-- ============================================================================
-- Table: master_keys
-- Purpose: Store generated master keys with metadata
-- ============================================================================
CREATE TABLE master_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    master_key_id VARCHAR(50) UNIQUE NOT NULL,
    id_key_ceremony UUID,
    id_bank UUID,
    id_terminal UUID,
    parent_key_id UUID, -- Reference to parent key in hierarchy
    key_type VARCHAR(50) NOT NULL DEFAULT 'HSM_MASTER_KEY',
    key_usage VARCHAR(50), -- PIN_ENCRYPTION, DATA_ENCRYPTION, KEY_ENCRYPTION, MAC_GENERATION
    algorithm VARCHAR(50) NOT NULL,
    key_size INTEGER NOT NULL,
    key_data_encrypted BYTEA NOT NULL, -- Encrypted master key
    key_fingerprint VARCHAR(255) NOT NULL,
    key_checksum VARCHAR(255) NOT NULL,
    combined_entropy_hash VARCHAR(255),
    generation_method VARCHAR(50) NOT NULL DEFAULT 'PBKDF2',
    kdf_iterations INTEGER NOT NULL DEFAULT 100000,
    kdf_salt VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ROTATED, REVOKED, EXPIRED
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revocation_reason TEXT,
    rotated_from_key_id UUID, -- Reference to the key this replaced
    rotation_reason TEXT,
    FOREIGN KEY (id_key_ceremony) REFERENCES key_ceremonies(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_bank) REFERENCES banks(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_terminal) REFERENCES terminals(id) ON DELETE RESTRICT,
    FOREIGN KEY (parent_key_id) REFERENCES master_keys(id) ON DELETE SET NULL,
    FOREIGN KEY (rotated_from_key_id) REFERENCES master_keys(id) ON DELETE SET NULL,
    CONSTRAINT chk_master_key_type CHECK (key_type IN ('HSM_MASTER_KEY', 'TMK', 'TPK', 'TSK', 'ZMK', 'ZPK', 'ZSK', 'LMK', 'KEK')),
    CONSTRAINT chk_master_key_usage CHECK (key_usage IN ('PIN_ENCRYPTION', 'DATA_ENCRYPTION', 'KEY_ENCRYPTION', 'MAC_GENERATION', 'SESSION', 'MASTER')),
    CONSTRAINT chk_master_key_status CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT chk_master_key_size CHECK (key_size IN (128, 192, 256))
);

CREATE INDEX idx_master_keys_ceremony ON master_keys(id_key_ceremony);
CREATE INDEX idx_master_keys_bank ON master_keys(id_bank);
CREATE INDEX idx_master_keys_terminal ON master_keys(id_terminal);
CREATE INDEX idx_master_keys_type ON master_keys(key_type);
CREATE INDEX idx_master_keys_status ON master_keys(status);
CREATE INDEX idx_master_keys_fingerprint ON master_keys(key_fingerprint);
CREATE INDEX idx_master_keys_parent ON master_keys(parent_key_id);
CREATE INDEX idx_master_keys_rotated_from ON master_keys(rotated_from_key_id);

-- ============================================================================
-- Table: key_shares
-- Purpose: Store Shamir's Secret Sharing key shares
-- ============================================================================
CREATE TABLE key_shares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    share_id VARCHAR(50) UNIQUE NOT NULL,
    id_master_key UUID NOT NULL,
    id_ceremony_custodian UUID NOT NULL,
    share_index INTEGER NOT NULL, -- x value in Shamir's scheme (1, 2, 3, etc.)
    share_data_encrypted BYTEA NOT NULL, -- Encrypted share data
    share_verification_hash VARCHAR(255) NOT NULL,
    polynomial_degree INTEGER NOT NULL, -- degree of polynomial (threshold - 1)
    prime_modulus TEXT, -- Prime number used in Shamir's scheme (for educational purposes)
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    distributed_at TIMESTAMP,
    distribution_method VARCHAR(50) DEFAULT 'EMAIL', -- EMAIL, PHYSICAL, API
    used_in_restoration BOOLEAN DEFAULT FALSE,
    last_used_at TIMESTAMP,
    FOREIGN KEY (id_master_key) REFERENCES master_keys(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_ceremony_custodian) REFERENCES ceremony_custodians(id) ON DELETE RESTRICT,
    UNIQUE(id_master_key, share_index),
    UNIQUE(id_master_key, id_ceremony_custodian),
    CONSTRAINT chk_share_index_positive CHECK (share_index > 0),
    CONSTRAINT chk_distribution_method CHECK (distribution_method IN ('EMAIL', 'PHYSICAL', 'API', 'MANUAL'))
);

CREATE INDEX idx_key_shares_master_key ON key_shares(id_master_key);
CREATE INDEX idx_key_shares_custodian ON key_shares(id_ceremony_custodian);
CREATE INDEX idx_key_shares_used ON key_shares(used_in_restoration);

-- ============================================================================
-- Table: ceremony_audit_logs
-- Purpose: Comprehensive audit trail for all ceremony activities
-- ============================================================================
CREATE TABLE ceremony_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_key_ceremony UUID,
    event_type VARCHAR(100) NOT NULL, -- CEREMONY_CREATED, CUSTODIAN_ADDED, CONTRIBUTION_RECEIVED, etc.
    event_category VARCHAR(50) NOT NULL, -- CEREMONY, CONTRIBUTION, KEY_GENERATION, DISTRIBUTION, SECURITY
    event_description TEXT NOT NULL,
    actor_type VARCHAR(50), -- ADMINISTRATOR, CUSTODIAN, SYSTEM
    actor_id VARCHAR(100), -- User/custodian identifier
    actor_name VARCHAR(255),
    target_entity_type VARCHAR(50), -- CEREMONY, CUSTODIAN, CONTRIBUTION, KEY, SHARE
    target_entity_id VARCHAR(100),
    event_status VARCHAR(20) NOT NULL, -- SUCCESS, FAILURE, WARNING, INFO
    event_severity VARCHAR(20) NOT NULL DEFAULT 'INFO', -- DEBUG, INFO, WARNING, ERROR, CRITICAL
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(100),
    session_id VARCHAR(100),
    event_metadata JSONB, -- Additional flexible metadata
    error_message TEXT,
    stack_trace TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_key_ceremony) REFERENCES key_ceremonies(id) ON DELETE SET NULL,
    CONSTRAINT chk_event_category CHECK (event_category IN ('CEREMONY', 'CONTRIBUTION', 'KEY_GENERATION', 'DISTRIBUTION', 'SECURITY', 'SYSTEM')),
    CONSTRAINT chk_event_status CHECK (event_status IN ('SUCCESS', 'FAILURE', 'WARNING', 'INFO')),
    CONSTRAINT chk_event_severity CHECK (event_severity IN ('DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    CONSTRAINT chk_actor_type CHECK (actor_type IN ('ADMINISTRATOR', 'CUSTODIAN', 'SYSTEM', 'API'))
);

CREATE INDEX idx_audit_ceremony ON ceremony_audit_logs(id_key_ceremony);
CREATE INDEX idx_audit_event_type ON ceremony_audit_logs(event_type);
CREATE INDEX idx_audit_event_category ON ceremony_audit_logs(event_category);
CREATE INDEX idx_audit_created_at ON ceremony_audit_logs(created_at);
CREATE INDEX idx_audit_actor ON ceremony_audit_logs(actor_id);
CREATE INDEX idx_audit_event_status ON ceremony_audit_logs(event_status);
CREATE INDEX idx_audit_event_severity ON ceremony_audit_logs(event_severity);
CREATE INDEX idx_audit_metadata ON ceremony_audit_logs USING GIN (event_metadata);

-- ============================================================================
-- Table: ceremony_statistics
-- Purpose: Store aggregated statistics for monitoring and reporting
-- ============================================================================
CREATE TABLE ceremony_statistics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_key_ceremony UUID NOT NULL,
    total_custodians INTEGER NOT NULL DEFAULT 0,
    contributions_received INTEGER NOT NULL DEFAULT 0,
    contributions_pending INTEGER NOT NULL DEFAULT 0,
    shares_generated INTEGER NOT NULL DEFAULT 0,
    shares_distributed INTEGER NOT NULL DEFAULT 0,
    average_contribution_time_minutes INTEGER,
    total_duration_minutes INTEGER,
    ceremony_completion_percentage DECIMAL(5,2) DEFAULT 0.00,
    last_activity_at TIMESTAMP,
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_key_ceremony) REFERENCES key_ceremonies(id) ON DELETE CASCADE,
    CONSTRAINT chk_completion_percentage CHECK (ceremony_completion_percentage >= 0.00 AND ceremony_completion_percentage <= 100.00)
);

CREATE UNIQUE INDEX idx_stats_ceremony ON ceremony_statistics(id_key_ceremony);

-- ============================================================================
-- Table: contribution_reminders
-- Purpose: Track reminder emails sent to custodians
-- ============================================================================
CREATE TABLE contribution_reminders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_ceremony_custodian UUID NOT NULL,
    reminder_type VARCHAR(50) NOT NULL, -- INITIAL, FIRST_REMINDER, SECOND_REMINDER, URGENT
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_by VARCHAR(100),
    email_status VARCHAR(20) NOT NULL DEFAULT 'SENT', -- SENT, DELIVERED, BOUNCED, FAILED
    delivery_confirmed_at TIMESTAMP,
    FOREIGN KEY (id_ceremony_custodian) REFERENCES ceremony_custodians(id) ON DELETE CASCADE,
    CONSTRAINT chk_reminder_type CHECK (reminder_type IN ('INITIAL', 'FIRST_REMINDER', 'SECOND_REMINDER', 'URGENT', 'DEADLINE_APPROACHING')),
    CONSTRAINT chk_email_status CHECK (email_status IN ('SENT', 'DELIVERED', 'BOUNCED', 'FAILED', 'OPENED'))
);

CREATE INDEX idx_reminders_ceremony_custodian ON contribution_reminders(id_ceremony_custodian);
CREATE INDEX idx_reminders_sent_at ON contribution_reminders(sent_at);

-- ============================================================================
-- Table: key_restoration_requests
-- Purpose: Track key restoration ceremony requests
-- ============================================================================
CREATE TABLE key_restoration_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    restoration_id VARCHAR(50) UNIQUE NOT NULL,
    id_key_ceremony_original UUID NOT NULL,
    id_master_key UUID NOT NULL,
    restoration_reason TEXT NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED
    shares_required INTEGER NOT NULL,
    shares_submitted INTEGER NOT NULL DEFAULT 0,
    restoration_completed_at TIMESTAMP,
    restored_key_verified BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_key_ceremony_original) REFERENCES key_ceremonies(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_master_key) REFERENCES master_keys(id) ON DELETE RESTRICT,
    CONSTRAINT chk_restoration_status CHECK (status IN ('PENDING', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_shares_required CHECK (shares_required >= 2)
);

CREATE INDEX idx_restoration_original_ceremony ON key_restoration_requests(id_key_ceremony_original);
CREATE INDEX idx_restoration_master_key ON key_restoration_requests(id_master_key);
CREATE INDEX idx_restoration_status ON key_restoration_requests(status);

-- ============================================================================
-- Table: restoration_share_submissions
-- Purpose: Track share submissions during restoration
-- ============================================================================
CREATE TABLE restoration_share_submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_key_restoration_request UUID NOT NULL,
    id_key_share UUID NOT NULL,
    id_ceremony_custodian UUID NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, REJECTED
    verified_at TIMESTAMP,
    ip_address VARCHAR(45),
    FOREIGN KEY (id_key_restoration_request) REFERENCES key_restoration_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (id_key_share) REFERENCES key_shares(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_ceremony_custodian) REFERENCES ceremony_custodians(id) ON DELETE RESTRICT,
    UNIQUE(id_key_restoration_request, id_key_share),
    CONSTRAINT chk_verification_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_restoration_submissions ON restoration_share_submissions(id_key_restoration_request);
CREATE INDEX idx_restoration_submissions_share ON restoration_share_submissions(id_key_share);
CREATE INDEX idx_restoration_submissions_custodian ON restoration_share_submissions(id_ceremony_custodian);

-- ============================================================================
-- Table: zone_key_exchanges
-- Purpose: Track inter-bank zone key exchange for ZMK, ZPK, ZSK
-- ============================================================================
CREATE TABLE zone_key_exchanges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exchange_id VARCHAR(50) UNIQUE NOT NULL,
    id_source_bank UUID NOT NULL,
    id_destination_bank UUID NOT NULL,
    id_zmk UUID NOT NULL, -- Zone Master Key used for this exchange
    id_exchanged_key UUID NOT NULL, -- The ZPK or ZSK being exchanged
    exchange_type VARCHAR(50) NOT NULL, -- INITIAL, RENEWAL, EMERGENCY
    key_transport_method VARCHAR(50) NOT NULL, -- ENCRYPTED_UNDER_ZMK, MANUAL, COURIER
    transport_key_fingerprint VARCHAR(255),
    exchange_status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP,
    activated_at TIMESTAMP,
    expires_at TIMESTAMP,
    initiated_by VARCHAR(100) NOT NULL,
    acknowledged_by VARCHAR(100),
    FOREIGN KEY (id_source_bank) REFERENCES banks(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_destination_bank) REFERENCES banks(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_zmk) REFERENCES master_keys(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_exchanged_key) REFERENCES master_keys(id) ON DELETE RESTRICT,
    CONSTRAINT chk_exchange_type CHECK (exchange_type IN ('INITIAL', 'RENEWAL', 'EMERGENCY', 'ROTATION')),
    CONSTRAINT chk_transport_method CHECK (key_transport_method IN ('ENCRYPTED_UNDER_ZMK', 'MANUAL', 'COURIER', 'SECURE_CHANNEL')),
    CONSTRAINT chk_exchange_status CHECK (exchange_status IN ('INITIATED', 'IN_TRANSIT', 'ACKNOWLEDGED', 'ACTIVATED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_different_banks CHECK (id_source_bank != id_destination_bank)
);

CREATE INDEX idx_zone_exchanges_source ON zone_key_exchanges(id_source_bank);
CREATE INDEX idx_zone_exchanges_destination ON zone_key_exchanges(id_destination_bank);
CREATE INDEX idx_zone_exchanges_zmk ON zone_key_exchanges(id_zmk);
CREATE INDEX idx_zone_exchanges_status ON zone_key_exchanges(exchange_status);
CREATE INDEX idx_zone_exchanges_initiated_at ON zone_key_exchanges(initiated_at);

-- ============================================================================
-- Table: key_rotation_history
-- Purpose: Track complete history of key rotations for compliance and audit
-- ============================================================================
CREATE TABLE key_rotation_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rotation_id VARCHAR(50) UNIQUE NOT NULL,
    id_old_key UUID NOT NULL,
    id_new_key UUID NOT NULL,
    rotation_type VARCHAR(50) NOT NULL, -- SCHEDULED, EMERGENCY, COMPLIANCE, COMPROMISE
    rotation_reason TEXT NOT NULL,
    rotation_initiated_by VARCHAR(100) NOT NULL,
    rotation_approved_by VARCHAR(100),
    rotation_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rotation_completed_at TIMESTAMP,
    rotation_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    affected_terminals_count INTEGER DEFAULT 0,
    affected_banks_count INTEGER DEFAULT 0,
    rollback_required BOOLEAN DEFAULT FALSE,
    rollback_completed_at TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (id_old_key) REFERENCES master_keys(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_new_key) REFERENCES master_keys(id) ON DELETE RESTRICT,
    CONSTRAINT chk_rotation_type CHECK (rotation_type IN ('SCHEDULED', 'EMERGENCY', 'COMPLIANCE', 'COMPROMISE', 'EXPIRATION')),
    CONSTRAINT chk_rotation_status CHECK (rotation_status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'ROLLED_BACK', 'CANCELLED'))
);

CREATE INDEX idx_rotation_history_old_key ON key_rotation_history(id_old_key);
CREATE INDEX idx_rotation_history_new_key ON key_rotation_history(id_new_key);
CREATE INDEX idx_rotation_history_status ON key_rotation_history(rotation_status);
CREATE INDEX idx_rotation_history_started_at ON key_rotation_history(rotation_started_at);

-- ============================================================================
-- Comments for documentation
-- ============================================================================
COMMENT ON TABLE banks IS 'Stores bank/organization information for four-party card processing model';
COMMENT ON TABLE terminals IS 'Stores terminal information (ATM, POS, etc.) for terminal key management';
COMMENT ON TABLE key_custodians IS 'Stores information about individuals who serve as key custodians';
COMMENT ON TABLE key_ceremonies IS 'Tracks the lifecycle and configuration of key ceremonies';
COMMENT ON TABLE ceremony_custodians IS 'Links custodians to specific ceremonies with their contribution status';
COMMENT ON TABLE passphrase_contributions IS 'Stores custodian passphrase contributions with security metadata';
COMMENT ON TABLE master_keys IS 'Stores all cryptographic keys: HSM master keys, TMK, TPK, TSK, ZMK, ZPK, ZSK with hierarchy support';
COMMENT ON TABLE key_shares IS 'Stores Shamir Secret Sharing key shares for master key recovery';
COMMENT ON TABLE ceremony_audit_logs IS 'Comprehensive audit trail for all ceremony-related activities';
COMMENT ON TABLE ceremony_statistics IS 'Aggregated statistics for ceremony monitoring and reporting';
COMMENT ON TABLE contribution_reminders IS 'Tracks reminder communications sent to custodians';
COMMENT ON TABLE key_restoration_requests IS 'Tracks requests to restore master keys using key shares';
COMMENT ON TABLE restoration_share_submissions IS 'Tracks share submissions during restoration ceremonies';
COMMENT ON TABLE zone_key_exchanges IS 'Tracks inter-bank zone key exchanges (ZMK, ZPK, ZSK) for four-party model';
COMMENT ON TABLE key_rotation_history IS 'Complete audit trail of key rotation activities for compliance';

-- ============================================================================
-- End of Migration V1
-- ============================================================================
