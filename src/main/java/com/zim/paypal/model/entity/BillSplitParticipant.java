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
 * BillSplitParticipant entity representing a participant in a bill split
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "bill_split_participants", indexes = {
    @Index(name = "idx_participant_split", columnList = "bill_split_id"),
    @Index(name = "idx_participant_user", columnList = "user_id"),
    @Index(name = "idx_participant_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"billSplit", "user", "transaction"})
@ToString(exclude = {"billSplit", "user", "transaction"})
public class BillSplitParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_split_id", nullable = false)
    @NotNull(message = "Bill split is required")
    private BillSplit billSplit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for payment status
     */
    public enum PaymentStatus {
        PENDING, PAID, PARTIALLY_PAID, CANCELLED
    }

    /**
     * Check if participant has paid
     * 
     * @return true if fully paid
     */
    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    /**
     * Mark as paid
     */
    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAmount = this.amount;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * Add paid amount
     * 
     * @param amount Amount paid
     */
    public void addPaidAmount(BigDecimal amount) {
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        this.paidAmount = this.paidAmount.add(amount);
        
        if (this.paidAmount.compareTo(this.amount) >= 0) {
            markAsPaid();
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = PaymentStatus.PARTIALLY_PAID;
        }
    }
}

