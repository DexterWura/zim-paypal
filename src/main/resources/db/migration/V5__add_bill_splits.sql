-- Add bill splits tables
-- Version 5.0.0

CREATE TABLE bill_splits (
    id BIGSERIAL PRIMARY KEY,
    split_number VARCHAR(30) NOT NULL UNIQUE,
    creator_id BIGINT NOT NULL,
    description VARCHAR(200) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    split_method VARCHAR(20) NOT NULL DEFAULT 'EQUAL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_amount DECIMAL(19,2) DEFAULT 0.00,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bill_split_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bill_split_creator ON bill_splits(creator_id);
CREATE INDEX idx_bill_split_status ON bill_splits(status);
CREATE INDEX idx_bill_split_created ON bill_splits(created_at);

CREATE TABLE bill_split_participants (
    id BIGSERIAL PRIMARY KEY,
    bill_split_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    paid_amount DECIMAL(19,2) DEFAULT 0.00,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id BIGINT,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_participant_split FOREIGN KEY (bill_split_id) REFERENCES bill_splits(id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL
);

CREATE INDEX idx_participant_split ON bill_split_participants(bill_split_id);
CREATE INDEX idx_participant_user ON bill_split_participants(user_id);
CREATE INDEX idx_participant_status ON bill_split_participants(payment_status);

