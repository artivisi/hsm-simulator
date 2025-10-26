-- Create generated_macs table for MAC (Message Authentication Code) management
CREATE TABLE generated_macs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message TEXT NOT NULL,
    message_length INTEGER NOT NULL,
    mac_value VARCHAR(64) NOT NULL,
    mac_algorithm VARCHAR(50) NOT NULL CHECK (mac_algorithm IN ('ISO9797-ALG3', 'HMAC-SHA256', 'CBC-MAC')),
    mac_key_id UUID NOT NULL REFERENCES master_keys(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    verification_attempts INTEGER DEFAULT 0,
    last_verified_at TIMESTAMP,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_generated_macs_mac_key_id ON generated_macs(mac_key_id);
CREATE INDEX idx_generated_macs_status ON generated_macs(status);
CREATE INDEX idx_generated_macs_generated_at ON generated_macs(generated_at);
CREATE INDEX idx_generated_macs_message_key ON generated_macs(message, mac_key_id);

-- Add comments for documentation
COMMENT ON TABLE generated_macs IS 'Stores generated MACs (Message Authentication Codes) for transaction integrity verification';
COMMENT ON COLUMN generated_macs.message IS 'Original message that was authenticated';
COMMENT ON COLUMN generated_macs.message_length IS 'Length of the message in bytes';
COMMENT ON COLUMN generated_macs.mac_value IS 'Generated MAC value (hexadecimal)';
COMMENT ON COLUMN generated_macs.mac_algorithm IS 'MAC algorithm used (ISO9797-ALG3, HMAC-SHA256, CBC-MAC)';
COMMENT ON COLUMN generated_macs.mac_key_id IS 'Reference to the MAC key (TSK or ZSK) used';
COMMENT ON COLUMN generated_macs.status IS 'Current status: ACTIVE, EXPIRED, or REVOKED';
COMMENT ON COLUMN generated_macs.verification_attempts IS 'Number of times this MAC has been verified';
COMMENT ON COLUMN generated_macs.last_verified_at IS 'Timestamp of last verification attempt';
COMMENT ON COLUMN generated_macs.generated_at IS 'Timestamp when MAC was generated';
