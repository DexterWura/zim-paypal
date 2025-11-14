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
 * SubscriptionPlan entity for subscription plans
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "subscription_plans", indexes = {
    @Index(name = "idx_plan_code", columnList = "plan_code"),
    @Index(name = "idx_plan_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"createdBy", "updatedBy"})
@ToString(exclude = {"createdBy", "updatedBy"})
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_name", nullable = false, length = 100)
    @NotBlank(message = "Plan name is required")
    private String planName;

    @Column(name = "plan_code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Plan code is required")
    private String planCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    @Column(name = "trial_period_days")
    private Integer trialPeriodDays;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for billing cycles
     */
    public enum BillingCycle {
        DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    }
}

