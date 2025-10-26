-- ============================================================================
-- Migration: V3__create_users_table.sql
-- Purpose: Create users table for authentication and authorization
-- ============================================================================

-- ============================================================================
-- Table: users
-- Purpose: Store system users with authentication credentials
-- ============================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- BCrypt hashed
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN', -- ADMIN, OPERATOR, AUDITOR
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'OPERATOR', 'AUDITOR'))
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(active);

-- Insert default admin user
-- Username: admin
-- Password: admin123 (BCrypt hash for 'admin123')
-- NOTE: Change this password in production!
INSERT INTO users (username, email, password, full_name, role, active) VALUES
('admin', 'admin@hsm-simulator.local', '$2a$10$XiLG2cUTyrhavQ0EDwicIOma78/x/bNX/V3PUMNJQuRumgwTLt4dy', 'System Administrator', 'ADMIN', TRUE);
