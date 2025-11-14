package com.zim.paypal.service;

import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for Anti-Money Laundering (AML) compliance
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AmlService {

    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final TransactionRepository transactionRepository;
    private final KycVerificationRepository kycVerificationRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    // AML reporting thresholds (configurable)
    private static final BigDecimal CTR_THRESHOLD = new BigDecimal("10000.00"); // Currency Transaction Report
    private static final BigDecimal SAR_THRESHOLD = new BigDecimal("5000.00"); // Suspicious Activity Report

    /**
     * Perform AML check on transaction
     * 
     * @param transaction Transaction to check
     * @return true if transaction passes AML checks
     */
    public boolean performAmlCheck(Transaction transaction) {
        User user = transaction.getSender();
        if (user == null) {
            user = transaction.getAccount() != null ? transaction.getAccount().getUser() : null;
        }

        if (user == null) {
            return false;
        }

        // Check KYC status
        if (!isKycVerified(user)) {
            createSuspiciousActivity(user, transaction, 
                    SuspiciousActivity.ActivityType.MONEY_LAUNDERING,
                    "Transaction attempted without KYC verification");
            return false;
        }

        // Check CTR threshold
        if (transaction.getAmount().compareTo(CTR_THRESHOLD) >= 0) {
            logAmlEvent(user, transaction, "CTR_THRESHOLD_EXCEEDED", 
                    "Transaction amount exceeds CTR threshold: " + transaction.getAmount());
        }

        // Check for structuring
        if (isStructuring(user, transaction)) {
            createSuspiciousActivity(user, transaction,
                    SuspiciousActivity.ActivityType.STRUCTURING,
                    "Possible structuring detected");
            return false;
        }

        // Check for unusual transaction patterns
        if (isUnusualPattern(user, transaction)) {
            createSuspiciousActivity(user, transaction,
                    SuspiciousActivity.ActivityType.UNUSUAL_PATTERN,
                    "Unusual transaction pattern detected");
        }

        return true;
    }

    /**
     * Check if user is KYC verified
     */
    private boolean isKycVerified(User user) {
        return kycVerificationRepository.findLatestByUser(user)
                .map(KycVerification::isValid)
                .orElse(false);
    }

    /**
     * Check for structuring (breaking large transactions into smaller ones)
     */
    private boolean isStructuring(User user, Transaction transaction) {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<Transaction> recentTransactions = transactionRepository.findBySenderIdAndCreatedAtBetween(
                user.getId(), last24Hours, LocalDateTime.now());

        BigDecimal totalAmount = recentTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // If multiple transactions totaling near threshold
        if (recentTransactions.size() >= 3 && 
            totalAmount.add(transaction.getAmount()).compareTo(CTR_THRESHOLD) >= 0 &&
            transaction.getAmount().compareTo(CTR_THRESHOLD) < 0) {
            return true;
        }

        return false;
    }

    /**
     * Check for unusual transaction patterns
     */
    private boolean isUnusualPattern(User user, Transaction transaction) {
        // Check for rapid cross-border transactions
        // Check for transactions at unusual hours
        LocalDateTime transactionTime = transaction.getCreatedAt();
        int hour = transactionTime.getHour();
        
        // Transactions between 2 AM and 5 AM are unusual
        if (hour >= 2 && hour < 5) {
            return true;
        }

        // Check for round number transactions (potential structuring)
        BigDecimal amount = transaction.getAmount();
        if (amount.remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) == 0 &&
            amount.compareTo(new BigDecimal("1000")) >= 0) {
            // Multiple round number transactions might indicate structuring
            LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
            List<Transaction> recentRoundTransactions = transactionRepository.findBySenderIdAndCreatedAtBetween(
                    user.getId(), last7Days, LocalDateTime.now())
                    .stream()
                    .filter(t -> t.getAmount().remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) == 0)
                    .toList();
            
            if (recentRoundTransactions.size() >= 5) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create suspicious activity
     */
    private SuspiciousActivity createSuspiciousActivity(User user, Transaction transaction,
                                                       SuspiciousActivity.ActivityType activityType,
                                                       String description) {
        SuspiciousActivity activity = SuspiciousActivity.builder()
                .user(user)
                .transaction(transaction)
                .activityType(activityType)
                .severity(SuspiciousActivity.Severity.HIGH)
                .status(SuspiciousActivity.Status.PENDING)
                .description(description)
                .autoDetected(true)
                .build();

        SuspiciousActivity saved = suspiciousActivityRepository.save(activity);
        log.warn("AML suspicious activity created: {} for user: {}", activityType, user.getUsername());
        return saved;
    }

    /**
     * Log AML event
     */
    private void logAmlEvent(User user, Transaction transaction, String eventType, String description) {
        log.info("AML Event - Type: {}, User: {}, Transaction: {}, Description: {}", 
                eventType, user.getUsername(), transaction.getTransactionNumber(), description);
        
        // In production, this would be sent to AML monitoring system
    }

    /**
     * Generate AML report for user
     * 
     * @param userId User ID
     * @return AML report data
     */
    @Transactional(readOnly = true)
    public AmlReport generateAmlReport(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        List<Transaction> transactions = transactionRepository.findBySenderIdAndCreatedAtBetween(
                userId, last30Days, LocalDateTime.now());

        BigDecimal totalVolume = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long suspiciousCount = suspiciousActivityRepository.findByUserOrderByCreatedAtDesc(user, 
                org.springframework.data.domain.PageRequest.of(0, 100))
                .getTotalElements();

        return new AmlReport(user, transactions.size(), totalVolume, suspiciousCount);
    }

    /**
     * AML Report data class
     */
    public record AmlReport(User user, int transactionCount, BigDecimal totalVolume, long suspiciousActivityCount) {}
}

