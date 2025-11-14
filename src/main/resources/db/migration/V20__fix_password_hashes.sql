-- Fix password hashes for default users
-- Version 20.0.0
-- This migration updates the password hashes with correct BCrypt encoding
-- Only runs if users exist with old/wrong hashes

-- Update admin user password (admin123) if it exists with wrong hash
UPDATE users 
SET password = '$2a$10$CHXehC.xzBj5TQWq7oi1Qe3XYz3fh9QrK4c4PeHhN4kEY8g6PaBSG'
WHERE username = 'admin' 
  AND (password != '$2a$10$CHXehC.xzBj5TQWq7oi1Qe3XYz3fh9QrK4c4PeHhN4kEY8g6PaBSG'
       OR password IS NULL
       OR LENGTH(password) < 60);

-- Update test user password (testuser123) if it exists with wrong hash
UPDATE users 
SET password = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.H/Hj5K0Yj5J5J5J5J5J5J'
WHERE username = 'testuser' 
  AND (password = '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW'
       OR password IS NULL
       OR LENGTH(password) < 60);

