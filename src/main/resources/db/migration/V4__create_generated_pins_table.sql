-- Create table for tracking generated PINs
CREATE TABLE generated_pins (
    id UUID PRIMARY KEY,
    account_number VARCHAR(19) NOT NULL UNIQUE,
    pin_length INTEGER NOT NULL CHECK (pin_length BETWEEN 4 AND 12),
    pin_format VARCHAR(20) NOT NULL,
    encrypted_pin_block TEXT NOT NULL,
    pin_verification_value VARCHAR(10),
    encryption_key_id UUID NOT NULL,
    clear_pin VARCHAR(12),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    generated_at TIMESTAMP NOT NULL,
    last_verified_at TIMESTAMP,
    verification_attempts INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_pin_encryption_key FOREIGN KEY (encryption_key_id)
        REFERENCES master_keys(id) ON DELETE RESTRICT
);

-- Create indexes for performance
CREATE INDEX idx_generated_pins_account ON generated_pins(account_number);
CREATE INDEX idx_generated_pins_status ON generated_pins(status);
CREATE INDEX idx_generated_pins_encryption_key ON generated_pins(encryption_key_id);
CREATE INDEX idx_generated_pins_generated_at ON generated_pins(generated_at DESC);

-- Add comments
COMMENT ON TABLE generated_pins IS 'Stores generated and encrypted PINs for card accounts';
COMMENT ON COLUMN generated_pins.pin_format IS 'PIN block format: ISO-0, ISO-1, ISO-3, etc.';
COMMENT ON COLUMN generated_pins.pin_verification_value IS 'PIN Verification Value (PVV) for offline PIN verification';
COMMENT ON COLUMN generated_pins.clear_pin IS 'Clear PIN for simulation/testing - NEVER store in production';
