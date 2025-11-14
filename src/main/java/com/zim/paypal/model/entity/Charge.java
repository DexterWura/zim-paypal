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
 * Charge entity representing transaction charges/fees
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "charges", indexes = {
    @Index(name = "idx_charge_type", columnList = "charge_type"),
    @Index(name = "idx_charge_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "charge_name", nullable = false, length = 100)
    @NotBlank(message = "Charge name is required")
    private String chargeName;

    @Column(name = "charge_code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Charge code is required")
    private String chargeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false)
    @NotNull(message = "Charge type is required")
    private ChargeType chargeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private Transaction.TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_method", nullable = false)
    @NotNull(message = "Charge method is required")
    private ChargeMethod chargeMethod;

    @Column(name = "fixed_amount", precision = 19, scale = 2)
    private BigDecimal fixedAmount;

    @Column(name = "percentage_rate", precision = 5, scale = 2)
    private BigDecimal percentageRate;

    @Column(name = "min_amount", precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "regulation_reference", length = 200)
    private String regulationReference;

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
     * Enumeration for charge types
     */
    public enum ChargeType {
        TRANSACTION_FEE, TRANSFER_FEE, WITHDRAWAL_FEE, DEPOSIT_FEE, PAYMENT_FEE, SERVICE_FEE, OTHER
    }

    /**
     * Enumeration for charge calculation methods
     */
    public enum ChargeMethod {
        FIXED, PERCENTAGE, TIERED, FIXED_PLUS_PERCENTAGE
    }

    /**
     * Calculate charge amount based on transaction amount
     * 
     * @param transactionAmount Transaction amount
     * @return Calculated charge amount
     */
    public BigDecimal calculateCharge(BigDecimal transactionAmount) {
        if (!isActive) {
            return BigDecimal.ZERO;
        }

        BigDecimal charge = BigDecimal.ZERO;

        switch (chargeMethod) {
            case FIXED:
                if (fixedAmount != null) {
                    charge = fixedAmount;
                }
                break;

            case PERCENTAGE:
                if (percentageRate != null && transactionAmount != null) {
                    charge = transactionAmount
                            .multiply(percentageRate)
                            .divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
                }
                break;

            case FIXED_PLUS_PERCENTAGE:
                if (fixedAmount != null) {
                    charge = fixedAmount;
                }
                if (percentageRate != null && transactionAmount != null) {
                    charge = charge.add(transactionAmount
                            .multiply(percentageRate)
                            .divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP));
                }
                break;

            case TIERED:
                // Tiered calculation would require additional tier configuration
                // For now, use percentage as fallback
                if (percentageRate != null && transactionAmount != null) {
                    charge = transactionAmount
                            .multiply(percentageRate)
                            .divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
                }
                break;
        }

        // Apply min/max limits
        if (minAmount != null && charge.compareTo(minAmount) < 0) {
            charge = minAmount;
        }
        if (maxAmount != null && charge.compareTo(maxAmount) > 0) {
            charge = maxAmount;
        }

        return charge;
    }
}

