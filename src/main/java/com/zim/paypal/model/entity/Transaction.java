package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity representing all financial transactions
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_sender", columnList = "sender_id"),
    @Index(name = "idx_transaction_receiver", columnList = "receiver_id"),
    @Index(name = "idx_transaction_account", columnList = "account_id"),
    @Index(name = "idx_transaction_card", columnList = "card_id"),
    @Index(name = "idx_transaction_type", columnList = "transaction_type"),
    @Index(name = "idx_transaction_status", columnList = "status"),
    @Index(name = "idx_transaction_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"sender", "receiver", "account", "card"})
@ToString(exclude = {"sender", "receiver", "account", "card"})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_number", nullable = false, unique = true, length = 30)
    private String transactionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "fee", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Enumeration for transaction types
     */
    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, REFUND, CHARGEBACK
    }

    /**
     * Enumeration for payment methods
     */
    public enum PaymentMethod {
        WALLET, CARD, BANK_TRANSFER
    }

    /**
     * Enumeration for transaction status
     */
    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED
    }

    /**
     * Calculate net amount after fees
     */
    public void calculateNetAmount() {
        if (amount != null && fee != null) {
            if (transactionType == TransactionType.DEPOSIT || 
                transactionType == TransactionType.TRANSFER && sender != null) {
                this.netAmount = amount.subtract(fee);
            } else {
                this.netAmount = amount.add(fee);
            }
        } else {
            this.netAmount = amount;
        }
    }

    /**
     * Mark transaction as completed
     */
    public void markAsCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark transaction as failed
     * 
     * @param reason Failure reason
     */
    public void markAsFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }
}

