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
 * AccountLimit entity representing transaction and account limits
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "account_limits", indexes = {
    @Index(name = "idx_limit_type", columnList = "limit_type"),
    @Index(name = "idx_limit_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"createdBy", "updatedBy"})
@ToString(exclude = {"createdBy", "updatedBy"})
public class AccountLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "limit_name", nullable = false, length = 100)
    @NotBlank(message = "Limit name is required")
    private String limitName;

    @Column(name = "limit_code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Limit code is required")
    private String limitCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false)
    @NotNull(message = "Limit type is required")
    private LimitType limitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type")
    private PeriodType periodType;

    @Column(name = "max_accounts_per_user")
    private Integer maxAccountsPerUser;

    @Column(name = "max_transaction_amount", precision = 19, scale = 2)
    private BigDecimal maxTransactionAmount;

    @Column(name = "max_daily_amount", precision = 19, scale = 2)
    private BigDecimal maxDailyAmount;

    @Column(name = "max_weekly_amount", precision = 19, scale = 2)
    private BigDecimal maxWeeklyAmount;

    @Column(name = "max_monthly_amount", precision = 19, scale = 2)
    private BigDecimal maxMonthlyAmount;

    @Column(name = "max_daily_count")
    private Integer maxDailyCount;

    @Column(name = "max_weekly_count")
    private Integer maxWeeklyCount;

    @Column(name = "max_monthly_count")
    private Integer maxMonthlyCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private User.UserRole userRole;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
     * Enumeration for limit types
     */
    public enum LimitType {
        ACCOUNT_COUNT, TRANSACTION_AMOUNT, TRANSACTION_COUNT, COMBINED
    }

    /**
     * Enumeration for period types
     */
    public enum PeriodType {
        DAILY, WEEKLY, MONTHLY, LIFETIME
    }
}

