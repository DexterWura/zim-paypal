package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * WebhookEvent entity for tracking webhook deliveries
 * 
 * @author dexterwura
 */
@Entity
@Table(name = "webhook_event_deliveries", indexes = {
    @Index(name = "idx_webhook_event_webhook", columnList = "webhook_id"),
    @Index(name = "idx_webhook_event_status", columnList = "status"),
    @Index(name = "idx_webhook_event_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"webhook"})
@ToString(exclude = {"webhook"})
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", nullable = false)
    @NotNull(message = "Webhook is required")
    private Webhook webhook;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    @NotNull(message = "Event type is required")
    private Webhook.EventType eventType;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    @NotNull(message = "Payload is required")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enumeration for delivery status
     */
    public enum DeliveryStatus {
        PENDING, SUCCESS, FAILED, RETRYING
    }

    /**
     * Increment attempt count
     */
    public void incrementAttempts() {
        this.attempts++;
        this.lastAttemptAt = LocalDateTime.now();
    }

    /**
     * Mark as successful
     */
    public void markAsSuccess(Integer responseCode, String responseBody) {
        this.status = DeliveryStatus.SUCCESS;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    /**
     * Mark as failed
     */
    public void markAsFailed(Integer responseCode, String responseBody) {
        this.status = DeliveryStatus.FAILED;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    /**
     * Schedule retry
     */
    public void scheduleRetry(LocalDateTime nextRetry) {
        this.status = DeliveryStatus.RETRYING;
        this.nextRetryAt = nextRetry;
    }
}

