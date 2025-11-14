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
 * FraudRule entity for configurable fraud detection rules
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "fraud_rules", indexes = {
    @Index(name = "idx_rule_code", columnList = "rule_code"),
    @Index(name = "idx_rule_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"createdBy", "updatedBy"})
@ToString(exclude = {"createdBy", "updatedBy"})
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 100)
    @NotBlank(message = "Rule name is required")
    private String ruleName;

    @Column(name = "rule_code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Rule code is required")
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    @NotNull(message = "Action type is required")
    private ActionType actionType;

    @Column(name = "threshold_amount", precision = 19, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "threshold_count")
    private Integer thresholdCount;

    @Column(name = "time_window_minutes")
    private Integer timeWindowMinutes;

    @Column(name = "risk_score_threshold", precision = 5, scale = 2)
    private BigDecimal riskScoreThreshold;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_conditions", columnDefinition = "TEXT")
    private String ruleConditions;

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
     * Enumeration for rule types
     */
    public enum RuleType {
        AMOUNT_THRESHOLD, VELOCITY_CHECK, PATTERN_DETECTION, GEOGRAPHIC_ANOMALY,
        DEVICE_FINGERPRINT, BEHAVIORAL_ANALYSIS, ACCOUNT_AGE, TRANSACTION_FREQUENCY,
        UNUSUAL_HOURS, CROSS_BORDER, STRUCTURING_DETECTION, OTHER
    }

    /**
     * Enumeration for action types
     */
    public enum ActionType {
        FLAG, BLOCK, REVIEW, ALERT, FREEZE_ACCOUNT, REQUIRE_VERIFICATION
    }
}

