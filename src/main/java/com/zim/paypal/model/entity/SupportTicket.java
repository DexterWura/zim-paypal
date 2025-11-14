package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SupportTicket entity representing customer support tickets
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_ticket_user", columnList = "user_id"),
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_priority", columnList = "priority"),
    @Index(name = "idx_ticket_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "assignedTo"})
@ToString(exclude = {"user", "assignedTo"})
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 20)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(nullable = false, length = 200)
    @NotBlank(message = "Subject is required")
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Description is required")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @NotNull(message = "Category is required")
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "last_response_at")
    private LocalDateTime lastResponseAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for ticket categories
     */
    public enum TicketCategory {
        ACCOUNT, PAYMENT, TRANSACTION, CARD, SECURITY, TECHNICAL, OTHER
    }

    /**
     * Enumeration for ticket priority
     */
    public enum TicketPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    /**
     * Enumeration for ticket status
     */
    public enum TicketStatus {
        OPEN, IN_PROGRESS, WAITING_CUSTOMER, RESOLVED, CLOSED
    }

    /**
     * Check if ticket is open
     * 
     * @return true if ticket is open or in progress
     */
    public boolean isOpen() {
        return status == TicketStatus.OPEN || status == TicketStatus.IN_PROGRESS || 
               status == TicketStatus.WAITING_CUSTOMER;
    }

    /**
     * Mark ticket as resolved
     * 
     * @param resolution Resolution text
     */
    public void markAsResolved(String resolution) {
        this.status = TicketStatus.RESOLVED;
        this.resolution = resolution;
        this.closedAt = LocalDateTime.now();
    }

    /**
     * Mark ticket as closed
     */
    public void markAsClosed() {
        this.status = TicketStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    /**
     * Update last response time
     */
    public void updateLastResponse() {
        this.lastResponseAt = LocalDateTime.now();
    }
}

