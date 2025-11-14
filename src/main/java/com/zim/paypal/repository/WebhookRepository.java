package com.zim.paypal.repository;

import com.zim.paypal.model.entity.User;
import com.zim.paypal.model.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Webhook entity
 * 
 * @author dexterwura
 */
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    /**
     * Find webhooks by user
     * 
     * @param user User entity
     * @return List of webhooks
     */
    List<Webhook> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find active webhooks by user
     * 
     * @param user User entity
     * @return List of active webhooks
     */
    List<Webhook> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user);

    /**
     * Find active webhooks subscribed to event type
     * 
     * @param eventType Event type
     * @return List of active webhooks
     */
    List<Webhook> findByIsActiveTrueAndEventsContaining(Webhook.EventType eventType);
}

