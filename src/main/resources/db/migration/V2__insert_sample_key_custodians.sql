-- ============================================================================
-- HSM Simulator - Sample Key Custodians for Testing
-- Version: 1.0
-- Purpose: Insert sample custodian data for development and testing
-- ============================================================================

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
        'alice.johnson@example.com',
        '+62-812-3456-7890',
        'IT Security',
        'ACTIVE',
        'system'
    ),
    (
        'CUST-B-002',
        'Bob Williams',
        'bob.williams@example.com',
        '+62-812-3456-7891',
        'IT Security',
        'ACTIVE',
        'system'
    ),
    (
        'CUST-C-003',
        'Carol Martinez',
        'carol.martinez@example.com',
        '+62-812-3456-7892',
        'IT Security',
        'ACTIVE',
        'system'
    );

-- ============================================================================
-- Comments
-- ============================================================================
COMMENT ON TABLE key_custodians IS 'Sample custodians created for testing key ceremony workflows';

-- ============================================================================
-- End of Migration V2
-- ============================================================================
