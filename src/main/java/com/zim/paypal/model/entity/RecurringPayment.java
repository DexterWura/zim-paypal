package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RecurringPayment entity for subscriptions and recurring payments
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "recurring_payments", indexes = {
    @Index(name = "idx_recurring_subscriber", columnList = "subscriber_id"),
    @Index(name = "idx_recurring_merchant", columnList = "merchant_id"),
    @Index(name = "idx_recurring_status", columnList = "status"),
    @Index(name = "idx_recurring_next_payment", columnList = "next_payment_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"subscriber", "merchant", "subscriptionPlan", "account"})
@ToString(exclude = {"subscriber", "merchant", "subscriptionPlan", "account"})
public class RecurringPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Subscription ID is required")
    private String subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    @NotNull(message = "Subscriber is required")
    private User subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @NotNull(message = "Merchant is required")
    private User merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan subscriptionPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account is required")
    private Account account;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    @NotNull(message = "Billing cycle is required")
    private SubscriptionPlan.BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @Column(name = "next_payment_date", nullable = false)
    @NotNull(message = "Next payment date is required")
    private LocalDateTime nextPaymentDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    @Column(name = "total_payments", nullable = false)
    @Builder.Default
    private Integer totalPayments = 0;

    @Column(name = "failed_payments", nullable = false)
    @Builder.Default
    private Integer failedPayments = 0;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = true;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for subscription status
     */
    public enum Status {
        ACTIVE, PAUSED, CANCELLED, EXPIRED, SUSPENDED
    }

    /**
     * Check if subscription is in trial period
     * 
     * @return true if in trial
     */
    public boolean isInTrial() {
        if (trialEndDate == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(trialEndDate);
    }

    /**
     * Calculate next payment date based on billing cycle
     */
    public void calculateNextPaymentDate() {
        LocalDateTime now = LocalDateTime.now();
        switch (billingCycle) {
            case DAILY:
                this.nextPaymentDate = now.plusDays(1);
                break;
            case WEEKLY:
                this.nextPaymentDate = now.plusWeeks(1);
                break;
            case MONTHLY:
                this.nextPaymentDate = now.plusMonths(1);
                break;
            case QUARTERLY:
                this.nextPaymentDate = now.plusMonths(3);
                break;
            case YEARLY:
                this.nextPaymentDate = now.plusYears(1);
                break;
        }
    }
}

