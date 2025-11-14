-- Add money requests table
-- Version 2.0.0

CREATE TABLE money_requests (
    id BIGSERIAL PRIMARY KEY,
    request_number VARCHAR(30) NOT NULL UNIQUE,
    requester_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message VARCHAR(500),
    note VARCHAR(1000),
    transaction_id BIGINT,
    expires_at TIMESTAMP,
    reminder_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    CONSTRAINT fk_request_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL
);

CREATE INDEX idx_request_requester ON money_requests(requester_id);
CREATE INDEX idx_request_recipient ON money_requests(recipient_id);
CREATE INDEX idx_request_status ON money_requests(status);
CREATE INDEX idx_request_created ON money_requests(created_at);

-- Add money_request_id to transactions table
ALTER TABLE transactions ADD COLUMN money_request_id BIGINT;
ALTER TABLE transactions ADD CONSTRAINT fk_transaction_money_request 
    FOREIGN KEY (money_request_id) REFERENCES money_requests(id) ON DELETE SET NULL;
CREATE INDEX idx_transaction_money_request ON transactions(money_request_id);

