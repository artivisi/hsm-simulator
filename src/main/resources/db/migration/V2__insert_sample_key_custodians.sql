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
-- NOTE: Master Keys Removed
-- ============================================================================
-- Sample keys have been removed because:
-- 1. In proper HSM architecture, LMK (Local Master Key) must be generated first
--    through a key ceremony
-- 2. All other keys (TMK, TPK, ZMK, ZPK, etc.) should be derived from LMK
-- 3. Without LMK, pre-populated keys serve no educational purpose
--
-- To use the simulator:
-- 1. Start a key ceremony at /hsm/initialize to generate LMK
-- 2. Generate other keys from /keys/generate after LMK exists
-- ============================================================================

-- ============================================================================
-- Comments
-- ============================================================================
COMMENT ON TABLE banks IS 'Four-party model: Issuer, Acquirer, Switch, and additional Issuer bank';
COMMENT ON TABLE terminals IS 'Sample terminals including ATM, POS, and MPOS across different banks';
COMMENT ON TABLE key_custodians IS 'Sample custodians created for testing key ceremony workflows';

-- ============================================================================
-- End of Migration V2
-- ============================================================================
