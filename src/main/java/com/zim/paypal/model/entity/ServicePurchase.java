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
 * ServicePurchase entity representing airtime/data/ZESA token purchases
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "service_purchases", indexes = {
    @Index(name = "idx_purchase_provider", columnList = "service_provider_id"),
    @Index(name = "idx_purchase_user", columnList = "user_id"),
    @Index(name = "idx_purchase_status", columnList = "status"),
    @Index(name = "idx_purchase_reference", columnList = "reference_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "serviceProvider", "transaction"})
@ToString(exclude = {"user", "serviceProvider", "transaction"})
public class ServicePurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 50)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_provider_id", nullable = false)
    @NotNull(message = "Service provider is required")
    private ServiceProvider serviceProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Column(name = "recipient_number", nullable = false, length = 20)
    @NotBlank(message = "Recipient number is required")
    private String recipientNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Column(name = "service_fee", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for service types
     */
    public enum ServiceType {
        AIRTIME, DATA, ZESA_TOKEN
    }

    /**
     * Enumeration for purchase status
     */
    public enum PurchaseStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
    }

    /**
     * Mark as completed
     * 
     * @param providerReference Provider reference number
     * @param providerResponse Provider response
     */
    public void markAsCompleted(String providerReference, String providerResponse) {
        this.status = PurchaseStatus.COMPLETED;
        this.providerReference = providerReference;
        this.providerResponse = providerResponse;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark as failed
     * 
     * @param errorMessage Error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = PurchaseStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark as processing
     */
    public void markAsProcessing() {
        this.status = PurchaseStatus.PROCESSING;
    }
}

