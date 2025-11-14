-- Add transaction reversals table
-- Version 6.0.0

CREATE TABLE transaction_reversals (
    id BIGSERIAL PRIMARY KEY,
    reversal_number VARCHAR(30) NOT NULL UNIQUE,
    transaction_id BIGINT NOT NULL,
    requested_by_id BIGINT NOT NULL,
    processed_by_id BIGINT,
    reversal_amount DECIMAL(19,2) NOT NULL,
    reversal_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason TEXT NOT NULL,
    admin_notes TEXT,
    reversal_transaction_id BIGINT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reversal_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_reversal_requested_by FOREIGN KEY (requested_by_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reversal_processed_by FOREIGN KEY (processed_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_reversal_reversal_transaction FOREIGN KEY (reversal_transaction_id) REFERENCES transactions(id) ON DELETE SET NULL
);

CREATE INDEX idx_reversal_transaction ON transaction_reversals(transaction_id);
CREATE INDEX idx_reversal_user ON transaction_reversals(requested_by_id);
CREATE INDEX idx_reversal_status ON transaction_reversals(status);
CREATE INDEX idx_reversal_created ON transaction_reversals(created_at);

