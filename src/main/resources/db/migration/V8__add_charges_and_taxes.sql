-- Add charges and taxes tables
-- Version 8.0.0

CREATE TABLE charges (
    id BIGSERIAL PRIMARY KEY,
    charge_name VARCHAR(100) NOT NULL,
    charge_code VARCHAR(50) NOT NULL UNIQUE,
    charge_type VARCHAR(50) NOT NULL,
    transaction_type VARCHAR(50),
    charge_method VARCHAR(50) NOT NULL,
    fixed_amount DECIMAL(19,2),
    percentage_rate DECIMAL(5,2),
    min_amount DECIMAL(19,2),
    max_amount DECIMAL(19,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    regulation_reference VARCHAR(200),
    created_by_id BIGINT,
    updated_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_charge_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_charge_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_charge_type ON charges(charge_type);
CREATE INDEX idx_charge_active ON charges(is_active);

CREATE TABLE taxes (
    id BIGSERIAL PRIMARY KEY,
    tax_name VARCHAR(100) NOT NULL,
    tax_code VARCHAR(50) NOT NULL UNIQUE,
    tax_type VARCHAR(50) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL,
    transaction_type VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    regulation_reference VARCHAR(200),
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    created_by_id BIGINT,
    updated_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tax_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_tax_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_tax_code ON taxes(tax_code);
CREATE INDEX idx_tax_active ON taxes(is_active);

-- Insert default charges
INSERT INTO charges (charge_name, charge_code, charge_type, transaction_type, charge_method, fixed_amount, percentage_rate, is_active, description, regulation_reference) VALUES
('Transfer Fee', 'TRANSFER_FEE', 'TRANSFER_FEE', 'TRANSFER', 'PERCENTAGE', NULL, 1.50, TRUE, 'Fee charged for money transfers', 'Regulation 2024-001'),
('Withdrawal Fee', 'WITHDRAWAL_FEE', 'WITHDRAWAL_FEE', 'WITHDRAWAL', 'FIXED', 2.00, NULL, TRUE, 'Fixed fee for account withdrawals', 'Regulation 2024-002'),
('Payment Processing Fee', 'PAYMENT_FEE', 'PAYMENT_FEE', 'PAYMENT', 'PERCENTAGE', NULL, 2.00, TRUE, 'Fee for processing payments', 'Regulation 2024-003');

-- Insert default taxes
INSERT INTO taxes (tax_name, tax_code, tax_type, tax_rate, transaction_type, is_active, description, regulation_reference, effective_from) VALUES
('Value Added Tax', 'VAT', 'VAT', 15.00, NULL, TRUE, 'Standard VAT rate', 'VAT Act 2024', CURRENT_TIMESTAMP),
('Transaction Tax', 'TRANSACTION_TAX', 'TRANSACTION_TAX', 0.50, NULL, TRUE, 'Tax on all transactions', 'Finance Act 2024', CURRENT_TIMESTAMP);

