package com.zim.paypal.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to generate and verify BCrypt password hashes
 */
public class PasswordHashTest {
    
    @Test
    public void generateAndVerifyHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hashes
        String adminHash = encoder.encode("admin123");
        String testuserHash = encoder.encode("testuser123");
        
        System.out.println("=== BCrypt Password Hashes ===");
        System.out.println("Password: admin123");
        System.out.println("Hash: " + adminHash);
        System.out.println();
        System.out.println("Password: testuser123");
        System.out.println("Hash: " + testuserHash);
        System.out.println();
        
        // Verify they work
        assertTrue(encoder.matches("admin123", adminHash));
        assertTrue(encoder.matches("testuser123", testuserHash));
        
        // Test the hashes we're using in migration
        String migrationAdminHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String migrationTestuserHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.H/Hj5K0Yj5J5J5J5J5J5J";
        
        System.out.println("=== Verifying Migration Hashes ===");
        boolean adminValid = encoder.matches("admin123", migrationAdminHash);
        boolean testuserValid = encoder.matches("testuser123", migrationTestuserHash);
        
        System.out.println("Admin hash valid: " + adminValid);
        System.out.println("Testuser hash valid: " + testuserValid);
        
        assertTrue(adminValid, "Admin hash should be valid");
        assertTrue(testuserValid, "Testuser hash should be valid");
    }
}

