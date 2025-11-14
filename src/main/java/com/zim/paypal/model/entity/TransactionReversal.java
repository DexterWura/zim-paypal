package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionReversal entity representing a reversal request or processed reversal
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "transaction_reversals", indexes = {
    @Index(name = "idx_reversal_transaction", columnList = "transaction_id"),
    @Index(name = "idx_reversal_user", columnList = "requested_by_id"),
    @Index(name = "idx_reversal_status", columnList = "status"),
    @Index(name = "idx_reversal_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"transaction", "requestedBy", "processedBy"})
@ToString(exclude = {"transaction", "requestedBy", "processedBy"})
public class TransactionReversal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reversal_number", nullable = false, unique = true, length = 30)
    private String reversalNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    @NotNull(message = "Transaction is required")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    @NotNull(message = "Requested by is required")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Reversal amount is required")
    private BigDecimal reversalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reversal_type", nullable = false)
    @NotNull(message = "Reversal type is required")
    private ReversalType reversalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReversalStatus status = ReversalStatus.PENDING;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Reason is required")
    private String reason;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_transaction_id")
    private Transaction reversalTransaction;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for reversal types
     */
    public enum ReversalType {
        FULL, PARTIAL, REFUND
    }

    /**
     * Enumeration for reversal status
     */
    public enum ReversalStatus {
        PENDING, APPROVED, REJECTED, PROCESSED, CANCELLED
    }

    /**
     * Check if reversal is pending
     * 
     * @return true if pending
     */
    public boolean isPending() {
        return status == ReversalStatus.PENDING;
    }

    /**
     * Check if reversal is approved
     * 
     * @return true if approved
     */
    public boolean isApproved() {
        return status == ReversalStatus.APPROVED;
    }

    /**
     * Check if reversal is processed
     * 
     * @return true if processed
     */
    public boolean isProcessed() {
        return status == ReversalStatus.PROCESSED;
    }

    /**
     * Mark as approved
     * 
     * @param admin Admin user who approved
     * @param notes Admin notes
     */
    public void markAsApproved(User admin, String notes) {
        this.status = ReversalStatus.APPROVED;
        this.processedBy = admin;
        this.adminNotes = notes;
    }

    /**
     * Mark as rejected
     * 
     * @param admin Admin user who rejected
     * @param notes Rejection reason
     */
    public void markAsRejected(User admin, String notes) {
        this.status = ReversalStatus.REJECTED;
        this.processedBy = admin;
        this.adminNotes = notes;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark as processed
     * 
     * @param reversalTransaction Reversal transaction
     */
    public void markAsProcessed(Transaction reversalTransaction) {
        this.status = ReversalStatus.PROCESSED;
        this.reversalTransaction = reversalTransaction;
        this.processedAt = LocalDateTime.now();
    }
}

