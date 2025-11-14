package com.zim.paypal.controller.api;

import com.zim.paypal.model.dto.ApiResponse;
import com.zim.paypal.model.dto.WebhookDto;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.model.entity.Webhook;
import com.zim.paypal.model.entity.WebhookEvent;
import com.zim.paypal.service.UserService;
import com.zim.paypal.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Webhook Management
 * 
 * @author dexterwura
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Webhook>>> getWebhooks(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<Webhook> webhooks = webhookService.getWebhooksByUser(user);
            return ResponseEntity.ok(ApiResponse.success(webhooks));
        } catch (Exception e) {
            log.error("Error getting webhooks: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Webhook>> createWebhook(@Valid @RequestBody WebhookDto webhookDto,
                                                              Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Webhook webhook = webhookService.createWebhook(webhookDto, user);
            return ResponseEntity.ok(ApiResponse.success("Webhook created successfully", webhook));
        } catch (Exception e) {
            log.error("Error creating webhook: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{webhookId}")
    public ResponseEntity<ApiResponse<Webhook>> getWebhook(@PathVariable Long webhookId,
                                                           Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Webhook webhook = webhookService.getWebhookById(webhookId);
            
            if (!webhook.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(webhook));
        } catch (Exception e) {
            log.error("Error getting webhook: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{webhookId}/events")
    public ResponseEntity<ApiResponse<Page<WebhookEvent>>> getWebhookEvents(
            @PathVariable Long webhookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Webhook webhook = webhookService.getWebhookById(webhookId);
            
            if (!webhook.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            Page<WebhookEvent> events = webhookService.getWebhookEvents(webhook, PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success(events));
        } catch (Exception e) {
            log.error("Error getting webhook events: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{webhookId}")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(@PathVariable Long webhookId,
                                                           Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Webhook webhook = webhookService.getWebhookById(webhookId);
            
            if (!webhook.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            webhookService.deleteWebhook(webhookId);
            return ResponseEntity.ok(ApiResponse.success("Webhook deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting webhook: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

