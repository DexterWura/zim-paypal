-- Initial schema creation for Zim PayPal Clone
-- Version 1.0.0

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    account_enabled BOOLEAN DEFAULT TRUE,
    account_locked BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);

-- Accounts table
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    account_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_account_user ON accounts(user_id);
CREATE INDEX idx_account_number ON accounts(account_number);

-- Cards table
CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    card_number VARCHAR(19) NOT NULL,
    last_four_digits VARCHAR(4) NOT NULL,
    cardholder_name VARCHAR(100) NOT NULL,
    expiry_date DATE NOT NULL,
    cvv VARCHAR(4) NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    card_brand VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_default BOOLEAN DEFAULT FALSE,
    billing_address VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(50),
    billing_zip VARCHAR(20),
    billing_country VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_card_user ON cards(user_id);
CREATE INDEX idx_card_last_four ON cards(last_four_digits);

-- Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_number VARCHAR(30) NOT NULL UNIQUE,
    sender_id BIGINT,
    receiver_id BIGINT,
    account_id BIGINT,
    card_id BIGINT,
    amount DECIMAL(19,2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    transaction_type VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(500),
    reference_number VARCHAR(50),
    fee DECIMAL(19,2) DEFAULT 0.00,
    net_amount DECIMAL(19,2),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_transaction_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_receiver FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE SET NULL
);

CREATE INDEX idx_transaction_sender ON transactions(sender_id);
CREATE INDEX idx_transaction_receiver ON transactions(receiver_id);
CREATE INDEX idx_transaction_account ON transactions(account_id);
CREATE INDEX idx_transaction_card ON transactions(card_id);
CREATE INDEX idx_transaction_type ON transactions(transaction_type);
CREATE INDEX idx_transaction_status ON transactions(status);
CREATE INDEX idx_transaction_created ON transactions(created_at);

-- Statements table
CREATE TABLE statements (
    id BIGSERIAL PRIMARY KEY,
    statement_number VARCHAR(30) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    opening_balance DECIMAL(19,2) NOT NULL,
    closing_balance DECIMAL(19,2) NOT NULL,
    total_credits DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    total_debits DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    transaction_count INTEGER DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    statement_type VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    file_path VARCHAR(500),
    generated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_statement_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_statement_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_statement_user ON statements(user_id);
CREATE INDEX idx_statement_account ON statements(account_id);
CREATE INDEX idx_statement_period ON statements(start_date, end_date);

-- Notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    error_message VARCHAR(500),
    retry_count INTEGER DEFAULT 0,
    reference_id VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_user ON notifications(user_id);
CREATE INDEX idx_notification_type ON notifications(notification_type);
CREATE INDEX idx_notification_status ON notifications(status);
CREATE INDEX idx_notification_created ON notifications(created_at);

