-- Add multi-currency support and account limits
-- Version 9.0.0

-- Create currencies table first
CREATE TABLE currencies (
    id BIGSERIAL PRIMARY KEY,
    currency_code VARCHAR(3) NOT NULL UNIQUE,
    currency_name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_base_currency BOOLEAN NOT NULL DEFAULT FALSE,
    decimal_places INTEGER NOT NULL DEFAULT 2,
    created_by_id BIGINT,
    updated_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_currency_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_currency_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_currency_code ON currencies(currency_code);
CREATE INDEX idx_currency_active ON currencies(is_active);

-- Create exchange_rates table
CREATE TABLE exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    from_currency_id BIGINT NOT NULL,
    to_currency_id BIGINT NOT NULL,
    rate DECIMAL(19,6) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id BIGINT,
    updated_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rate_from_currency FOREIGN KEY (from_currency_id) REFERENCES currencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_rate_to_currency FOREIGN KEY (to_currency_id) REFERENCES currencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_rate_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_rate_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_rate_from_currency ON exchange_rates(from_currency_id);
CREATE INDEX idx_rate_to_currency ON exchange_rates(to_currency_id);
CREATE INDEX idx_rate_effective ON exchange_rates(effective_from, effective_to);

-- Add currency_id to accounts table (after currencies table is created)
ALTER TABLE accounts ADD COLUMN currency_id BIGINT;
ALTER TABLE accounts ADD CONSTRAINT fk_account_currency FOREIGN KEY (currency_id) REFERENCES currencies(id) ON DELETE SET NULL;
CREATE INDEX idx_account_currency ON accounts(currency_id);

-- Create account_limits table
CREATE TABLE account_limits (
    id BIGSERIAL PRIMARY KEY,
    limit_name VARCHAR(100) NOT NULL,
    limit_code VARCHAR(50) NOT NULL UNIQUE,
    limit_type VARCHAR(50) NOT NULL,
    period_type VARCHAR(20),
    max_accounts_per_user INTEGER,
    max_transaction_amount DECIMAL(19,2),
    max_daily_amount DECIMAL(19,2),
    max_weekly_amount DECIMAL(19,2),
    max_monthly_amount DECIMAL(19,2),
    max_daily_count INTEGER,
    max_weekly_count INTEGER,
    max_monthly_count INTEGER,
    user_role VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_by_id BIGINT,
    updated_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_limit_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_limit_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_limit_type ON account_limits(limit_type);
CREATE INDEX idx_limit_active ON account_limits(is_active);

-- Insert default currencies
INSERT INTO currencies (currency_code, currency_name, symbol, is_active, is_base_currency, decimal_places) VALUES
('USD', 'US Dollar', '$', TRUE, TRUE, 2),
('ZWL', 'Zimbabwean Dollar', 'Z$', TRUE, FALSE, 2),
('EUR', 'Euro', '€', TRUE, FALSE, 2),
('GBP', 'British Pound', '£', TRUE, FALSE, 2),
('ZAR', 'South African Rand', 'R', TRUE, FALSE, 2);

-- Insert default exchange rates (example rates - should be updated by admin)
INSERT INTO exchange_rates (from_currency_id, to_currency_id, rate, effective_from, is_active) 
SELECT 
    (SELECT id FROM currencies WHERE currency_code = 'USD'),
    (SELECT id FROM currencies WHERE currency_code = 'ZWL'),
    35.00,
    CURRENT_TIMESTAMP,
    TRUE
WHERE EXISTS (SELECT 1 FROM currencies WHERE currency_code = 'USD')
AND EXISTS (SELECT 1 FROM currencies WHERE currency_code = 'ZWL');

-- Insert default account limits
INSERT INTO account_limits (limit_name, limit_code, limit_type, period_type, max_accounts_per_user, max_transaction_amount, max_daily_amount, max_weekly_amount, max_monthly_amount, user_role, is_active, description) VALUES
('Standard User Account Limit', 'USER_ACCOUNT_LIMIT', 'ACCOUNT_COUNT', NULL, 5, NULL, NULL, NULL, NULL, 'USER', TRUE, 'Maximum 5 accounts per standard user'),
('Standard User Transaction Limit', 'USER_TRANSACTION_LIMIT', 'TRANSACTION_AMOUNT', 'DAILY', NULL, 10000.00, 50000.00, 200000.00, 500000.00, 'USER', TRUE, 'Transaction limits for standard users'),
('Merchant Account Limit', 'MERCHANT_ACCOUNT_LIMIT', 'ACCOUNT_COUNT', NULL, 10, NULL, NULL, NULL, NULL, 'MERCHANT', TRUE, 'Maximum 10 accounts per merchant'),
('Merchant Transaction Limit', 'MERCHANT_TRANSACTION_LIMIT', 'TRANSACTION_AMOUNT', 'DAILY', NULL, 100000.00, 500000.00, 2000000.00, 10000000.00, 'MERCHANT', TRUE, 'Transaction limits for merchants');

