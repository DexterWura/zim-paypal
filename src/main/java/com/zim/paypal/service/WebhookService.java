package com.zim.paypal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zim.paypal.model.dto.WebhookDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.WebhookEventRepository;
import com.zim.paypal.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for webhook management and delivery
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int RETRY_DELAY_MINUTES = 5;

    /**
     * Create a new webhook
     * 
     * @param webhookDto Webhook DTO
     * @param user User entity
     * @return Created webhook
     */
    public Webhook createWebhook(WebhookDto webhookDto, User user) {
        String secret = generateSecret();

        Webhook webhook = Webhook.builder()
                .user(user)
                .url(webhookDto.getUrl())
                .secret(secret)
                .events(webhookDto.getEvents())
                .isActive(true)
                .description(webhookDto.getDescription())
                .build();

        return webhookRepository.save(webhook);
    }

    /**
     * Trigger webhook event
     * 
     * @param eventType Event type
     * @param payload Event payload
     * @param userId User ID (optional, for user-specific webhooks)
     */
    public void triggerWebhook(Webhook.EventType eventType, Map<String, Object> payload, Long userId) {
        try {
            List<Webhook> webhooks = findWebhooksForEvent(eventType, userId);
            
            for (Webhook webhook : webhooks) {
                if (webhook.getEvents().contains(eventType)) {
                    String payloadJson = objectMapper.writeValueAsString(payload);
                    createWebhookEvent(webhook, eventType, payloadJson);
                }
            }
        } catch (Exception e) {
            log.error("Error triggering webhook: {}", e.getMessage(), e);
        }
    }

    /**
     * Create webhook event
     * 
     * @param webhook Webhook entity
     * @param eventType Event type
     * @param payload Payload JSON
     * @return Created webhook event
     */
    public WebhookEvent createWebhookEvent(Webhook webhook, Webhook.EventType eventType, String payload) {
        WebhookEvent webhookEvent = WebhookEvent.builder()
                .webhook(webhook)
                .eventType(eventType)
                .payload(payload)
                .status(WebhookEvent.DeliveryStatus.PENDING)
                .attempts(0)
                .build();

        return webhookEventRepository.save(webhookEvent);
    }

    /**
     * Process pending webhook events (scheduled job)
     */
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void processPendingWebhooks() {
        List<WebhookEvent> pendingEvents = webhookEventRepository
                .findByStatusOrderByCreatedAtAsc(WebhookEvent.DeliveryStatus.PENDING);

        for (WebhookEvent event : pendingEvents) {
            try {
                deliverWebhook(event);
            } catch (Exception e) {
                log.error("Error processing webhook event {}: {}", event.getId(), e.getMessage());
            }
        }

        // Process retrying events
        List<WebhookEvent> retryingEvents = webhookEventRepository
                .findByStatusAndNextRetryAtLessThanEqual(
                        WebhookEvent.DeliveryStatus.RETRYING, LocalDateTime.now());

        for (WebhookEvent event : retryingEvents) {
            try {
                deliverWebhook(event);
            } catch (Exception e) {
                log.error("Error retrying webhook event {}: {}", event.getId(), e.getMessage());
            }
        }
    }

    /**
     * Deliver webhook event
     * 
     * @param event Webhook event
     */
    private void deliverWebhook(WebhookEvent event) {
        event.incrementAttempts();
        webhookEventRepository.save(event);

        try {
            String signature = generateSignature(event.getPayload(), event.getWebhook().getSecret());
            
            // In production, use RestTemplate or WebClient for HTTP requests
            // For now, we'll simulate the delivery
            boolean success = sendHttpRequest(event.getWebhook().getUrl(), event.getPayload(), signature);

            if (success) {
                event.markAsSuccess(200, "Webhook delivered successfully");
                webhookEventRepository.save(event);
                log.info("Webhook event {} delivered successfully", event.getId());
            } else {
                handleFailedDelivery(event);
            }
        } catch (Exception e) {
            log.error("Error delivering webhook event {}: {}", event.getId(), e.getMessage());
            handleFailedDelivery(event);
        }
    }

    /**
     * Send HTTP request to webhook URL
     * 
     * @param url Webhook URL
     * @param payload Payload
     * @param signature Signature
     * @return true if successful
     */
    private boolean sendHttpRequest(String url, String payload, String signature) {
        // TODO: Implement actual HTTP request using RestTemplate or WebClient
        // For now, return true to simulate success
        log.debug("Sending webhook to {} with signature {}", url, signature);
        return true;
    }

    /**
     * Handle failed webhook delivery
     * 
     * @param event Webhook event
     */
    private void handleFailedDelivery(WebhookEvent event) {
        if (event.getAttempts() >= MAX_RETRY_ATTEMPTS) {
            event.markAsFailed(0, "Max retry attempts reached");
        } else {
            LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES * event.getAttempts());
            event.scheduleRetry(nextRetry);
        }
        webhookEventRepository.save(event);
    }

    /**
     * Generate webhook signature
     * 
     * @param payload Payload
     * @param secret Secret key
     * @return Signature
     */
    public String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating webhook signature: {}", e.getMessage());
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Verify webhook signature
     * 
     * @param payload Payload
     * @param signature Signature
     * @param secret Secret key
     * @return true if valid
     */
    public boolean verifySignature(String payload, String signature, String secret) {
        String expectedSignature = generateSignature(payload, secret);
        return expectedSignature.equals(signature);
    }

    /**
     * Find webhooks for event
     * 
     * @param eventType Event type
     * @param userId User ID (optional)
     * @return List of webhooks
     */
    private List<Webhook> findWebhooksForEvent(Webhook.EventType eventType, Long userId) {
        if (userId != null) {
            User user = userService.findById(userId);
            return webhookRepository.findByUserAndIsActiveTrueOrderByCreatedAtDesc(user);
        }
        return webhookRepository.findByIsActiveTrueAndEventsContaining(eventType);
    }

    /**
     * Generate webhook secret
     * 
     * @return Secret string
     */
    private String generateSecret() {
        return "whsec_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Get webhooks by user
     * 
     * @param user User entity
     * @return List of webhooks
     */
    @Transactional(readOnly = true)
    public List<Webhook> getWebhooksByUser(User user) {
        return webhookRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get webhook by ID
     * 
     * @param webhookId Webhook ID
     * @return Webhook entity
     */
    @Transactional(readOnly = true)
    public Webhook getWebhookById(Long webhookId) {
        return webhookRepository.findById(webhookId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));
    }

    /**
     * Get webhook events
     * 
     * @param webhook Webhook entity
     * @param pageable Pageable object
     * @return Page of webhook events
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<WebhookEvent> getWebhookEvents(
            Webhook webhook, org.springframework.data.domain.Pageable pageable) {
        return webhookEventRepository.findByWebhookOrderByCreatedAtDesc(webhook, pageable);
    }

    /**
     * Delete webhook
     * 
     * @param webhookId Webhook ID
     */
    public void deleteWebhook(Long webhookId) {
        Webhook webhook = getWebhookById(webhookId);
        webhookRepository.delete(webhook);
        log.info("Webhook deleted: {}", webhookId);
    }

    // Inject required services
    private final UserService userService;
}

