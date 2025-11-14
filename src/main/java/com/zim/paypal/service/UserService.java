package com.zim.paypal.service;

import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for user management operations
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    /**
     * Register a new user
     * 
     * @param user User entity to register
     * @return Registered user
     * @throws IllegalArgumentException if username or email already exists
     */
    public User registerUser(User user) {
        log.info("Registering new user: {}", user.getUsername());
        
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Create default account for user
        accountService.createDefaultAccount(savedUser);
        
        log.info("User registered successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Find user by username
     * 
     * @param username Username
     * @return User entity
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Find user by email
     * 
     * @param email Email address
     * @return User entity
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    /**
     * Find user by ID
     * 
     * @param id User ID
     * @return User entity
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    /**
     * Verify email address
     * 
     * @param userId User ID
     */
    public void verifyEmail(Long userId) {
        User user = findById(userId);
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified for user: {}", user.getUsername());
    }

    /**
     * Verify phone number
     * 
     * @param userId User ID
     */
    public void verifyPhone(Long userId) {
        User user = findById(userId);
        user.setPhoneVerified(true);
        userRepository.save(user);
        log.info("Phone verified for user: {}", user.getUsername());
    }

    /**
     * Update user profile
     * 
     * @param userId User ID
     * @param firstName First name
     * @param lastName Last name
     * @param phoneNumber Phone number
     * @return Updated user
     */
    public User updateProfile(Long userId, String firstName, String lastName, String phoneNumber) {
        User user = findById(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            user.setPhoneNumber(phoneNumber);
        }
        return userRepository.save(user);
    }

    /**
     * Change password
     * 
     * @param userId User ID
     * @param oldPassword Old password
     * @param newPassword New password
     * @throws IllegalArgumentException if old password is incorrect
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }
}

