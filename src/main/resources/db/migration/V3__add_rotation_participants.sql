-- ============================================================================
-- HSM Simulator - Add Rotation Participants Table
-- Version: 3.0
-- Purpose: Add rotation_participants table for tracking individual terminal/bank
--          update status during key rotation with pending state support
-- ============================================================================

-- ============================================================================
-- Rotation Participants Table
-- Purpose: Track which terminals/banks have completed their key update during rotation
-- ============================================================================

CREATE TABLE rotation_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_rotation UUID NOT NULL,
    id_terminal UUID,
    id_bank UUID,
    participant_type VARCHAR(20) NOT NULL CHECK (participant_type IN ('TERMINAL', 'BANK')),
    update_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (update_status IN ('PENDING', 'DELIVERED', 'CONFIRMED', 'FAILED', 'SKIPPED')),
    new_key_delivered_at TIMESTAMP,
    update_confirmed_at TIMESTAMP,
    update_confirmed_by VARCHAR(100),
    delivery_attempts INTEGER NOT NULL DEFAULT 0,
    last_delivery_attempt TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_rotation_participant_rotation FOREIGN KEY (id_rotation) REFERENCES key_rotation_history(id) ON DELETE CASCADE,
    CONSTRAINT fk_rotation_participant_terminal FOREIGN KEY (id_terminal) REFERENCES terminals(id) ON DELETE SET NULL,
    CONSTRAINT fk_rotation_participant_bank FOREIGN KEY (id_bank) REFERENCES banks(id) ON DELETE SET NULL,
    CONSTRAINT chk_participant_has_terminal_or_bank CHECK (
        (participant_type = 'TERMINAL' AND id_terminal IS NOT NULL) OR
        (participant_type = 'BANK' AND id_bank IS NOT NULL)
    )
);

-- ============================================================================
-- Indexes for Rotation Participants
-- ============================================================================

CREATE INDEX idx_rotation_participant_rotation ON rotation_participants(id_rotation);
CREATE INDEX idx_rotation_participant_terminal ON rotation_participants(id_terminal);
CREATE INDEX idx_rotation_participant_bank ON rotation_participants(id_bank);
CREATE INDEX idx_rotation_participant_status ON rotation_participants(update_status);

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON TABLE rotation_participants IS 'Tracks individual participants (terminals/banks) in a key rotation process with pending/delivered/confirmed states';
COMMENT ON COLUMN rotation_participants.participant_type IS 'Type of participant: TERMINAL or BANK';
COMMENT ON COLUMN rotation_participants.update_status IS 'Status of key update: PENDING, DELIVERED, CONFIRMED, FAILED, SKIPPED';
COMMENT ON COLUMN rotation_participants.new_key_delivered_at IS 'When new key was delivered to participant';
COMMENT ON COLUMN rotation_participants.update_confirmed_at IS 'When participant confirmed successful key installation';
COMMENT ON COLUMN rotation_participants.delivery_attempts IS 'Number of times key delivery was attempted';
COMMENT ON COLUMN rotation_participants.failure_reason IS 'Reason for failure if update_status is FAILED';

-- ============================================================================
-- End of Migration V3
-- ============================================================================
