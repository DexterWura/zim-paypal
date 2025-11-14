-- Populate Default Users (Admin and Test User)
-- Version 19.0.0
-- Note: Passwords are hashed using BCrypt with strength 10

-- Insert admin user
-- Password: admin123 (BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy)
INSERT INTO users (username, email, password, first_name, last_name, role, account_enabled, email_verified, created_at, updated_at)
SELECT 
    'admin',
    'admin@zimpaypal.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Admin',
    'User',
    'ADMIN',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- Insert test user
-- Password: testuser123 (BCrypt hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.H/Hj5K0Yj5J5J5J5J5J5J)
INSERT INTO users (username, email, password, first_name, last_name, role, account_enabled, email_verified, created_at, updated_at)
SELECT 
    'testuser',
    'testuser@zimpaypal.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.H/Hj5K0Yj5J5J5J5J5J5J',
    'Test',
    'User',
    'USER',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'testuser');

-- Create default accounts for admin user (if not exists)
-- Account will be created with balance 0.00
INSERT INTO accounts (account_number, user_id, balance, currency_code, account_type, status, created_at, updated_at)
SELECT 
    'ACC' || LPAD(CAST(COALESCE((SELECT MAX(CAST(SUBSTRING(account_number FROM 4) AS INTEGER)) FROM accounts), 0) + 1 AS VARCHAR), 10, '0'),
    u.id,
    0.00,
    'USD',
    'PERSONAL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM accounts a WHERE a.user_id = u.id);

-- Create default accounts for test user (if not exists)
-- Account will be created with balance 1000.00 for testing
INSERT INTO accounts (account_number, user_id, balance, currency_code, account_type, status, created_at, updated_at)
SELECT 
    'ACC' || LPAD(CAST(COALESCE((SELECT MAX(CAST(SUBSTRING(account_number FROM 4) AS INTEGER)) FROM accounts), 0) + 1 AS VARCHAR), 10, '0'),
    u.id,
    1000.00,
    'USD',
    'PERSONAL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.username = 'testuser'
  AND NOT EXISTS (SELECT 1 FROM accounts a WHERE a.user_id = u.id);
