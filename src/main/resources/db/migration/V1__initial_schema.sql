-- Flyway migration: V1__initial_schema.sql
-- This is the initial database schema for Sahastra Digital Backend

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create audit_log table for recording all sensitive actions
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(50) NOT NULL,
    actor_id VARCHAR(255),
    actor_type VARCHAR(50) NOT NULL,
    target_entity VARCHAR(100) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    changes JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(36)
);

CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);
CREATE INDEX idx_audit_log_target ON audit_log(target_entity, target_id);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);

-- Create system_setting table for configurable security and application settings
CREATE TABLE system_setting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description TEXT,
    value_type VARCHAR(50) NOT NULL,
    min_value VARCHAR(255),
    max_value VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_setting_key ON system_setting(setting_key);

-- Insert default security settings
INSERT INTO system_setting (setting_key, setting_value, description, value_type, min_value, max_value) VALUES
    ('ACCESS_TOKEN_EXPIRY_MINUTES', '15', 'Access token expiry time in minutes', 'INTEGER', '1', '1440'),
    ('REFRESH_TOKEN_EXPIRY_DAYS', '7', 'Customer refresh token expiry time in days', 'INTEGER', '1', '365'),
    ('ADMIN_REFRESH_TOKEN_EXPIRY_HOURS', '8', 'Admin refresh token expiry time in hours', 'INTEGER', '1', '168'),
    ('REMEMBER_ME_EXPIRY_DAYS', '30', 'Remember-me token expiry time in days', 'INTEGER', '1', '365'),
    ('ABSOLUTE_SESSION_EXPIRY_DAYS', '30', 'Absolute session expiry time in days', 'INTEGER', '1', '365'),
    ('IDLE_TIMEOUT_DAYS', '14', 'Idle session timeout in days', 'INTEGER', '1', '365'),
    ('OTP_EXPIRY_MINUTES', '5', 'OTP expiry time in minutes', 'INTEGER', '1', '60'),
    ('OTP_MAX_ATTEMPTS', '5', 'Maximum OTP verification attempts', 'INTEGER', '1', '20'),
    ('LOGIN_RATE_LIMIT_PER_MINUTE', '5', 'Login attempts per minute per IP', 'INTEGER', '1', '100')
ON CONFLICT (setting_key) DO NOTHING;

COMMIT;
