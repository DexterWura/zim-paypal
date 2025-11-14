package com.zim.paypal.service;

import com.zim.paypal.model.entity.Account;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for account management operations
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    /**
     * Create default account for user
     * 
     * @param user User entity
     * @return Created account
     */
    public Account createDefaultAccount(User user) {
        log.info("Creating default account for user: {}", user.getUsername());
        
        String accountNumber = generateAccountNumber();
        
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .user(user)
                .balance(BigDecimal.ZERO)
                .currencyCode("USD")
                .accountType(Account.AccountType.PERSONAL)
                .status(Account.AccountStatus.ACTIVE)
                .build();
        
        Account savedAccount = accountRepository.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());
        return savedAccount;
    }

    /**
     * Find account by account number
     * 
     * @param accountNumber Account number
     * @return Account entity
     * @throws IllegalArgumentException if account not found
     */
    @Transactional(readOnly = true)
    public Account findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
    }

    /**
     * Find active account by user
     * 
     * @param user User entity
     * @return Account entity
     * @throws IllegalArgumentException if account not found
     */
    @Transactional(readOnly = true)
    public Account findActiveAccountByUser(User user) {
        return accountRepository.findActiveAccountByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No active account found for user: " + user.getUsername()));
    }

    /**
     * Find all accounts by user
     * 
     * @param user User entity
     * @return List of accounts
     */
    @Transactional(readOnly = true)
    public List<Account> findByUser(User user) {
        return accountRepository.findByUser(user);
    }

    /**
     * Deposit money into account
     * 
     * @param accountId Account ID
     * @param amount Amount to deposit
     * @return Updated account
     * @throws IllegalArgumentException if amount is invalid
     */
    public Account deposit(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        if (!account.isActive()) {
            throw new IllegalStateException("Account is not active");
        }
        
        account.deposit(amount);
        Account savedAccount = accountRepository.save(account);
        log.info("Deposited {} {} to account: {}", amount, account.getCurrencyCode(), account.getAccountNumber());
        return savedAccount;
    }

    /**
     * Withdraw money from account
     * 
     * @param accountId Account ID
     * @param amount Amount to withdraw
     * @return Updated account
     * @throws IllegalArgumentException if amount is invalid or insufficient balance
     */
    public Account withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        if (!account.isActive()) {
            throw new IllegalStateException("Account is not active");
        }
        
        account.withdraw(amount);
        Account savedAccount = accountRepository.save(account);
        log.info("Withdrew {} {} from account: {}", amount, account.getCurrencyCode(), account.getAccountNumber());
        return savedAccount;
    }

    /**
     * Find account by ID
     * 
     * @param accountId Account ID
     * @return Account entity
     * @throws IllegalArgumentException if account not found
     */
    @Transactional(readOnly = true)
    public Account findById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

    /**
     * Get account balance
     * 
     * @param accountId Account ID
     * @return Account balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
        Account account = findById(accountId);
        return account.getBalance();
    }

    /**
     * Generate unique account number
     * 
     * @return Account number
     */
    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "ZIM" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}

