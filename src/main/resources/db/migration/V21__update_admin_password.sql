-- Update admin password hash
-- Version 21.0.0
-- Updates the admin user password to the new hash

-- Update admin user password
UPDATE users 
SET password = '$2a$10$CHXehC.xzBj5TQWq7oi1Qe3XYz3fh9QrK4c4PeHhN4kEY8g6PaBSG'
WHERE username = 'admin';

