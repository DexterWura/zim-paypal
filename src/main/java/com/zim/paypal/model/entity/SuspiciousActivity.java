package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SuspiciousActivity entity for tracking flagged activities
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "suspicious_activities", indexes = {
    @Index(name = "idx_suspicious_user", columnList = "user_id"),
    @Index(name = "idx_suspicious_transaction", columnList = "transaction_id"),
    @Index(name = "idx_suspicious_status", columnList = "status"),
    @Index(name = "idx_suspicious_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "transaction", "reviewedBy"})
@ToString(exclude = {"user", "transaction", "reviewedBy"})
public class SuspiciousActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    @NotNull(message = "Severity is required")
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Description is required")
    private String description;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Column(name = "auto_detected", nullable = false)
    @Builder.Default
    private Boolean autoDetected = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

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
     * Enumeration for activity types
     */
    public enum ActivityType {
        UNUSUAL_TRANSACTION, RAPID_TRANSFERS, LARGE_AMOUNT, STRUCTURING, 
        UNUSUAL_PATTERN, ACCOUNT_TAKEOVER, IDENTITY_THEFT, MONEY_LAUNDERING,
        FRAUDULENT_ACTIVITY, SUSPICIOUS_BEHAVIOR, OTHER
    }

    /**
     * Enumeration for severity levels
     */
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Enumeration for review status
     */
    public enum Status {
        PENDING, UNDER_REVIEW, APPROVED, REJECTED, ESCALATED, RESOLVED
    }
}

