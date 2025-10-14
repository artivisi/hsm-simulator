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
    custodian_label VARCHAR(10) NOT NULL, -- A, B, C, etc.
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
-- Table: master_keys
-- Purpose: Store generated master keys with metadata
-- ============================================================================
CREATE TABLE master_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    master_key_id VARCHAR(50) UNIQUE NOT NULL,
    id_key_ceremony UUID NOT NULL,
    key_type VARCHAR(50) NOT NULL DEFAULT 'HSM_MASTER_KEY',
    algorithm VARCHAR(50) NOT NULL,
    key_size INTEGER NOT NULL,
    key_data_encrypted BYTEA NOT NULL, -- Encrypted master key
    key_fingerprint VARCHAR(255) NOT NULL,
    key_checksum VARCHAR(255) NOT NULL,
    combined_entropy_hash VARCHAR(255) NOT NULL, -- Hash of combined contributions
    generation_method VARCHAR(50) NOT NULL DEFAULT 'PBKDF2',
    kdf_iterations INTEGER NOT NULL DEFAULT 100000,
    kdf_salt VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ROTATED, REVOKED, EXPIRED
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revocation_reason TEXT,
    FOREIGN KEY (id_key_ceremony) REFERENCES key_ceremonies(id) ON DELETE RESTRICT,
    CONSTRAINT chk_master_key_status CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT chk_master_key_size CHECK (key_size IN (128, 192, 256))
);

CREATE INDEX idx_master_keys_ceremony ON master_keys(id_key_ceremony);
CREATE INDEX idx_master_keys_status ON master_keys(status);
CREATE INDEX idx_master_keys_fingerprint ON master_keys(key_fingerprint);

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
-- Comments for documentation
-- ============================================================================
COMMENT ON TABLE key_custodians IS 'Stores information about individuals who serve as key custodians';
COMMENT ON TABLE key_ceremonies IS 'Tracks the lifecycle and configuration of key ceremonies';
COMMENT ON TABLE ceremony_custodians IS 'Links custodians to specific ceremonies with their contribution status';
COMMENT ON TABLE passphrase_contributions IS 'Stores custodian passphrase contributions with security metadata';
COMMENT ON TABLE master_keys IS 'Stores generated HSM master keys';
COMMENT ON TABLE key_shares IS 'Stores Shamir Secret Sharing key shares for master key recovery';
COMMENT ON TABLE ceremony_audit_logs IS 'Comprehensive audit trail for all ceremony-related activities';
COMMENT ON TABLE ceremony_statistics IS 'Aggregated statistics for ceremony monitoring and reporting';
COMMENT ON TABLE contribution_reminders IS 'Tracks reminder communications sent to custodians';
COMMENT ON TABLE key_restoration_requests IS 'Tracks requests to restore master keys using key shares';
COMMENT ON TABLE restoration_share_submissions IS 'Tracks share submissions during restoration ceremonies';

-- ============================================================================
-- End of Migration V1
-- ============================================================================
