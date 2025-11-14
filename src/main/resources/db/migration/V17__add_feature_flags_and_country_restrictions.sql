-- Add Feature Flags and Country Restrictions
-- Version 17.0.0

-- Create feature_flags table
CREATE TABLE feature_flags (
    id BIGSERIAL PRIMARY KEY,
    feature_name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    description TEXT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    category VARCHAR(50),
    requires_restart BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_feature_flag_name ON feature_flags(feature_name);
CREATE INDEX idx_feature_flag_enabled ON feature_flags(is_enabled);

-- Create country_restrictions table
CREATE TABLE country_restrictions (
    id BIGSERIAL PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL UNIQUE,
    country_name VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_registration_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    is_transaction_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    is_merchant_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_country_code ON country_restrictions(country_code);
CREATE INDEX idx_country_enabled ON country_restrictions(is_enabled);

-- Insert default feature flags
INSERT INTO feature_flags (feature_name, display_name, description, category, is_enabled) VALUES
('PAYMENT_LINKS', 'Payment Links', 'Allow users to create shareable payment links', 'PAYMENT', TRUE),
('QR_CODE_PAYMENTS', 'QR Code Payments', 'Enable QR code generation for payments', 'PAYMENT', TRUE),
('INVOICING', 'Invoicing System', 'Allow users to create and send invoices', 'PAYMENT', TRUE),
('RECURRING_PAYMENTS', 'Recurring Payments', 'Enable subscription and recurring payment features', 'PAYMENT', TRUE),
('MERCHANT_TOOLS', 'Merchant Tools', 'Enable payment buttons and merchant features', 'MERCHANT', TRUE),
('WEBHOOKS', 'Webhooks', 'Enable webhook notifications for integrations', 'INTEGRATION', TRUE),
('TWO_FACTOR_AUTH', 'Two-Factor Authentication', 'Enable 2FA for enhanced security', 'SECURITY', TRUE),
('SERVICE_PURCHASES', 'Service Purchases', 'Allow purchases of airtime, data, and utilities', 'SERVICES', TRUE),
('BILL_SPLITS', 'Bill Splitting', 'Enable bill splitting functionality', 'PAYMENT', TRUE),
('TRANSACTION_REVERSALS', 'Transaction Reversals', 'Allow users to request transaction reversals', 'PAYMENT', TRUE),
('MONEY_REQUESTS', 'Money Requests', 'Enable money request feature', 'PAYMENT', TRUE),
('REWARDS_POINTS', 'Rewards & Points', 'Enable rewards and points system', 'LOYALTY', TRUE),
('MULTI_CURRENCY', 'Multi-Currency', 'Enable multiple currency support', 'PAYMENT', TRUE),
('COUNTRY_RESTRICTIONS', 'Country Restrictions', 'Enable country-based restrictions', 'SECURITY', TRUE);

-- Insert common countries (you can add more as needed)
INSERT INTO country_restrictions (country_code, country_name, is_enabled, is_registration_allowed, is_transaction_allowed, is_merchant_allowed) VALUES
('US', 'United States', TRUE, TRUE, TRUE, TRUE),
('GB', 'United Kingdom', TRUE, TRUE, TRUE, TRUE),
('ZW', 'Zimbabwe', TRUE, TRUE, TRUE, TRUE),
('ZA', 'South Africa', TRUE, TRUE, TRUE, TRUE),
('KE', 'Kenya', TRUE, TRUE, TRUE, TRUE),
('NG', 'Nigeria', TRUE, TRUE, TRUE, TRUE),
('GH', 'Ghana', TRUE, TRUE, TRUE, TRUE),
('TZ', 'Tanzania', TRUE, TRUE, TRUE, TRUE),
('UG', 'Uganda', TRUE, TRUE, TRUE, TRUE),
('RW', 'Rwanda', TRUE, TRUE, TRUE, TRUE);

