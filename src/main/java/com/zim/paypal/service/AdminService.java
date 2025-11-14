package com.zim.paypal.service;

import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.AccountRepository;
import com.zim.paypal.repository.CardRepository;
import com.zim.paypal.repository.MoneyRequestRepository;
import com.zim.paypal.repository.RewardsRepository;
import com.zim.paypal.repository.TransactionRepository;
import com.zim.paypal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for admin operations
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final MoneyRequestRepository moneyRequestRepository;
    private final RewardsRepository rewardsRepository;

    /**
     * Get dashboard statistics
     * 
     * @return Map of statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .count();
        long lockedUsers = userRepository.findAll().stream()
                .filter(u -> u.getAccountLocked())
                .count();
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("lockedUsers", lockedUsers);
        stats.put("inactiveUsers", totalUsers - activeUsers);
        
        // Account statistics
        long totalAccounts = accountRepository.count();
        BigDecimal totalBalance = accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("totalAccounts", totalAccounts);
        stats.put("totalBalance", totalBalance);
        
        // Transaction statistics
        long totalTransactions = transactionRepository.count();
        long completedTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .count();
        BigDecimal totalTransactionVolume = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("totalTransactions", totalTransactions);
        stats.put("completedTransactions", completedTransactions);
        stats.put("totalTransactionVolume", totalTransactionVolume);
        
        // Card statistics
        long totalCards = cardRepository.count();
        long activeCards = cardRepository.findAll().stream()
                .filter(c -> c.getStatus() == Card.CardStatus.ACTIVE)
                .count();
        
        stats.put("totalCards", totalCards);
        stats.put("activeCards", activeCards);
        
        // Money request statistics
        long totalMoneyRequests = moneyRequestRepository.count();
        long pendingRequests = moneyRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == MoneyRequest.RequestStatus.PENDING)
                .count();
        
        stats.put("totalMoneyRequests", totalMoneyRequests);
        stats.put("pendingRequests", pendingRequests);
        
        // Recent activity (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long recentUsers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt().isAfter(yesterday))
                .count();
        long recentTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(yesterday))
                .count();
        
        stats.put("recentUsers", recentUsers);
        stats.put("recentTransactions", recentTransactions);
        
        return stats;
    }

    /**
     * Get all users with pagination
     * 
     * @param pageable Pageable object
     * @return Page of users
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return User entity
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    /**
     * Update user role
     * 
     * @param userId User ID
     * @param role New role
     * @return Updated user
     */
    public User updateUserRole(Long userId, User.UserRole role) {
        User user = getUserById(userId);
        user.setRole(role);
        User savedUser = userRepository.save(user);
        log.info("Updated user {} role to {}", user.getUsername(), role);
        return savedUser;
    }

    /**
     * Enable/disable user account
     * 
     * @param userId User ID
     * @param enabled Enabled status
     * @return Updated user
     */
    public User setUserEnabled(Long userId, Boolean enabled) {
        User user = getUserById(userId);
        user.setAccountEnabled(enabled);
        User savedUser = userRepository.save(user);
        log.info("Set user {} enabled status to {}", user.getUsername(), enabled);
        return savedUser;
    }

    /**
     * Lock/unlock user account
     * 
     * @param userId User ID
     * @param locked Locked status
     * @return Updated user
     */
    public User setUserLocked(Long userId, Boolean locked) {
        User user = getUserById(userId);
        user.setAccountLocked(locked);
        if (!locked) {
            user.resetFailedLoginAttempts();
        }
        User savedUser = userRepository.save(user);
        log.info("Set user {} locked status to {}", user.getUsername(), locked);
        return savedUser;
    }

    /**
     * Get all transactions with pagination
     * 
     * @param pageable Pageable object
     * @return Page of transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    /**
     * Get all accounts
     * 
     * @return List of accounts
     */
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Search users by username or email
     * 
     * @param query Search query
     * @param pageable Pageable object
     * @return Page of users
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, Pageable pageable) {
        // Simple search - in production, use proper search functionality
        return userRepository.findAll(pageable);
    }
}

