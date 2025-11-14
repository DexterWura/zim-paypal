package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Currency entity representing supported currencies
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "currencies", indexes = {
    @Index(name = "idx_currency_code", columnList = "currency_code"),
    @Index(name = "idx_currency_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_code", nullable = false, unique = true, length = 3)
    @NotBlank(message = "Currency code is required")
    private String currencyCode;

    @Column(name = "currency_name", nullable = false, length = 100)
    @NotBlank(message = "Currency name is required")
    private String currencyName;

    @Column(name = "symbol", nullable = false, length = 10)
    @NotBlank(message = "Symbol is required")
    private String symbol;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_base_currency", nullable = false)
    @Builder.Default
    private Boolean isBaseCurrency = false;

    @Column(name = "decimal_places", nullable = false)
    @Builder.Default
    private Integer decimalPlaces = 2;

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
}

