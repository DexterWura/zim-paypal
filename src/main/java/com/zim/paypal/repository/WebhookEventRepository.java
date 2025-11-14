package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Webhook;
import com.zim.paypal.model.entity.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for WebhookEvent entity
 * 
 * @author dexterwura
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    /**
     * Find events by webhook
     * 
     * @param webhook Webhook entity
     * @param pageable Pageable object
     * @return Page of events
     */
    Page<WebhookEvent> findByWebhookOrderByCreatedAtDesc(Webhook webhook, Pageable pageable);

    /**
     * Find pending events
     * 
     * @return List of pending events
     */
    List<WebhookEvent> findByStatusOrderByCreatedAtAsc(WebhookEvent.DeliveryStatus status);

    /**
     * Find events ready for retry
     * 
     * @param now Current date time
     * @return List of events ready for retry
     */
    List<WebhookEvent> findByStatusAndNextRetryAtLessThanEqual(
            WebhookEvent.DeliveryStatus status, LocalDateTime now);

    /**
     * Find failed events with attempts less than max
     * 
     * @param maxAttempts Maximum attempts
     * @return List of failed events
     */
    List<WebhookEvent> findByStatusAndAttemptsLessThan(
            WebhookEvent.DeliveryStatus status, Integer maxAttempts);
}

