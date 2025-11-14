-- Add Merchant Tools (Payment Buttons and API Keys)
-- Version 16.0.0

-- Create payment_buttons table
CREATE TABLE payment_buttons (
    id BIGSERIAL PRIMARY KEY,
    button_code VARCHAR(50) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    button_name VARCHAR(200) NOT NULL,
    description TEXT,
    amount DECIMAL(19,2),
    allow_custom_amount BOOLEAN NOT NULL DEFAULT FALSE,
    currency_id BIGINT,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    button_style VARCHAR(20) NOT NULL DEFAULT 'DEFAULT',
    button_size VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    button_color VARCHAR(7) DEFAULT '#0070BA',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    success_url VARCHAR(500),
    cancel_url VARCHAR(500),
    notify_url VARCHAR(500),
    total_clicks INTEGER NOT NULL DEFAULT 0,
    total_payments INTEGER NOT NULL DEFAULT 0,
    total_revenue DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_button_merchant FOREIGN KEY (merchant_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_button_currency FOREIGN KEY (currency_id) REFERENCES currencies(id) ON DELETE SET NULL
);

CREATE INDEX idx_button_code ON payment_buttons(button_code);
CREATE INDEX idx_button_merchant ON payment_buttons(merchant_id);
CREATE INDEX idx_button_active ON payment_buttons(is_active);

-- Create merchant_api_keys table
CREATE TABLE merchant_api_keys (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    api_key VARCHAR(100) NOT NULL UNIQUE,
    api_secret VARCHAR(100) NOT NULL,
    key_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    usage_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_api_key_merchant FOREIGN KEY (merchant_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_api_key_merchant ON merchant_api_keys(merchant_id);
CREATE INDEX idx_api_key_key ON merchant_api_keys(api_key);
CREATE INDEX idx_api_key_active ON merchant_api_keys(is_active);

