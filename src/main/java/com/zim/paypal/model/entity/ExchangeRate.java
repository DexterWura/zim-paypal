package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ExchangeRate entity representing currency exchange rates
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "exchange_rates", indexes = {
    @Index(name = "idx_rate_from_currency", columnList = "from_currency_id"),
    @Index(name = "idx_rate_to_currency", columnList = "to_currency_id"),
    @Index(name = "idx_rate_effective", columnList = "effective_from, effective_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"fromCurrency", "toCurrency", "createdBy", "updatedBy"})
@ToString(exclude = {"fromCurrency", "toCurrency", "createdBy", "updatedBy"})
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_currency_id", nullable = false)
    @NotNull(message = "From currency is required")
    private Currency fromCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_currency_id", nullable = false)
    @NotNull(message = "To currency is required")
    private Currency toCurrency;

    @Column(name = "rate", nullable = false, precision = 19, scale = 6)
    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.000001", message = "Rate must be greater than zero")
    private BigDecimal rate;

    @Column(name = "effective_from", nullable = false)
    @NotNull(message = "Effective from date is required")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

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
     * Check if rate is currently effective
     * 
     * @return true if effective
     */
    public boolean isEffective() {
        if (!isActive) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveTo != null && now.isAfter(effectiveTo)) {
            return false;
        }

        return true;
    }
}

