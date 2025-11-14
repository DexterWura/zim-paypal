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
 * Service for fraud detection and risk assessment
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FraudDetectionService {

    private final FraudRuleRepository fraudRuleRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    /**
     * Analyze transaction for fraud
     * 
     * @param transaction Transaction to analyze
     * @return Risk score
     */
    public RiskScore analyzeTransaction(Transaction transaction) {
        BigDecimal riskScore = BigDecimal.ZERO;
        List<String> riskFactors = new ArrayList<>();

        // Get user
        User user = transaction.getSender();
        if (user == null) {
            user = transaction.getAccount() != null ? transaction.getAccount().getUser() : null;
        }

        if (user == null) {
            return createRiskScore(null, transaction, BigDecimal.ZERO, RiskScore.RiskLevel.LOW, 
                    RiskScore.ScoreType.TRANSACTION, "No user associated with transaction");
        }

        // Check amount threshold
        riskScore = riskScore.add(checkAmountThreshold(transaction, riskFactors));

        // Check velocity (rapid transactions)
        riskScore = riskScore.add(checkVelocity(user, transaction, riskFactors));

        // Check unusual patterns
        riskScore = riskScore.add(checkUnusualPatterns(user, transaction, riskFactors));

        // Check account age
        riskScore = riskScore.add(checkAccountAge(user, riskFactors));

        // Check transaction frequency
        riskScore = riskScore.add(checkTransactionFrequency(user, riskFactors));

        // Determine risk level
        RiskScore.RiskLevel riskLevel = determineRiskLevel(riskScore);

        // Create risk score record
        RiskScore score = createRiskScore(user, transaction, riskScore, riskLevel, 
                RiskScore.ScoreType.TRANSACTION, String.join(", ", riskFactors));

        // If high risk, create suspicious activity
        if (riskLevel == RiskScore.RiskLevel.HIGH || riskLevel == RiskScore.RiskLevel.CRITICAL) {
            createSuspiciousActivity(user, transaction, riskLevel, riskFactors);
        }

        return score;
    }

    /**
     * Check amount threshold rules
     */
    private BigDecimal checkAmountThreshold(Transaction transaction, List<String> riskFactors) {
        BigDecimal risk = BigDecimal.ZERO;
        List<FraudRule> rules = fraudRuleRepository.findByRuleTypeAndIsActiveTrue(FraudRule.RuleType.AMOUNT_THRESHOLD);

        for (FraudRule rule : rules) {
            if (rule.getThresholdAmount() != null && 
                transaction.getAmount().compareTo(rule.getThresholdAmount()) > 0) {
                risk = risk.add(new BigDecimal("15"));
                riskFactors.add("Amount exceeds threshold: " + rule.getRuleName());
                
                // Execute action
                executeRuleAction(rule, transaction);
            }
        }

        return risk;
    }

    /**
     * Check velocity (rapid transactions)
     */
    private BigDecimal checkVelocity(User user, Transaction transaction, List<String> riskFactors) {
        BigDecimal risk = BigDecimal.ZERO;
        List<FraudRule> rules = fraudRuleRepository.findByRuleTypeAndIsActiveTrue(FraudRule.RuleType.VELOCITY_CHECK);

        for (FraudRule rule : rules) {
            if (rule.getTimeWindowMinutes() != null && rule.getThresholdCount() != null) {
                LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rule.getTimeWindowMinutes());
                List<Transaction> recentTransactions = transactionRepository.findBySenderIdAndCreatedAtBetween(
                        user.getId(), windowStart, LocalDateTime.now());

                if (recentTransactions.size() >= rule.getThresholdCount()) {
                    risk = risk.add(new BigDecimal("20"));
                    riskFactors.add("High velocity: " + recentTransactions.size() + 
                            " transactions in " + rule.getTimeWindowMinutes() + " minutes");
                    
                    executeRuleAction(rule, transaction);
                }
            }
        }

        return risk;
    }

    /**
     * Check unusual patterns
     */
    private BigDecimal checkUnusualPatterns(User user, Transaction transaction, List<String> riskFactors) {
        BigDecimal risk = BigDecimal.ZERO;

        // Check for structuring (transactions just below reporting threshold)
        List<FraudRule> structuringRules = fraudRuleRepository.findByRuleTypeAndIsActiveTrue(
                FraudRule.RuleType.STRUCTURING_DETECTION);

        for (FraudRule rule : structuringRules) {
            if (rule.getThresholdAmount() != null) {
                BigDecimal threshold = rule.getThresholdAmount();
                BigDecimal ninetyPercent = threshold.multiply(new BigDecimal("0.90"));
                
                if (transaction.getAmount().compareTo(ninetyPercent) >= 0 && 
                    transaction.getAmount().compareTo(threshold) < 0) {
                    risk = risk.add(new BigDecimal("25"));
                    riskFactors.add("Possible structuring: amount just below threshold");
                    executeRuleAction(rule, transaction);
                }
            }
        }

        return risk;
    }

    /**
     * Check account age
     */
    private BigDecimal checkAccountAge(User user, List<String> riskFactors) {
        BigDecimal risk = BigDecimal.ZERO;
        
        List<Account> accounts = accountRepository.findByUser(user);
        if (!accounts.isEmpty()) {
            Account account = accounts.get(0);
            long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                    account.getCreatedAt(), LocalDateTime.now());
            
            if (daysSinceCreation < 7) {
                risk = risk.add(new BigDecimal("10"));
                riskFactors.add("New account: " + daysSinceCreation + " days old");
            }
        }

        return risk;
    }

    /**
     * Check transaction frequency
     */
    private BigDecimal checkTransactionFrequency(User user, List<String> riskFactors) {
        BigDecimal risk = BigDecimal.ZERO;
        
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<Transaction> recentTransactions = transactionRepository.findBySenderIdAndCreatedAtBetween(
                user.getId(), last24Hours, LocalDateTime.now());

        if (recentTransactions.size() > 50) {
            risk = risk.add(new BigDecimal("15"));
            riskFactors.add("High transaction frequency: " + recentTransactions.size() + " in 24 hours");
        }

        return risk;
    }

    /**
     * Determine risk level from score
     */
    private RiskScore.RiskLevel determineRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("70")) >= 0) {
            return RiskScore.RiskLevel.CRITICAL;
        } else if (score.compareTo(new BigDecimal("50")) >= 0) {
            return RiskScore.RiskLevel.HIGH;
        } else if (score.compareTo(new BigDecimal("30")) >= 0) {
            return RiskScore.RiskLevel.MEDIUM;
        } else {
            return RiskScore.RiskLevel.LOW;
        }
    }

    /**
     * Create risk score record
     */
    private RiskScore createRiskScore(User user, Transaction transaction, BigDecimal score, 
                                     RiskScore.RiskLevel level, RiskScore.ScoreType type, String factors) {
        RiskScore riskScore = RiskScore.builder()
                .user(user)
                .transaction(transaction)
                .riskScore(score)
                .riskLevel(level)
                .scoreType(type)
                .riskFactors(factors)
                .build();

        return riskScoreRepository.save(riskScore);
    }

    /**
     * Create suspicious activity record
     */
    private SuspiciousActivity createSuspiciousActivity(User user, Transaction transaction, 
                                                       RiskScore.RiskLevel riskLevel, 
                                                       List<String> riskFactors) {
        SuspiciousActivity.Severity severity = riskLevel == RiskScore.RiskLevel.CRITICAL ? 
                SuspiciousActivity.Severity.CRITICAL : SuspiciousActivity.Severity.HIGH;

        SuspiciousActivity activity = SuspiciousActivity.builder()
                .user(user)
                .transaction(transaction)
                .activityType(SuspiciousActivity.ActivityType.FRAUDULENT_ACTIVITY)
                .severity(severity)
                .status(SuspiciousActivity.Status.PENDING)
                .description("Fraud detected: " + String.join(", ", riskFactors))
                .riskFactors(String.join(", ", riskFactors))
                .autoDetected(true)
                .build();

        SuspiciousActivity saved = suspiciousActivityRepository.save(activity);
        log.warn("Suspicious activity detected for user: {} - Transaction: {}", 
                user.getUsername(), transaction.getTransactionNumber());
        return saved;
    }

    /**
     * Execute rule action
     */
    private void executeRuleAction(FraudRule rule, Transaction transaction) {
        switch (rule.getActionType()) {
            case FLAG:
                // Already flagged by creating suspicious activity
                break;
            case BLOCK:
                transaction.markAsFailed("Transaction blocked by fraud rule: " + rule.getRuleName());
                break;
            case FREEZE_ACCOUNT:
                if (transaction.getAccount() != null) {
                    transaction.getAccount().setStatus(Account.AccountStatus.SUSPENDED);
                }
                break;
            case REQUIRE_VERIFICATION:
                // Mark for KYC verification
                break;
            default:
                break;
        }
    }

    /**
     * Calculate user risk score
     * 
     * @param user User entity
     * @return Risk score
     */
    public RiskScore calculateUserRiskScore(User user) {
        BigDecimal riskScore = BigDecimal.ZERO;
        List<String> riskFactors = new ArrayList<>();

        // Check recent suspicious activities
        long suspiciousCount = suspiciousActivityRepository.countByStatus(SuspiciousActivity.Status.PENDING);
        if (suspiciousCount > 0) {
            riskScore = riskScore.add(new BigDecimal("20"));
            riskFactors.add("Pending suspicious activities: " + suspiciousCount);
        }

        // Check transaction history
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        List<Transaction> recentTransactions = transactionRepository.findBySenderIdAndCreatedAtBetween(
                user.getId(), last30Days, LocalDateTime.now());

        if (recentTransactions.size() > 200) {
            riskScore = riskScore.add(new BigDecimal("15"));
            riskFactors.add("High transaction volume: " + recentTransactions.size() + " in 30 days");
        }

        RiskScore.RiskLevel riskLevel = determineRiskLevel(riskScore);
        return createRiskScore(user, null, riskScore, riskLevel, 
                RiskScore.ScoreType.USER_PROFILE, String.join(", ", riskFactors));
    }
}

