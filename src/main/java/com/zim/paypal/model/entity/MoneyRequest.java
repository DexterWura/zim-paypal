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
 * MoneyRequest entity representing a request for money from one user to another
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "money_requests", indexes = {
    @Index(name = "idx_request_requester", columnList = "requester_id"),
    @Index(name = "idx_request_recipient", columnList = "recipient_id"),
    @Index(name = "idx_request_status", columnList = "status"),
    @Index(name = "idx_request_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"requester", "recipient", "transaction"})
@ToString(exclude = {"requester", "recipient", "transaction"})
public class MoneyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 30)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @NotNull(message = "Requester is required")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    @NotNull(message = "Recipient is required")
    private User recipient;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "note", length = 1000)
    private String note;

    @OneToOne(mappedBy = "moneyRequest", cascade = CascadeType.ALL)
    private Transaction transaction;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /**
     * Enumeration for request status
     */
    public enum RequestStatus {
        PENDING, APPROVED, DECLINED, EXPIRED, CANCELLED
    }

    /**
     * Check if request is pending
     * 
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return this.status == RequestStatus.PENDING;
    }

    /**
     * Check if request is expired
     * 
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Mark request as approved
     */
    public void markAsApproved() {
        this.status = RequestStatus.APPROVED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Mark request as declined
     */
    public void markAsDeclined() {
        this.status = RequestStatus.DECLINED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Mark request as cancelled
     */
    public void markAsCancelled() {
        this.status = RequestStatus.CANCELLED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Mark request as expired
     */
    public void markAsExpired() {
        this.status = RequestStatus.EXPIRED;
    }
}

