-- Add Payment Gateways
-- Version 22.0.0
-- Creates tables for payment gateways and their configurations

-- Payment Gateway table
CREATE TABLE IF NOT EXISTS payment_gateways (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    gateway_name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    gateway_type VARCHAR(50) NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    api_key VARCHAR(500),
    api_secret VARCHAR(500),
    merchant_id VARCHAR(200),
    webhook_url VARCHAR(500),
    callback_url VARCHAR(500),
    additional_config TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Payment Gateway Transactions (to track deposits via gateways)
CREATE TABLE IF NOT EXISTS gateway_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    gateway_id BIGINT NOT NULL,
    transaction_id BIGINT,
    gateway_transaction_id VARCHAR(200),
    amount DECIMAL(19, 2) NOT NULL,
    currency_code VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50),
    phone_number VARCHAR(20),
    email VARCHAR(100),
    gateway_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (gateway_id) REFERENCES payment_gateways(id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    INDEX idx_gateway_transaction_id (gateway_transaction_id),
    INDEX idx_user_gateway (user_id, gateway_id),
    INDEX idx_status (status)
);

-- Insert default payment gateways (if not exists)
INSERT INTO payment_gateways (gateway_name, display_name, gateway_type, is_enabled)
SELECT 'ECOCASH', 'EcoCash', 'MOBILE_MONEY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_gateways WHERE gateway_name = 'ECOCASH');

INSERT INTO payment_gateways (gateway_name, display_name, gateway_type, is_enabled)
SELECT 'PAYNOW_ZIM', 'PayNow Zimbabwe', 'MOBILE_MONEY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_gateways WHERE gateway_name = 'PAYNOW_ZIM');

INSERT INTO payment_gateways (gateway_name, display_name, gateway_type, is_enabled)
SELECT 'PAYPAL', 'PayPal', 'ONLINE', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_gateways WHERE gateway_name = 'PAYPAL');

