CREATE TABLE IF NOT EXISTS system_setting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description TEXT,
    value_type VARCHAR(50) NOT NULL,
    min_value VARCHAR(255),
    max_value VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_setting (setting_key, setting_value, description, value_type, min_value, max_value)
VALUES
    ('security.jwt.access-token-expiry-minutes', '15', 'Access token lifetime in minutes', 'INTEGER', '1', '60'),
    ('security.jwt.refresh-token-expiry-days', '7', 'Customer refresh token lifetime in days', 'INTEGER', '1', '30'),
    ('security.jwt.remember-me-expiry-days', '30', 'Remember-me token lifetime in days', 'INTEGER', '1', '30'),
    ('security.jwt.admin-refresh-token-expiry-hours', '8', 'Admin refresh token lifetime in hours', 'INTEGER', '1', '24'),
    ('security.jwt.absolute-token-cap-days', '30', 'Maximum token lifetime in days', 'INTEGER', '1', '30'),
    ('security.session.idle-timeout-days', '14', 'Session idle timeout in days', 'INTEGER', '1', '30'),
    ('security.otp.expiry-minutes', '5', 'OTP lifetime in minutes', 'INTEGER', '1', '15'),
    ('security.otp.max-attempts', '5', 'Maximum OTP verification attempts', 'INTEGER', '1', '10'),
    ('security.rate-limit.login-per-minute', '5', 'Login attempts allowed per minute', 'INTEGER', '1', '20')
ON CONFLICT (setting_key) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_system_setting_key ON system_setting(setting_key);
