package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RiskScore entity for tracking user and transaction risk levels
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "risk_scores", indexes = {
    @Index(name = "idx_risk_user", columnList = "user_id"),
    @Index(name = "idx_risk_score", columnList = "risk_score"),
    @Index(name = "idx_risk_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "transaction"})
@ToString(exclude = {"user", "transaction"})
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Risk score is required")
    @DecimalMin(value = "0.00", message = "Risk score must be non-negative")
    @DecimalMax(value = "100.00", message = "Risk score cannot exceed 100")
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    @NotNull(message = "Risk level is required")
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_type", nullable = false)
    @NotNull(message = "Score type is required")
    private ScoreType scoreType;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for risk levels
     */
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Enumeration for score types
     */
    public enum ScoreType {
        USER_PROFILE, TRANSACTION, ACCOUNT_ACTIVITY, PATTERN_ANALYSIS
    }
}

