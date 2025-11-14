-- Add Webhooks System
-- Version 14.0.0

-- Create webhooks table
CREATE TABLE webhooks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_webhook_user ON webhooks(user_id);
CREATE INDEX idx_webhook_active ON webhooks(is_active);

-- Create webhook_events table (for event types subscription)
CREATE TABLE webhook_events (
    webhook_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_webhook_event_webhook FOREIGN KEY (webhook_id) REFERENCES webhooks(id) ON DELETE CASCADE,
    PRIMARY KEY (webhook_id, event_type)
);

-- Create webhook_event_deliveries table (for tracking deliveries)
-- Note: This table name matches WebhookEvent entity
CREATE TABLE webhook_event_deliveries (
    id BIGSERIAL PRIMARY KEY,
    webhook_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    response_code INTEGER,
    response_body TEXT,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_delivery_webhook FOREIGN KEY (webhook_id) REFERENCES webhooks(id) ON DELETE CASCADE
);

CREATE INDEX idx_webhook_delivery_webhook ON webhook_event_deliveries(webhook_id);
CREATE INDEX idx_webhook_delivery_status ON webhook_event_deliveries(status);
CREATE INDEX idx_webhook_delivery_created ON webhook_event_deliveries(created_at);

