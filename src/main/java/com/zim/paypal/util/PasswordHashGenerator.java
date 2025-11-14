package com.zim.paypal.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for database migrations
 * Run this main method to generate correct hashes
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("=== BCrypt Password Hashes ===");
        System.out.println();
        System.out.println("Password: admin123");
        System.out.println("Hash: " + encoder.encode("admin123"));
        System.out.println();
        System.out.println("Password: testuser123");
        System.out.println("Hash: " + encoder.encode("testuser123"));
        System.out.println();
    }
}

