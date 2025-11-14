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
 * PaymentLink entity for shareable payment links
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "payment_links", indexes = {
    @Index(name = "idx_payment_link_code", columnList = "link_code"),
    @Index(name = "idx_payment_link_creator", columnList = "creator_id"),
    @Index(name = "idx_payment_link_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"creator", "currency"})
@ToString(exclude = {"creator", "currency"})
public class PaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Link code is required")
    private String linkCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @NotNull(message = "Creator is required")
    private User creator;

    @Column(name = "title", nullable = false, length = 200)
    @NotBlank(message = "Title is required")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false)
    @Builder.Default
    private LinkType linkType = LinkType.ONE_TIME;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    @Builder.Default
    private Integer currentUses = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "allow_partial_payment", nullable = false)
    @Builder.Default
    private Boolean allowPartialPayment = false;

    @Column(name = "collect_shipping_address", nullable = false)
    @Builder.Default
    private Boolean collectShippingAddress = false;

    @Column(name = "collect_phone_number", nullable = false)
    @Builder.Default
    private Boolean collectPhoneNumber = false;

    @Column(name = "email_notification", nullable = false)
    @Builder.Default
    private Boolean emailNotification = true;

    @Column(name = "return_url", length = 500)
    private String returnUrl;

    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for payment link status
     */
    public enum Status {
        ACTIVE, PAUSED, EXPIRED, COMPLETED, CANCELLED
    }

    /**
     * Enumeration for link types
     */
    public enum LinkType {
        ONE_TIME, MULTIPLE_USE, RECURRING
    }

    /**
     * Check if link is still valid
     * 
     * @return true if valid
     */
    public boolean isValid() {
        if (status != Status.ACTIVE) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        if (maxUses != null && currentUses >= maxUses) {
            return false;
        }
        return true;
    }

    /**
     * Increment usage count
     */
    public void incrementUses() {
        this.currentUses++;
        if (maxUses != null && currentUses >= maxUses) {
            this.status = Status.COMPLETED;
        }
    }
}

