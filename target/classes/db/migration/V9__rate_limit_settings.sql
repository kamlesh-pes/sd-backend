INSERT INTO system_setting (setting_key, setting_value, description, value_type, min_value, max_value)
VALUES
    ('security.rate-limit.otp-per-minute', '5', 'OTP and password-reset requests allowed per minute', 'INTEGER', '1', '20'),
    ('security.rate-limit.api-per-minute', '120', 'API requests allowed per minute per client address', 'INTEGER', '10', '1000')
ON CONFLICT (setting_key) DO NOTHING;
