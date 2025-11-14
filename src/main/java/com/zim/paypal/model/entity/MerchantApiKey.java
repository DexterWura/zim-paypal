package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * MerchantApiKey entity for API key management
 * 
 * @author dexterwura
 */
@Entity
@Table(name = "merchant_api_keys", indexes = {
    @Index(name = "idx_api_key_merchant", columnList = "merchant_id"),
    @Index(name = "idx_api_key_key", columnList = "api_key"),
    @Index(name = "idx_api_key_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"merchant"})
@ToString(exclude = {"merchant", "apiKey"})
public class MerchantApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @NotNull(message = "Merchant is required")
    private User merchant;

    @Column(name = "api_key", nullable = false, unique = true, length = 100)
    @NotBlank(message = "API key is required")
    private String apiKey;

    @Column(name = "api_secret", nullable = false, length = 100)
    @NotBlank(message = "API secret is required")
    private String apiSecret;

    @Column(name = "key_name", nullable = false, length = 200)
    @NotBlank(message = "Key name is required")
    private String keyName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Record API key usage
     */
    public void recordUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }
}

