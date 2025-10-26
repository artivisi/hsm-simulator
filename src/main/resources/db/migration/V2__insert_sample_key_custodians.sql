-- ============================================================================
-- HSM Simulator - Sample Data for Four-Party Card Processing Model
-- Version: 2.0
-- Purpose: Insert sample data for banks, terminals, custodians, and keys
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
-- Sample Master Keys (TMK, ZMK) for demonstration
-- Purpose: Create sample keys for terminal and zone key management
-- Note: In production, these would be generated through proper key ceremonies
-- ============================================================================

-- TMK for Issuer Bank Terminal 1
INSERT INTO master_keys (
    master_key_id,
    id_bank,
    id_terminal,
    key_type,
    key_usage,
    algorithm,
    key_size,
    key_data_encrypted,
    key_fingerprint,
    key_checksum,
    generation_method,
    kdf_iterations,
    kdf_salt,
    status
)
SELECT
    'TMK-ISS001-ATM001-2024',
    b.id,
    t.id,
    'TMK',
    'KEY_ENCRYPTION',
    'AES-256',
    256,
    decode('deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef', 'hex'),
    'FP-TMK-ISS001-ATM001-SHA256-ABC123',
    'CK-TMK-ISS001-ATM001-XYZ789',
    'SECURE_RANDOM',
    0,
    'SALT-TMK-ISS001-ATM001',
    'ACTIVE'
FROM banks b
CROSS JOIN terminals t
WHERE b.bank_code = 'ISS001' AND t.terminal_id = 'TRM-ISS001-ATM-001';

-- TMK for Acquirer Bank POS 1
INSERT INTO master_keys (
    master_key_id,
    id_bank,
    id_terminal,
    key_type,
    key_usage,
    algorithm,
    key_size,
    key_data_encrypted,
    key_fingerprint,
    key_checksum,
    generation_method,
    kdf_iterations,
    kdf_salt,
    status
)
SELECT
    'TMK-ACQ001-POS001-2024',
    b.id,
    t.id,
    'TMK',
    'KEY_ENCRYPTION',
    'AES-256',
    256,
    decode('cafebabecafebabecafebabecafebabecafebabecafebabecafebabecafebabe', 'hex'),
    'FP-TMK-ACQ001-POS001-SHA256-DEF456',
    'CK-TMK-ACQ001-POS001-UVW012',
    'SECURE_RANDOM',
    0,
    'SALT-TMK-ACQ001-POS001',
    'ACTIVE'
FROM banks b
CROSS JOIN terminals t
WHERE b.bank_code = 'ACQ001' AND t.terminal_id = 'TRM-ACQ001-POS-001';

-- ZMK between Issuer and Acquirer (shared key)
INSERT INTO master_keys (
    master_key_id,
    id_bank,
    key_type,
    key_usage,
    algorithm,
    key_size,
    key_data_encrypted,
    key_fingerprint,
    key_checksum,
    generation_method,
    kdf_iterations,
    kdf_salt,
    status
)
SELECT
    'ZMK-ISS001-ACQ001-2024',
    b.id,
    'ZMK',
    'KEY_ENCRYPTION',
    'AES-256',
    256,
    decode('0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef', 'hex'),
    'FP-ZMK-ISS001-ACQ001-SHA256-GHI789',
    'CK-ZMK-ISS001-ACQ001-RST345',
    'KEY_CEREMONY',
    100000,
    'SALT-ZMK-ISS001-ACQ001',
    'ACTIVE'
FROM banks b
WHERE b.bank_code = 'ISS001';

-- ZMK between Switch and Issuer
INSERT INTO master_keys (
    master_key_id,
    id_bank,
    key_type,
    key_usage,
    algorithm,
    key_size,
    key_data_encrypted,
    key_fingerprint,
    key_checksum,
    generation_method,
    kdf_iterations,
    kdf_salt,
    status
)
SELECT
    'ZMK-SWT001-ISS001-2024',
    b.id,
    'ZMK',
    'KEY_ENCRYPTION',
    'AES-256',
    256,
    decode('fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210', 'hex'),
    'FP-ZMK-SWT001-ISS001-SHA256-JKL012',
    'CK-ZMK-SWT001-ISS001-OPQ678',
    'KEY_CEREMONY',
    100000,
    'SALT-ZMK-SWT001-ISS001',
    'ACTIVE'
