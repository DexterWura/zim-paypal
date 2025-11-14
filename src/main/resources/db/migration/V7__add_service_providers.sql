-- Add service providers and purchases tables
-- Version 7.0.0

CREATE TABLE service_providers (
    id BIGSERIAL PRIMARY KEY,
    provider_code VARCHAR(20) NOT NULL UNIQUE,
    provider_name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(20) NOT NULL,
    api_endpoint VARCHAR(500),
    api_key VARCHAR(500),
    api_secret VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    supports_airtime BOOLEAN DEFAULT FALSE,
    supports_data BOOLEAN DEFAULT FALSE,
    supports_tokens BOOLEAN DEFAULT FALSE,
    min_amount DECIMAL(19,2),
    max_amount DECIMAL(19,2),
    service_fee_percentage DECIMAL(5,2) DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_provider_code ON service_providers(provider_code);
CREATE INDEX idx_provider_active ON service_providers(is_active);

CREATE TABLE service_purchases (
    id BIGSERIAL PRIMARY KEY,
    reference_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    service_provider_id BIGINT NOT NULL,
    service_type VARCHAR(20) NOT NULL,
    recipient_number VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    service_fee DECIMAL(19,2) DEFAULT 0.00,
    total_amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id BIGINT,
    provider_response TEXT,
    provider_reference VARCHAR(100),
    error_message TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_provider FOREIGN KEY (service_provider_id) REFERENCES service_providers(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL
);

CREATE INDEX idx_purchase_provider ON service_purchases(service_provider_id);
CREATE INDEX idx_purchase_user ON service_purchases(user_id);
CREATE INDEX idx_purchase_status ON service_purchases(status);
CREATE INDEX idx_purchase_reference ON service_purchases(reference_number);

-- Insert default providers
INSERT INTO service_providers (provider_code, provider_name, provider_type, is_active, supports_airtime, supports_data, supports_tokens, min_amount, max_amount, service_fee_percentage) VALUES
('ECONET', 'Econet Wireless', 'TELECOM', TRUE, TRUE, TRUE, FALSE, 1.00, 1000.00, 2.50),
('NETONE', 'NetOne', 'TELECOM', TRUE, TRUE, TRUE, FALSE, 1.00, 1000.00, 2.50),
('TELECASH', 'Telecash', 'TELECOM', TRUE, TRUE, TRUE, FALSE, 1.00, 1000.00, 2.50),
('ZESA', 'Zimbabwe Electricity Supply Authority', 'UTILITY', TRUE, FALSE, FALSE, TRUE, 5.00, 10000.00, 1.50);

