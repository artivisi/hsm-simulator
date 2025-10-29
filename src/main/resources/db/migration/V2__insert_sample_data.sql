-- ============================================================================
-- HSM Simulator - Sample Data
-- Version: 2.0
-- Purpose: Insert sample data for banks, terminals, custodians, and users
-- ============================================================================

-- ============================================================================
-- Sample Banks (Four-Party Model)
-- Purpose: Create sample banks representing Issuer, Acquirer, Switch, Processor
-- ============================================================================

INSERT INTO banks (bank_code, bank_name, bank_type, country_code, status) VALUES
    ('ISS001', 'National Issuer Bank', 'ISSUER', 'IDN', 'ACTIVE'),
    ('ACQ001', 'Merchant Acquirer Bank', 'ACQUIRER', 'IDN', 'ACTIVE'),
    ('SWT001', 'National Payment Switch', 'SWITCH', 'IDN', 'ACTIVE'),
    ('ISS002', 'Regional Issuer Bank', 'ISSUER', 'IDN', 'ACTIVE');

-- ============================================================================
-- Sample Terminals
-- Purpose: Create sample terminals (ATM, POS) for different banks
-- ============================================================================

INSERT INTO terminals (terminal_id, terminal_name, id_bank, terminal_type, location, status)
SELECT
    'TRM-ISS001-ATM-001',
    'National Issuer Bank - ATM Jakarta Pusat',
    b.id,
    'ATM',
    'Jakarta Pusat, Indonesia',
    'ACTIVE'
FROM banks b WHERE b.bank_code = 'ISS001'
UNION ALL
SELECT
    'TRM-ISS001-ATM-002',
    'National Issuer Bank - ATM Surabaya',
    b.id,
    'ATM',
    'Surabaya, Indonesia',
    'ACTIVE'
FROM banks b WHERE b.bank_code = 'ISS001'
UNION ALL
SELECT
    'TRM-ACQ001-POS-001',
    'Merchant Acquirer - POS SuperMarket',
    b.id,
    'POS',
    'Jakarta Selatan, Indonesia',
    'ACTIVE'
FROM banks b WHERE b.bank_code = 'ACQ001'
UNION ALL
SELECT
    'TRM-ACQ001-POS-002',
    'Merchant Acquirer - POS Restaurant',
    b.id,
    'POS',
    'Bandung, Indonesia',
    'ACTIVE'
FROM banks b WHERE b.bank_code = 'ACQ001'
UNION ALL
SELECT
    'TRM-ACQ001-MPOS-001',
    'Merchant Acquirer - Mobile POS Taxi',
    b.id,
    'MPOS',
    'Jakarta, Indonesia',
    'ACTIVE'
FROM banks b WHERE b.bank_code = 'ACQ001';

-- ============================================================================
-- Sample Key Custodians
-- Purpose: Create 3 sample custodians for testing key ceremony functionality
-- ============================================================================

INSERT INTO key_custodians (
    custodian_id,
    full_name,
    email,
    phone,
    department,
    status,
    created_by
) VALUES
    (
        'CUST-A-001',
        'Alice Johnson',
        'alice.johnson@yopmail.com',
        '+62-812-3456-7890',
        'IT Security',
        'ACTIVE',
        'system'
    ),
    (
        'CUST-B-002',
        'Bob Williams',
        'bob.williams@yopmail.com',
        '+62-812-3456-7891',
        'IT Security',
        'ACTIVE',
        'system'
    ),
    (
        'CUST-C-003',
        'Carol Martinez',
        'carol.martinez@yopmail.com',
        '+62-812-3456-7892',
        'IT Security',
        'ACTIVE',
        'system'
    );

-- ============================================================================
-- Default Admin User
-- Purpose: Create default admin user for initial system access
-- ============================================================================

-- Insert default admin user
-- Username: admin
-- Password: admin123 (BCrypt hash for 'admin123')
-- NOTE: Change this password in production!
INSERT INTO users (username, email, password, full_name, role, active) VALUES
('admin', 'admin@hsm-simulator.local', '$2a$10$XiLG2cUTyrhavQ0EDwicIOma78/x/bNX/V3PUMNJQuRumgwTLt4dy', 'System Administrator', 'ADMIN', TRUE);

-- ============================================================================
-- Sample Master Keys
-- Purpose: Create sample keys for testing PIN operations
-- ============================================================================

-- Sample LMK (Local Master Key) for PIN encryption
-- This is a test key for demonstration purposes only
INSERT INTO master_keys (
    master_key_id,
    key_type,
    algorithm,
    key_size,
    key_data_encrypted,
    key_fingerprint,
    key_checksum,
    generation_method,
    kdf_iterations,
    kdf_salt,
    status,
    generated_at,
    activated_at
) VALUES (
    'LMK-SAMPLE-001',
    'LMK',
    'AES',
    256,
    decode('0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF', 'hex'),
    'A1B2C3D4E5F6789012345678',
    'ABC123',
    'SAMPLE',
    100000,
    'sample_salt_value_12345',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Sample TPK (Terminal PIN Key) for terminal PIN encryption
-- This is a test key for demonstration purposes only
INSERT INTO master_keys (
    master_key_id,
    key_type,
    algorithm,
    key_size,
    key_data_encrypted,
    key_fingerprint,
    key_checksum,
    generation_method,
    kdf_iterations,
    kdf_salt,
    status,
    generated_at,
    activated_at
) VALUES (
    'TPK-SAMPLE-001',
    'TPK',
    'AES',
    256,
    decode('FEDCBA9876543210FEDCBA9876543210FEDCBA9876543210FEDCBA9876543210', 'hex'),
    'F1E2D3C4B5A6987654321098',
    'FED987',
    'SAMPLE',
    100000,
    'sample_tpk_salt_67890',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- Comments
-- ============================================================================
COMMENT ON TABLE banks IS 'Four-party model: Issuer, Acquirer, Switch, and additional Issuer bank';
COMMENT ON TABLE terminals IS 'Sample terminals including ATM, POS, and MPOS across different banks';
COMMENT ON TABLE key_custodians IS 'Sample custodians created for testing key ceremony workflows';
COMMENT ON TABLE users IS 'Default admin user for initial system access';

-- ============================================================================
-- End of Migration V2
-- ============================================================================
