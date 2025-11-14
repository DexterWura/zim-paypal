-- Add fraud detection, AML, and theft controls
-- Version 10.0.0

-- Create risk_scores table
CREATE TABLE risk_scores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    transaction_id BIGINT,
    risk_score DECIMAL(5,2) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    score_type VARCHAR(50) NOT NULL,
    risk_factors TEXT,
    description TEXT,
    created_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_risk_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_risk_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_risk_user ON risk_scores(user_id);
CREATE INDEX idx_risk_score ON risk_scores(risk_score);
CREATE INDEX idx_risk_created ON risk_scores(created_at);

-- Create suspicious_activities table
CREATE TABLE suspicious_activities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    transaction_id BIGINT,
    activity_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT NOT NULL,
    risk_factors TEXT,
    auto_detected BOOLEAN NOT NULL DEFAULT TRUE,
    reviewed_by_id BIGINT,
    review_notes TEXT,
    reviewed_at TIMESTAMP,
    created_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_suspicious_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_suspicious_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_suspicious_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_suspicious_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_suspicious_user ON suspicious_activities(user_id);
CREATE INDEX idx_suspicious_transaction ON suspicious_activities(transaction_id);
CREATE INDEX idx_suspicious_status ON suspicious_activities(status);
CREATE INDEX idx_suspicious_created ON suspicious_activities(created_at);

-- Create fraud_rules table
CREATE TABLE fraud_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_type VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    threshold_amount DECIMAL(19,2),
    threshold_count INTEGER,
    time_window_minutes INTEGER,
    risk_score_threshold DECIMAL(5,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    rule_conditions TEXT,
    created_by_id BIGINT,
    updated_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rule_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_rule_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_rule_code ON fraud_rules(rule_code);
CREATE INDEX idx_rule_active ON fraud_rules(is_active);

-- Create kyc_verifications table
CREATE TABLE kyc_verifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    verification_level VARCHAR(20) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    document_type VARCHAR(50),
    document_number VARCHAR(100),
    document_path VARCHAR(500),
    date_of_birth DATE,
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    phone_number VARCHAR(20),
    verification_notes TEXT,
    verified_by_id BIGINT,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kyc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_kyc_verified_by FOREIGN KEY (verified_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_kyc_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_kyc_user ON kyc_verifications(user_id);
CREATE INDEX idx_kyc_status ON kyc_verifications(verification_status);
CREATE INDEX idx_kyc_level ON kyc_verifications(verification_level);

-- Insert default fraud rules
INSERT INTO fraud_rules (rule_name, rule_code, rule_type, action_type, threshold_amount, threshold_count, time_window_minutes, is_active, description) VALUES
('Large Amount Detection', 'LARGE_AMOUNT_RULE', 'AMOUNT_THRESHOLD', 'FLAG', 10000.00, NULL, NULL, TRUE, 'Flag transactions exceeding $10,000'),
('Rapid Transfer Detection', 'RAPID_TRANSFER_RULE', 'VELOCITY_CHECK', 'FLAG', NULL, 10, 60, TRUE, 'Flag if 10+ transactions in 60 minutes'),
('Structuring Detection', 'STRUCTURING_RULE', 'STRUCTURING_DETECTION', 'REVIEW', 9500.00, NULL, NULL, TRUE, 'Detect transactions just below reporting threshold'),
('New Account Risk', 'NEW_ACCOUNT_RULE', 'ACCOUNT_AGE', 'REVIEW', NULL, NULL, NULL, TRUE, 'Review transactions from accounts less than 7 days old'),
('High Frequency Detection', 'HIGH_FREQUENCY_RULE', 'TRANSACTION_FREQUENCY', 'FLAG', NULL, 50, 1440, TRUE, 'Flag if 50+ transactions in 24 hours');

