package com.zim.paypal.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes for database migrations
 * 
 * @author dexterwura
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String adminPassword = "admin123";
        String testUserPassword = "testuser123";
        
        System.out.println("Admin password hash (admin123):");
        System.out.println(encoder.encode(adminPassword));
        System.out.println();
        System.out.println("Test user password hash (testuser123):");
        System.out.println(encoder.encode(testUserPassword));
    }
}