FROM banks b
WHERE b.bank_code = 'SWT001';

-- TPK for Issuer Bank Terminal 1 (encrypted under TMK)
INSERT INTO master_keys (
    master_key_id,
    id_bank,
    id_terminal,
    parent_key_id,
    key_type,
    key_usage,
    algorithm,
    key_size,
    key_data_encrypted,
    key_fingerprint,
    key_checksum,
    generation_method,
    kdf_iterations,
    kdf_salt,
    status
)
SELECT
    'TPK-ISS001-ATM001-2024',
    b.id,
    t.id,
    tmk.id,
    'TPK',
    'PIN_ENCRYPTION',
    'AES-256',
    256,
    decode('1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff', 'hex'),
    'FP-TPK-ISS001-ATM001-SHA256-MNO345',
    'CK-TPK-ISS001-ATM001-TUV901',
    'DERIVED',
    0,
    'SALT-TPK-ISS001-ATM001',
    'ACTIVE'
FROM banks b
CROSS JOIN terminals t
CROSS JOIN master_keys tmk
WHERE b.bank_code = 'ISS001'
  AND t.terminal_id = 'TRM-ISS001-ATM-001'
  AND tmk.master_key_id = 'TMK-ISS001-ATM001-2024';

-- ZPK between Issuer and Acquirer (encrypted under ZMK)
INSERT INTO master_keys (
    master_key_id,
    id_bank,
    parent_key_id,
    key_type,
    key_usage,
    algorithm,
    key_size,
    key_data_encrypted,
    key_fingerprint,
    key_checksum,
    generation_method,
    kdf_iterations,
    kdf_salt,
    status
)
SELECT
    'ZPK-ISS001-ACQ001-2024',
    b.id,
    zmk.id,
    'ZPK',
    'PIN_ENCRYPTION',
    'AES-256',
    256,
    decode('aaaa1111bbbb2222cccc3333dddd4444eeee5555ffff66660000777788889999', 'hex'),
    'FP-ZPK-ISS001-ACQ001-SHA256-PQR678',
    'CK-ZPK-ISS001-ACQ001-WXY234',
    'DERIVED',
    0,
    'SALT-ZPK-ISS001-ACQ001',
    'ACTIVE'
FROM banks b
CROSS JOIN master_keys zmk
WHERE b.bank_code = 'ISS001'
  AND zmk.master_key_id = 'ZMK-ISS001-ACQ001-2024';

-- ============================================================================
-- Sample Zone Key Exchange
-- Purpose: Demonstrate inter-bank key exchange workflow
-- ============================================================================

INSERT INTO zone_key_exchanges (
    exchange_id,
    id_source_bank,
    id_destination_bank,
    id_zmk,
    id_exchanged_key,
    exchange_type,
    key_transport_method,
    transport_key_fingerprint,
    exchange_status,
    initiated_by,
    acknowledged_by
)
SELECT
    'EXCHANGE-ISS001-ACQ001-ZPK-001',
    iss.id,
    acq.id,
    zmk.id,
    zpk.id,
    'INITIAL',
    'ENCRYPTED_UNDER_ZMK',
    'FP-TRANSPORT-ISS001-ACQ001-ABC',
    'ACTIVATED',
    'admin@issuer.com',
    'admin@acquirer.com'
FROM banks iss
CROSS JOIN banks acq
CROSS JOIN master_keys zmk
CROSS JOIN master_keys zpk
WHERE iss.bank_code = 'ISS001'
  AND acq.bank_code = 'ACQ001'
  AND zmk.master_key_id = 'ZMK-ISS001-ACQ001-2024'
  AND zpk.master_key_id = 'ZPK-ISS001-ACQ001-2024';

-- ============================================================================
-- Comments
-- ============================================================================
COMMENT ON TABLE banks IS 'Four-party model: Issuer, Acquirer, Switch, and additional Issuer bank';
COMMENT ON TABLE terminals IS 'Sample terminals including ATM, POS, and MPOS across different banks';
COMMENT ON TABLE key_custodians IS 'Sample custodians created for testing key ceremony workflows';

-- ============================================================================
-- End of Migration V2
-- ============================================================================
