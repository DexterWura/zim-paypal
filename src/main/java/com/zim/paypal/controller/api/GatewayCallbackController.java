package com.zim.paypal.controller.api;

import com.zim.paypal.model.entity.GatewayTransaction;
import com.zim.paypal.service.GatewayTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling payment gateway callbacks/webhooks
 * 
 * @author dexterwura
 */
@RestController
@RequestMapping("/api/gateways")
@RequiredArgsConstructor
@Slf4j
public class GatewayCallbackController {

    private final GatewayTransactionService gatewayTransactionService;

    /**
     * Handle Paynow callback/webhook
     */
    @PostMapping("/paynow/callback")
    public ResponseEntity<String> handlePaynowCallback(@RequestParam Map<String, String> params) {
        try {
            log.info("Received Paynow callback: {}", params);
            
            // Extract poll URL or transaction reference from callback
            String pollUrl = params.get("pollurl");
            String reference = params.get("reference");
            
            if (pollUrl != null) {
                // Find gateway transaction by poll URL
                try {
                    GatewayTransaction gatewayTransaction = 
                            gatewayTransactionService.getByGatewayTransactionId(pollUrl);
                    
                    // Poll and update transaction status
                    gatewayTransactionService.pollAndUpdateTransactionStatus(gatewayTransaction);
                    
                    log.info("Paynow callback processed successfully for transaction: {}", 
                            gatewayTransaction.getId());
                } catch (Exception e) {
                    log.error("Error processing Paynow callback: {}", e.getMessage());
                }
            }
            
            // Return OK to Paynow
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error handling Paynow callback: {}", e.getMessage(), e);
            return ResponseEntity.ok("ERROR");
        }
    }

    /**
     * Handle Paynow return URL (user redirected back after payment)
     */
    @GetMapping("/paynow/return")
    public String handlePaynowReturn(@RequestParam Map<String, String> params) {
        log.info("User returned from Paynow: {}", params);
        // Redirect to deposit page with status
        return "redirect:/deposit?status=processing";
    }
}

