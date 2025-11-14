package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * CountryRestriction entity for managing allowed countries
 * 
 * @author dexterwura
 */
@Entity
@Table(name = "country_restrictions", indexes = {
    @Index(name = "idx_country_code", columnList = "country_code", unique = true),
    @Index(name = "idx_country_enabled", columnList = "is_enabled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class CountryRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, unique = true, length = 2)
    @NotBlank(message = "Country code is required")
    private String countryCode; // ISO 3166-1 alpha-2 (e.g., "US", "ZW", "GB")

    @Column(name = "country_name", nullable = false, length = 100)
    @NotBlank(message = "Country name is required")
    private String countryName;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "is_registration_allowed", nullable = false)
    @Builder.Default
    private Boolean isRegistrationAllowed = true;

    @Column(name = "is_transaction_allowed", nullable = false)
    @Builder.Default
    private Boolean isTransactionAllowed = true;

    @Column(name = "is_merchant_allowed", nullable = false)
    @Builder.Default
    private Boolean isMerchantAllowed = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Admin notes about restrictions

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy; // Admin username who last updated
}

