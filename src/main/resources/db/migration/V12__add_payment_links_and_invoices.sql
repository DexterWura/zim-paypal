-- Add Payment Links and Invoicing System
-- Version 12.0.0

-- Create payment_links table
CREATE TABLE payment_links (
    id BIGSERIAL PRIMARY KEY,
    link_code VARCHAR(50) NOT NULL UNIQUE,
    creator_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    amount DECIMAL(19,2) NOT NULL,
    currency_id BIGINT,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    link_type VARCHAR(20) NOT NULL DEFAULT 'ONE_TIME',
    max_uses INTEGER,
    current_uses INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    allow_partial_payment BOOLEAN NOT NULL DEFAULT FALSE,
    collect_shipping_address BOOLEAN NOT NULL DEFAULT FALSE,
    collect_phone_number BOOLEAN NOT NULL DEFAULT FALSE,
    email_notification BOOLEAN NOT NULL DEFAULT TRUE,
    return_url VARCHAR(500),
    cancel_url VARCHAR(500),
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_link_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_link_currency FOREIGN KEY (currency_id) REFERENCES currencies(id) ON DELETE SET NULL
);

CREATE INDEX idx_payment_link_code ON payment_links(link_code);
CREATE INDEX idx_payment_link_creator ON payment_links(creator_id);
CREATE INDEX idx_payment_link_status ON payment_links(status);

-- Create invoices table
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    customer_id BIGINT,
    customer_email VARCHAR(100),
    customer_name VARCHAR(200),
    customer_address VARCHAR(500),
    invoice_date DATE NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    subtotal DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(19,2) DEFAULT 0.00,
    discount_amount DECIMAL(19,2) DEFAULT 0.00,
    total_amount DECIMAL(19,2) NOT NULL,
    currency_id BIGINT,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    notes TEXT,
    terms TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    CONSTRAINT fk_invoice_merchant FOREIGN KEY (merchant_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_invoice_currency FOREIGN KEY (currency_id) REFERENCES currencies(id) ON DELETE SET NULL
);

CREATE INDEX idx_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoice_merchant ON invoices(merchant_id);
CREATE INDEX idx_invoice_customer ON invoices(customer_id);
CREATE INDEX idx_invoice_status ON invoices(status);

-- Add invoice_id to transactions table
ALTER TABLE transactions ADD COLUMN invoice_id BIGINT;
ALTER TABLE transactions ADD CONSTRAINT fk_transaction_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL;
CREATE INDEX idx_transaction_invoice ON transactions(invoice_id);

-- Create invoice_items table
CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(19,2) NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    tax_rate DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_item_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_item_invoice ON invoice_items(invoice_id);

