-- Add Subscriptions and Recurring Payments
-- Version 13.0.0

-- Create subscription_plans table
CREATE TABLE subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    plan_name VARCHAR(100) NOT NULL,
    plan_code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    amount DECIMAL(19,2) NOT NULL,
    currency_id BIGINT,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    billing_cycle VARCHAR(20) NOT NULL,
    trial_period_days INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id BIGINT,
    updated_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_currency FOREIGN KEY (currency_id) REFERENCES currencies(id) ON DELETE SET NULL,
    CONSTRAINT fk_plan_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_plan_updated_by FOREIGN KEY (updated_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_plan_code ON subscription_plans(plan_code);
CREATE INDEX idx_plan_active ON subscription_plans(is_active);

-- Create recurring_payments table
CREATE TABLE recurring_payments (
    id BIGSERIAL PRIMARY KEY,
    subscription_id VARCHAR(50) NOT NULL UNIQUE,
    subscriber_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    plan_id BIGINT,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date TIMESTAMP NOT NULL,
    next_payment_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    trial_end_date TIMESTAMP,
    total_payments INTEGER NOT NULL DEFAULT 0,
    failed_payments INTEGER NOT NULL DEFAULT 0,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recurring_subscriber FOREIGN KEY (subscriber_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_merchant FOREIGN KEY (merchant_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE SET NULL,
    CONSTRAINT fk_recurring_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_recurring_subscriber ON recurring_payments(subscriber_id);
CREATE INDEX idx_recurring_merchant ON recurring_payments(merchant_id);
CREATE INDEX idx_recurring_status ON recurring_payments(status);
CREATE INDEX idx_recurring_next_payment ON recurring_payments(next_payment_date);

-- Insert default subscription plans
INSERT INTO subscription_plans (plan_name, plan_code, description, amount, billing_cycle, is_active) VALUES
('Basic Monthly', 'BASIC_MONTHLY', 'Basic subscription plan', 9.99, 'MONTHLY', TRUE),
('Premium Monthly', 'PREMIUM_MONTHLY', 'Premium subscription plan', 19.99, 'MONTHLY', TRUE),
('Basic Yearly', 'BASIC_YEARLY', 'Basic yearly subscription', 99.99, 'YEARLY', TRUE),
('Premium Yearly', 'PREMIUM_YEARLY', 'Premium yearly subscription', 199.99, 'YEARLY', TRUE);

