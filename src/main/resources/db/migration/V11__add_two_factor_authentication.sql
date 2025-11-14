-- Add Two-Factor Authentication support
-- Version 11.0.0

CREATE TABLE two_factor_auth (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    method VARCHAR(20) NOT NULL DEFAULT 'SMS',
    secret VARCHAR(100),
    phone_number VARCHAR(20),
    backup_codes TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_used_at TIMESTAMP,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_2fa_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_2fa_user ON two_factor_auth(user_id);
CREATE INDEX idx_2fa_enabled ON two_factor_auth(is_enabled);

