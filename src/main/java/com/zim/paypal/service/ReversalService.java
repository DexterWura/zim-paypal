package com.zim.paypal.service;

import com.zim.paypal.model.dto.ReversalRequestDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.AccountRepository;
import com.zim.paypal.repository.TransactionReversalRepository;
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
 * Service for transaction reversal management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReversalService {

    private final TransactionReversalRepository reversalRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    /**
     * Request a transaction reversal
     * 
     * @param userId User ID requesting reversal
     * @param reversalDto Reversal request DTO
     * @return Created reversal request
     */
    public TransactionReversal requestReversal(Long userId, ReversalRequestDto reversalDto) {
        User user = userService.findById(userId);
        Transaction transaction = transactionRepository.findById(reversalDto.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + reversalDto.getTransactionId()));

        // Validate user can request reversal for this transaction
        if ((transaction.getSender() != null && !transaction.getSender().getId().equals(userId)) &&
            (transaction.getReceiver() != null && !transaction.getReceiver().getId().equals(userId)) &&
            (transaction.getAccount() != null && !transaction.getAccount().getUser().getId().equals(userId))) {
            throw new IllegalArgumentException("You are not authorized to reverse this transaction");
        }

        // Check if transaction is reversible
        if (!isTransactionReversible(transaction)) {
            throw new IllegalStateException("This transaction cannot be reversed");
        }

        // Check if reversal already exists
        if (reversalRepository.findByTransaction(transaction).isPresent()) {
            throw new IllegalStateException("A reversal request already exists for this transaction");
        }

        // Validate reversal amount
        validateReversalAmount(transaction, reversalDto.getReversalType(), reversalDto.getReversalAmount());

        String reversalNumber = generateReversalNumber();

        TransactionReversal reversal = TransactionReversal.builder()
                .reversalNumber(reversalNumber)
                .transaction(transaction)
                .requestedBy(user)
                .reversalAmount(reversalDto.getReversalAmount())
                .reversalType(reversalDto.getReversalType())
                .status(TransactionReversal.ReversalStatus.PENDING)
                .reason(reversalDto.getReason())
                .build();

        TransactionReversal savedReversal = reversalRepository.save(reversal);

        // Send notification to admins
        log.info("Reversal request created: {} for transaction: {}", reversalNumber, transaction.getId());
        
        return savedReversal;
    }

    /**
     * Approve a reversal request (admin/support)
     * 
     * @param reversalId Reversal ID
     * @param adminId Admin user ID
     * @param notes Admin notes
     */
    public void approveReversal(Long reversalId, Long adminId, String notes) {
        TransactionReversal reversal = getReversalById(reversalId);
        User admin = userService.findById(adminId);

        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can approve reversals");
        }

        if (!reversal.isPending()) {
            throw new IllegalStateException("Only pending reversals can be approved");
        }

        reversal.markAsApproved(admin, notes);
        reversalRepository.save(reversal);

        log.info("Reversal approved: {} by admin: {}", reversal.getReversalNumber(), admin.getUsername());
    }

    /**
     * Reject a reversal request (admin/support)
     * 
     * @param reversalId Reversal ID
     * @param adminId Admin user ID
     * @param notes Rejection reason
     */
    public void rejectReversal(Long reversalId, Long adminId, String notes) {
        TransactionReversal reversal = getReversalById(reversalId);
        User admin = userService.findById(adminId);

        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can reject reversals");
        }

        if (!reversal.isPending()) {
            throw new IllegalStateException("Only pending reversals can be rejected");
        }

        reversal.markAsRejected(admin, notes);
        reversalRepository.save(reversal);

        // Send notification to user
        notificationService.sendReversalRejectedNotification(reversal);

        log.info("Reversal rejected: {} by admin: {}", reversal.getReversalNumber(), admin.getUsername());
    }

    /**
     * Process an approved reversal
     * 
     * @param reversalId Reversal ID
     * @param adminId Admin user ID
     */
    public void processReversal(Long reversalId, Long adminId) {
        TransactionReversal reversal = getReversalById(reversalId);
        User admin = userService.findById(adminId);

        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can process reversals");
        }

        if (!reversal.isApproved()) {
            throw new IllegalStateException("Only approved reversals can be processed");
        }

        Transaction originalTransaction = reversal.getTransaction();
        BigDecimal reversalAmount = reversal.getReversalAmount();

        // Create reversal transaction
        Transaction reversalTransaction;
        if (originalTransaction.getTransactionType() == Transaction.TransactionType.TRANSFER) {
            // Reverse transfer: send money back
            if (originalTransaction.getReceiver() == null || originalTransaction.getSender() == null) {
                throw new IllegalStateException("Invalid transaction for reversal");
            }
            reversalTransaction = transactionService.createTransfer(
                    originalTransaction.getReceiver().getId(),
                    originalTransaction.getSender().getEmail(),
                    reversalAmount,
                    "Reversal: " + originalTransaction.getDescription()
            );
        } else if (originalTransaction.getTransactionType() == Transaction.TransactionType.PAYMENT) {
            // Reverse payment: refund to original payer
            if (originalTransaction.getReceiver() == null || originalTransaction.getSender() == null) {
                throw new IllegalStateException("Invalid transaction for reversal");
            }
            reversalTransaction = transactionService.createTransfer(
                    originalTransaction.getReceiver().getId(),
                    originalTransaction.getSender().getEmail(),
                    reversalAmount,
                    "Refund: " + originalTransaction.getDescription()
            );
        } else {
            throw new IllegalStateException("Cannot reverse transaction type: " + originalTransaction.getTransactionType());
        }

        reversal.markAsProcessed(reversalTransaction);
        reversalRepository.save(reversal);

        // Send notification to user
        notificationService.sendReversalProcessedNotification(reversal);

        log.info("Reversal processed: {} - Transaction: {}", reversal.getReversalNumber(), reversalTransaction.getId());
    }

    /**
     * Get reversal by ID
     * 
     * @param reversalId Reversal ID
     * @return TransactionReversal entity
     */
    @Transactional(readOnly = true)
    public TransactionReversal getReversalById(Long reversalId) {
        return reversalRepository.findById(reversalId)
                .orElseThrow(() -> new IllegalArgumentException("Reversal not found: " + reversalId));
    }

    /**
     * Get reversals requested by user
     * 
     * @param userId User ID
     * @param pageable Pageable object
     * @return Page of reversals
     */
    @Transactional(readOnly = true)
    public Page<TransactionReversal> getReversalsByUser(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return reversalRepository.findByRequestedByOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get pending reversals (for admin)
     * 
     * @param pageable Pageable object
     * @return Page of reversals
     */
    @Transactional(readOnly = true)
    public Page<TransactionReversal> getPendingReversals(Pageable pageable) {
        return reversalRepository.findPendingReversals(pageable);
    }

    /**
     * Get all reversals (for admin)
     * 
     * @param pageable Pageable object
     * @return Page of reversals
     */
    @Transactional(readOnly = true)
    public Page<TransactionReversal> getAllReversals(Pageable pageable) {
        return reversalRepository.findAll(pageable);
    }

    /**
     * Get reversal statistics (for admin)
     * 
     * @return Map of statistics
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getReversalStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalReversals", reversalRepository.count());
        stats.put("pendingReversals", reversalRepository.countByStatus(TransactionReversal.ReversalStatus.PENDING));
        stats.put("approvedReversals", reversalRepository.countByStatus(TransactionReversal.ReversalStatus.APPROVED));
        stats.put("processedReversals", reversalRepository.countByStatus(TransactionReversal.ReversalStatus.PROCESSED));
        stats.put("rejectedReversals", reversalRepository.countByStatus(TransactionReversal.ReversalStatus.REJECTED));
        return stats;
    }

    /**
     * Check if transaction is reversible
     */
    private boolean isTransactionReversible(Transaction transaction) {
        // Can only reverse completed transactions
        if (transaction.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            return false;
        }

        // Cannot reverse reversals themselves
        if (transaction.getDescription() != null && transaction.getDescription().startsWith("Reversal:")) {
            return false;
        }

        // Check if transaction is not too old (e.g., within 90 days)
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        if (transaction.getCreatedAt().isBefore(ninetyDaysAgo)) {
            return false;
        }

        return true;
    }

    /**
     * Validate reversal amount
     */
    private void validateReversalAmount(Transaction transaction, TransactionReversal.ReversalType type, BigDecimal amount) {
        BigDecimal transactionAmount = transaction.getAmount();

        if (type == TransactionReversal.ReversalType.FULL) {
            if (amount.compareTo(transactionAmount) != 0) {
                throw new IllegalArgumentException("Full reversal amount must equal transaction amount");
            }
        } else if (type == TransactionReversal.ReversalType.PARTIAL) {
            if (amount.compareTo(transactionAmount) >= 0) {
                throw new IllegalArgumentException("Partial reversal amount must be less than transaction amount");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Reversal amount must be greater than zero");
            }
        } else if (type == TransactionReversal.ReversalType.REFUND) {
            // Refund can be full or partial
            if (amount.compareTo(transactionAmount) > 0) {
                throw new IllegalArgumentException("Refund amount cannot exceed transaction amount");
            }
        }
    }

    /**
     * Generate unique reversal number
     * 
     * @return Reversal number
     */
    private String generateReversalNumber() {
        String reversalNumber;
        do {
            reversalNumber = "REV" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (reversalRepository.findByReversalNumber(reversalNumber).isPresent());
        return reversalNumber;
    }
}

