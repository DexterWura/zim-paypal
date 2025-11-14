package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * ServiceProvider entity representing telecom/utility service providers
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "service_providers", indexes = {
    @Index(name = "idx_provider_code", columnList = "provider_code"),
    @Index(name = "idx_provider_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"purchases"})
@ToString(exclude = {"purchases"})
public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_code", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Provider code is required")
    private String providerCode;

    @Column(name = "provider_name", nullable = false, length = 100)
    @NotBlank(message = "Provider name is required")
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    @NotNull(message = "Provider type is required")
    private ProviderType providerType;

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "api_secret", length = 500)
    private String apiSecret;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "supports_airtime")
    @Builder.Default
    private Boolean supportsAirtime = false;

    @Column(name = "supports_data")
    @Builder.Default
    private Boolean supportsData = false;

    @Column(name = "supports_tokens")
    @Builder.Default
    private Boolean supportsTokens = false;

    @Column(name = "min_amount", precision = 19, scale = 2)
    private java.math.BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 2)
    private java.math.BigDecimal maxAmount;

    @Column(name = "service_fee_percentage", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal serviceFeePercentage = java.math.BigDecimal.ZERO;

    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ServicePurchase> purchases = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for provider types
     */
    public enum ProviderType {
        TELECOM, UTILITY, OTHER
    }

    /**
     * Check if provider supports a service type
     * 
     * @param serviceType Service type
     * @return true if supported
     */
    public boolean supportsService(ServicePurchase.ServiceType serviceType) {
        switch (serviceType) {
            case AIRTIME:
                return supportsAirtime != null && supportsAirtime;
            case DATA:
                return supportsData != null && supportsData;
            case ZESA_TOKEN:
                return supportsTokens != null && supportsTokens;
            default:
                return false;
        }
    }
}

