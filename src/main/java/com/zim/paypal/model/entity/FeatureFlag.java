package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * FeatureFlag entity for feature toggles
 * 
 * @author dexterwura
 */
@Entity
@Table(name = "feature_flags", indexes = {
    @Index(name = "idx_feature_flag_name", columnList = "feature_name", unique = true),
    @Index(name = "idx_feature_flag_enabled", columnList = "is_enabled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Feature name is required")
    private String featureName;

    @Column(name = "display_name", nullable = false, length = 200)
    @NotBlank(message = "Display name is required")
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "category", length = 50)
    private String category; // e.g., "PAYMENT", "SECURITY", "MERCHANT", etc.

    @Column(name = "requires_restart", nullable = false)
    @Builder.Default
    private Boolean requiresRestart = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy; // Admin username who last updated

    /**
     * Common feature names
     */
    public static class FeatureNames {
        public static final String PAYMENT_LINKS = "PAYMENT_LINKS";
        public static final String QR_CODE_PAYMENTS = "QR_CODE_PAYMENTS";
        public static final String INVOICING = "INVOICING";
        public static final String RECURRING_PAYMENTS = "RECURRING_PAYMENTS";
        public static final String MERCHANT_TOOLS = "MERCHANT_TOOLS";
        public static final String WEBHOOKS = "WEBHOOKS";
        public static final String TWO_FACTOR_AUTH = "TWO_FACTOR_AUTH";
        public static final String SERVICE_PURCHASES = "SERVICE_PURCHASES";
        public static final String BILL_SPLITS = "BILL_SPLITS";
        public static final String TRANSACTION_REVERSALS = "TRANSACTION_REVERSALS";
        public static final String MONEY_REQUESTS = "MONEY_REQUESTS";
        public static final String REWARDS_POINTS = "REWARDS_POINTS";
        public static final String MULTI_CURRENCY = "MULTI_CURRENCY";
        public static final String COUNTRY_RESTRICTIONS = "COUNTRY_RESTRICTIONS";
    }
}

