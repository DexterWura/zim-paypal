-- Add rewards table
-- Version 3.0.0

CREATE TABLE rewards (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    points INTEGER NOT NULL DEFAULT 0,
    cash_back_value DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    total_earned DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    total_redeemed DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    lifetime_points INTEGER NOT NULL DEFAULT 0,
    tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rewards_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_rewards_user ON rewards(user_id);

