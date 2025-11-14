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
import java.util.HashSet;
import java.util.Set;

/**
 * BillSplit entity representing a bill split among multiple participants
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "bill_splits", indexes = {
    @Index(name = "idx_bill_split_creator", columnList = "creator_id"),
    @Index(name = "idx_bill_split_status", columnList = "status"),
    @Index(name = "idx_bill_split_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"creator", "participants"})
@ToString(exclude = {"creator", "participants"})
public class BillSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "split_number", nullable = false, unique = true, length = 30)
    private String splitNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @NotNull(message = "Creator is required")
    private User creator;

    @Column(nullable = false, length = 200)
    @NotBlank(message = "Description is required")
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "split_method", nullable = false)
    @Builder.Default
    private SplitMethod splitMethod = SplitMethod.EQUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SplitStatus status = SplitStatus.PENDING;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "billSplit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<BillSplitParticipant> participants = new HashSet<>();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for split methods
     */
    public enum SplitMethod {
        EQUAL, PERCENTAGE, CUSTOM
    }

    /**
     * Enumeration for split status
     */
    public enum SplitStatus {
        PENDING, PARTIALLY_PAID, PAID, CANCELLED, EXPIRED
    }

    /**
     * Check if split is fully paid
     * 
     * @return true if fully paid
     */
    public boolean isFullyPaid() {
        return paidAmount != null && paidAmount.compareTo(totalAmount) >= 0;
    }

    /**
     * Check if split is expired
     * 
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Mark as paid
     */
    public void markAsPaid() {
        this.status = SplitStatus.PAID;
        this.paidAmount = this.totalAmount;
    }

    /**
     * Update paid amount
     * 
     * @param amount Amount paid
     */
    public void addPaidAmount(BigDecimal amount) {
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        this.paidAmount = this.paidAmount.add(amount);
        
        if (this.paidAmount.compareTo(this.totalAmount) >= 0) {
            markAsPaid();
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = SplitStatus.PARTIALLY_PAID;
        }
    }
}

