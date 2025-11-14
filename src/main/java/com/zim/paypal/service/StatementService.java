package com.zim.paypal.service;

import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.StatementRepository;
import com.zim.paypal.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for statement generation and management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatementService {

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final UserService userService;

    /**
     * Generate monthly statement
     * 
     * @param userId User ID
     * @param year Year
     * @param month Month (1-12)
     * @return Generated statement
     */
    public Statement generateMonthlyStatement(Long userId, int year, int month) {
        User user = userService.findById(userId);
        Account account = accountService.findActiveAccountByUser(user);
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // Check if statement already exists
        statementRepository.findByAccountAndDateRange(account, startDate, endDate)
                .ifPresent(statement -> {
                    throw new IllegalArgumentException("Statement already exists for this period");
                });
        
        // Get transactions for the period
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Transaction> transactions = transactionRepository.findByAccountAndDateRange(
                account, startDateTime, endDateTime);
        
        // Calculate opening balance (balance at start of period)
        BigDecimal openingBalance = account.getBalance();
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        
        // Calculate totals from transactions
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionType() == Transaction.TransactionType.DEPOSIT ||
                transaction.getTransactionType() == Transaction.TransactionType.TRANSFER && 
                transaction.getReceiver() != null && transaction.getReceiver().getId().equals(userId)) {
                totalCredits = totalCredits.add(transaction.getAmount());
            } else {
                totalDebits = totalDebits.add(transaction.getAmount());
            }
        }
        
        // Calculate closing balance
        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);
        
        Statement statement = Statement.builder()
                .statementNumber(generateStatementNumber())
                .user(user)
                .account(account)
                .startDate(startDate)
                .endDate(endDate)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .transactionCount(transactions.size())
                .currencyCode(account.getCurrencyCode())
                .statementType(Statement.StatementType.MONTHLY)
                .generated(true)
                .build();
        
        Statement savedStatement = statementRepository.save(statement);
        log.info("Monthly statement generated: {}", savedStatement.getStatementNumber());
        return savedStatement;
    }

    /**
     * Generate custom statement
     * 
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @return Generated statement
     */
    public Statement generateCustomStatement(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userService.findById(userId);
        Account account = accountService.findActiveAccountByUser(user);
        
        // Get transactions for the period
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Transaction> transactions = transactionRepository.findByAccountAndDateRange(
                account, startDateTime, endDateTime);
        
        // Calculate balances and totals
        BigDecimal openingBalance = account.getBalance();
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionType() == Transaction.TransactionType.DEPOSIT ||
                transaction.getTransactionType() == Transaction.TransactionType.TRANSFER && 
                transaction.getReceiver() != null && transaction.getReceiver().getId().equals(userId)) {
                totalCredits = totalCredits.add(transaction.getAmount());
            } else {
                totalDebits = totalDebits.add(transaction.getAmount());
            }
        }
        
        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);
        
        Statement statement = Statement.builder()
                .statementNumber(generateStatementNumber())
                .user(user)
                .account(account)
                .startDate(startDate)
                .endDate(endDate)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .transactionCount(transactions.size())
                .currencyCode(account.getCurrencyCode())
                .statementType(Statement.StatementType.CUSTOM)
                .generated(true)
                .build();
        
        Statement savedStatement = statementRepository.save(statement);
        log.info("Custom statement generated: {}", savedStatement.getStatementNumber());
        return savedStatement;
    }

    /**
     * Find statement by statement number
     * 
     * @param statementNumber Statement number
     * @return Statement entity
     */
    @Transactional(readOnly = true)
    public Statement findByStatementNumber(String statementNumber) {
        return statementRepository.findByStatementNumber(statementNumber)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found: " + statementNumber));
    }

    /**
     * Get all statements for user
     * 
     * @param userId User ID
     * @return List of statements
     */
    @Transactional(readOnly = true)
    public List<Statement> getStatementsByUser(Long userId) {
        User user = userService.findById(userId);
        return statementRepository.findByUserOrderByEndDateDesc(user);
    }

    /**
     * Generate unique statement number
     * 
     * @return Statement number
     */
    private String generateStatementNumber() {
        String statementNumber;
        do {
            statementNumber = "STMT" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();
        } while (statementRepository.findByStatementNumber(statementNumber).isPresent());
        return statementNumber;
    }
}

