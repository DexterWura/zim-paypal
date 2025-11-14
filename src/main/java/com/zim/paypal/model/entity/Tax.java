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
 * Tax entity representing tax rates
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "taxes", indexes = {
    @Index(name = "idx_tax_code", columnList = "tax_code"),
    @Index(name = "idx_tax_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class Tax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_name", nullable = false, length = 100)
    @NotBlank(message = "Tax name is required")
    private String taxName;

    @Column(name = "tax_code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Tax code is required")
    private String taxCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false)
    @NotNull(message = "Tax type is required")
    private TaxType taxType;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.00", message = "Tax rate must be non-negative")
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private Transaction.TransactionType transactionType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "regulation_reference", length = 200)
    private String regulationReference;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

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
     * Enumeration for tax types
     */
    public enum TaxType {
        VAT, INCOME_TAX, TRANSACTION_TAX, SERVICE_TAX, OTHER
    }

    /**
     * Calculate tax amount based on transaction amount
     * 
     * @param transactionAmount Transaction amount
     * @return Calculated tax amount
     */
    public BigDecimal calculateTax(BigDecimal transactionAmount) {
        if (!isActive) {
            return BigDecimal.ZERO;
        }

        // Check if tax is effective
        LocalDateTime now = LocalDateTime.now();
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return BigDecimal.ZERO;
        }
        if (effectiveTo != null && now.isAfter(effectiveTo)) {
            return BigDecimal.ZERO;
        }

        if (taxRate != null && transactionAmount != null) {
            return transactionAmount
                    .multiply(taxRate)
                    .divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Check if tax is currently effective
     * 
     * @return true if tax is effective
     */
    public boolean isEffective() {
        if (!isActive) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveTo != null && now.isAfter(effectiveTo)) {
            return false;
        }

        return true;
    }
}

