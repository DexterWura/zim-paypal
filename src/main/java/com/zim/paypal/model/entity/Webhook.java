package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Webhook entity for webhook configurations
 * 
 * @author dexterwura
 */
@Entity
@Table(name = "webhooks", indexes = {
    @Index(name = "idx_webhook_user", columnList = "user_id"),
    @Index(name = "idx_webhook_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "events"})
@ToString(exclude = {"user", "events"})
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "url", nullable = false, length = 500)
    @NotBlank(message = "URL is required")
    private String url;

    @Column(name = "secret", nullable = false, length = 100)
    @NotBlank(message = "Secret is required")
    private String secret;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "webhook_events", joinColumns = @JoinColumn(name = "webhook_id"))
    @Column(name = "event_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<EventType> events = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "webhook", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WebhookEvent> webhookEvents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for webhook event types
     */
    public enum EventType {
        TRANSACTION_CREATED,
        TRANSACTION_COMPLETED,
        TRANSACTION_FAILED,
        PAYMENT_LINK_CREATED,
        PAYMENT_LINK_PAID,
        INVOICE_CREATED,
        INVOICE_PAID,
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_CANCELLED,
        MONEY_REQUEST_CREATED,
        MONEY_REQUEST_APPROVED,
        MONEY_REQUEST_DECLINED,
        ACCOUNT_CREATED,
        ACCOUNT_UPDATED
    }
}

