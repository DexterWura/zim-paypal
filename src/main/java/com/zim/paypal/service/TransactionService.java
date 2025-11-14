package com.zim.paypal.service;

import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for transaction management operations
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final UserService userService;
    private final CardService cardService;
    private final NotificationService notificationService;
    private final RewardsService rewardsService;
    private final AccountLimitService accountLimitService;
    private final FraudDetectionService fraudDetectionService;
    private final AmlService amlService;
    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.029"); // 2.9%
    private static final BigDecimal MIN_TRANSFER_FEE = new BigDecimal("0.30");
    private static final BigDecimal MAX_TRANSFER_FEE = new BigDecimal("2.99");

    /**
     * Create a deposit transaction
     * 
     * @param userId User ID
     * @param amount Amount to deposit
     * @param description Description
     * @return Created transaction
     */
    public Transaction createDeposit(Long userId, BigDecimal amount, String description) {
        User user = userService.findById(userId);
        Account account = accountService.findActiveAccountByUser(user);
        
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .account(account)
                .amount(amount)
                .currencyCode(account.getCurrencyCode())
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .paymentMethod(Transaction.PaymentMethod.BANK_TRANSFER)
                .status(Transaction.TransactionStatus.PENDING)
                .description(description)
                .fee(BigDecimal.ZERO)
                .build();
        
        transaction.calculateNetAmount();
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Perform fraud and AML checks
        try {
            fraudDetectionService.analyzeTransaction(savedTransaction);
            if (!amlService.performAmlCheck(savedTransaction)) {
                savedTransaction.markAsFailed("Transaction failed AML compliance check");
                transactionRepository.save(savedTransaction);
                throw new IllegalStateException("Transaction failed AML compliance check");
            }
        } catch (Exception e) {
            log.warn("Fraud/AML check failed: {}", e.getMessage());
            // Continue with transaction but flag it
        }
        
        // Process deposit
        accountService.deposit(account.getId(), amount);
        savedTransaction.markAsCompleted();
        transactionRepository.save(savedTransaction);
        
        // Send notification
        notificationService.sendTransactionNotification(savedTransaction);
        
        // Award rewards points
        try {
            rewardsService.earnPointsFromTransaction(user.getId(), savedTransaction);
        } catch (Exception e) {
            log.warn("Failed to award rewards points: {}", e.getMessage());
        }
        
        log.info("Deposit transaction created: {}", savedTransaction.getTransactionNumber());
        return savedTransaction;
    }

    /**
     * Create a transfer transaction (send money)
     * 
     * @param senderId Sender user ID
     * @param receiverEmail Receiver email
     * @param amount Amount to transfer
     * @param description Description
     * @return Created transaction
     */
    public Transaction createTransfer(Long senderId, String receiverEmail, BigDecimal amount, String description) {
        User sender = userService.findById(senderId);
        User receiver = userService.findByEmail(receiverEmail);
        
        Account senderAccount = accountService.findActiveAccountByUser(sender);
        Account receiverAccount = accountService.findActiveAccountByUser(receiver);
        
        // Check transaction limits
        if (!accountLimitService.isTransactionAmountAllowed(senderId, sender.getRole(), amount)) {
            throw new IllegalStateException("Transaction amount exceeds allowed limits");
        }
        
        // Calculate fee
        BigDecimal fee = calculateTransferFee(amount);
        BigDecimal totalAmount = amount.add(fee);
        
        // Check sufficient balance
        if (!senderAccount.hasSufficientBalance(totalAmount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .sender(sender)
                .receiver(receiver)
                .account(senderAccount)
                .amount(amount)
                .currencyCode(senderAccount.getCurrencyCode())
                .transactionType(Transaction.TransactionType.TRANSFER)
                .paymentMethod(Transaction.PaymentMethod.WALLET)
                .status(Transaction.TransactionStatus.PENDING)
                .description(description)
                .fee(fee)
                .build();
        
        transaction.calculateNetAmount();
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Perform fraud and AML checks
        try {
            RiskScore riskScore = fraudDetectionService.analyzeTransaction(savedTransaction);
            if (!amlService.performAmlCheck(savedTransaction)) {
                savedTransaction.markAsFailed("Transaction failed AML compliance check");
                transactionRepository.save(savedTransaction);
                throw new IllegalStateException("Transaction failed AML compliance check");
            }
            
            // If critical risk, block transaction
            if (riskScore.getRiskLevel() == RiskScore.RiskLevel.CRITICAL) {
                savedTransaction.markAsFailed("Transaction blocked due to high fraud risk");
                transactionRepository.save(savedTransaction);
                throw new IllegalStateException("Transaction blocked due to high fraud risk");
            }
        } catch (Exception e) {
            log.warn("Fraud/AML check failed: {}", e.getMessage());
            if (e instanceof IllegalStateException) {
                throw e;
            }
        }
        
        // Process transfer
        accountService.withdraw(senderAccount.getId(), totalAmount);
        accountService.deposit(receiverAccount.getId(), amount);
        
        savedTransaction.markAsCompleted();
        transactionRepository.save(savedTransaction);
        
        // Send notifications
        notificationService.sendTransactionNotification(savedTransaction);
        
        // Award rewards points to sender
        try {
            rewardsService.earnPointsFromTransaction(sender.getId(), savedTransaction);
        } catch (Exception e) {
            log.warn("Failed to award rewards points: {}", e.getMessage());
        }
        
        log.info("Transfer transaction created: {}", savedTransaction.getTransactionNumber());
        return savedTransaction;
    }

    /**
     * Create a payment transaction using wallet
     * 
     * @param userId User ID
     * @param amount Amount to pay
     * @param description Description
     * @param merchantId Merchant ID (optional)
     * @return Created transaction
     */
    public Transaction createPaymentFromWallet(Long userId, BigDecimal amount, String description, Long merchantId) {
        User user = userService.findById(userId);
        Account account = accountService.findActiveAccountByUser(user);
        
        // Check transaction limits
        if (!accountLimitService.isTransactionAmountAllowed(userId, user.getRole(), amount)) {
            throw new IllegalStateException("Transaction amount exceeds allowed limits");
        }
        
        // Calculate fee
        BigDecimal fee = calculatePaymentFee(amount);
        BigDecimal totalAmount = amount.add(fee);
        
        if (!account.hasSufficientBalance(totalAmount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .sender(user)
                .account(account)
                .amount(amount)
                .currencyCode(account.getCurrencyCode())
                .transactionType(Transaction.TransactionType.PAYMENT)
                .paymentMethod(Transaction.PaymentMethod.WALLET)
                .status(Transaction.TransactionStatus.PENDING)
                .description(description)
                .fee(fee)
                .build();
        
        transaction.calculateNetAmount();
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Process payment
        accountService.withdraw(account.getId(), totalAmount);
        savedTransaction.markAsCompleted();
        transactionRepository.save(savedTransaction);
        
        // Send notification
        notificationService.sendTransactionNotification(savedTransaction);
        
        // Award rewards points
        try {
            rewardsService.earnPointsFromTransaction(userId, savedTransaction);
        } catch (Exception e) {
            log.warn("Failed to award rewards points: {}", e.getMessage());
        }
        
        log.info("Payment transaction created: {}", savedTransaction.getTransactionNumber());
        return savedTransaction;
    }

    /**
     * Create a payment transaction using card
     * 
     * @param userId User ID
     * @param cardId Card ID
     * @param amount Amount to pay
     * @param description Description
     * @return Created transaction
     */
    public Transaction createPaymentFromCard(Long userId, Long cardId, BigDecimal amount, String description) {
        User user = userService.findById(userId);
        Card card = cardService.findById(cardId);
        
        if (!card.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Card does not belong to user");
        }
        
        if (!card.isValid()) {
            throw new IllegalStateException("Card is not valid");
        }
        
        // Calculate fee
        BigDecimal fee = calculatePaymentFee(amount);
        BigDecimal totalAmount = amount.add(fee);
        
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .sender(user)
                .card(card)
                .amount(amount)
                .currencyCode("USD")
                .transactionType(Transaction.TransactionType.PAYMENT)
                .paymentMethod(Transaction.PaymentMethod.CARD)
                .status(Transaction.TransactionStatus.PENDING)
                .description(description)
                .fee(fee)
                .build();
        
        transaction.calculateNetAmount();
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // In a real system, this would process the card payment through a payment gateway
        // For now, we'll just mark it as completed
        savedTransaction.markAsCompleted();
        transactionRepository.save(savedTransaction);
        
        // Send notification
        notificationService.sendTransactionNotification(savedTransaction);
        
        // Award rewards points
        try {
            rewardsService.earnPointsFromTransaction(userId, savedTransaction);
        } catch (Exception e) {
            log.warn("Failed to award rewards points: {}", e.getMessage());
        }
        
        log.info("Card payment transaction created: {}", savedTransaction.getTransactionNumber());
        return savedTransaction;
    }

    /**
     * Find transaction by transaction number
     * 
     * @param transactionNumber Transaction number
     * @return Transaction entity
     */
    @Transactional(readOnly = true)
    public Transaction findByTransactionNumber(String transactionNumber) {
        return transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionNumber));
    }

    /**
     * Get transactions for user
     * 
     * @param userId User ID
     * @param pageable Pageable object
     * @return Page of transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByUser(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return transactionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get transactions for account
     * 
     * @param accountId Account ID
     * @param pageable Pageable object
     * @return Page of transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByAccount(Long accountId, Pageable pageable) {
        Account account = accountService.findById(accountId);
        return transactionRepository.findByAccountOrderByCreatedAtDesc(account, pageable);
    }

    /**
     * Calculate transfer fee
     * 
     * @param amount Transfer amount
     * @return Fee amount
     */
    private BigDecimal calculateTransferFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(TRANSFER_FEE_RATE);
        if (fee.compareTo(MIN_TRANSFER_FEE) < 0) {
            return MIN_TRANSFER_FEE;
        }
        if (fee.compareTo(MAX_TRANSFER_FEE) > 0) {
            return MAX_TRANSFER_FEE;
        }
        return fee;
    }

    /**
     * Calculate payment fee
     * 
     * @param amount Payment amount
     * @return Fee amount
     */
    private BigDecimal calculatePaymentFee(BigDecimal amount) {
        return calculateTransferFee(amount);
    }

    /**
     * Generate unique transaction number
     * 
     * @return Transaction number
     */
    private String generateTransactionNumber() {
        String transactionNumber;
        do {
            transactionNumber = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();
        } while (transactionRepository.findByTransactionNumber(transactionNumber).isPresent());
        return transactionNumber;
    }
}

