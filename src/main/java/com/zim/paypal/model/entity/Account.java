package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Account entity representing a user's wallet/account balance
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_user", columnList = "user_id"),
    @Index(name = "idx_account_number", columnList = "account_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "transactions"})
@ToString(exclude = {"user", "transactions"})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    @Builder.Default
    private AccountType accountType = AccountType.PERSONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Transaction> transactions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for account types
     */
    public enum AccountType {
        PERSONAL, BUSINESS, MERCHANT
    }

    /**
     * Enumeration for account status
     */
    public enum AccountStatus {
        ACTIVE, SUSPENDED, CLOSED
    }

    /**
     * Deposit money into account
     * 
     * @param amount Amount to deposit
     * @throws IllegalArgumentException if amount is negative or zero
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Withdraw money from account
     * 
     * @param amount Amount to withdraw
     * @throws IllegalArgumentException if amount is negative, zero, or exceeds balance
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Check if account has sufficient balance
     * 
     * @param amount Amount to check
     * @return true if balance is sufficient
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    /**
     * Check if account is active
     * 
     * @return true if account status is ACTIVE
     */
    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }
}

