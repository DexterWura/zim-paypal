-- Add country_code to users table
-- Version 18.0.0

ALTER TABLE users ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);

CREATE INDEX IF NOT EXISTS idx_user_country ON users(country_code);

