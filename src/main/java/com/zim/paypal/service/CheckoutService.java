package com.zim.paypal.service;

import com.zim.paypal.model.dto.CheckoutRequestDto;
import com.zim.paypal.model.entity.PaymentButton;
import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for checkout processing
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CheckoutService {

    private final PaymentButtonService buttonService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final WebhookService webhookService;

    /**
     * Process checkout payment
     * 
     * @param checkoutRequest Checkout request DTO
     * @return Transaction entity
     */
    public Transaction processCheckout(CheckoutRequestDto checkoutRequest) {
        PaymentButton button = buttonService.getPaymentButtonByCode(checkoutRequest.getButtonCode());
        
        if (!button.getIsActive()) {
            throw new IllegalStateException("Payment button is inactive");
        }

        // Validate amount
        if (button.getAmount() != null && !button.getAllowCustomAmount()) {
            if (checkoutRequest.getAmount().compareTo(button.getAmount()) != 0) {
                throw new IllegalArgumentException("Amount must be " + button.getAmount());
            }
        }

        // Find or create customer
        User customer = null;
        try {
            customer = userService.findByEmail(checkoutRequest.getCustomerEmail());
        } catch (Exception e) {
            throw new IllegalArgumentException("Customer must have an account to pay. Please register first.");
        }

        // Process payment
        Transaction transaction = transactionService.createPaymentFromWallet(
                customer.getId(),
                checkoutRequest.getAmount(),
                checkoutRequest.getDescription() != null ? checkoutRequest.getDescription() : 
                    "Payment via button: " + button.getButtonName(),
                button.getMerchant().getId());

        // Record payment on button
        button.recordPayment(checkoutRequest.getAmount());
        buttonService.updatePaymentButton(button);

        // Trigger webhook if notify URL is set
        if (button.getNotifyUrl() != null) {
            try {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("buttonCode", button.getButtonCode());
                payload.put("transactionId", transaction.getId());
                payload.put("amount", transaction.getAmount());
                payload.put("status", transaction.getStatus().name());
                webhookService.triggerWebhook(
                    com.zim.paypal.model.entity.Webhook.EventType.PAYMENT_LINK_PAID,
                    payload,
                    button.getMerchant().getId());
            } catch (Exception e) {
                log.warn("Failed to trigger webhook: {}", e.getMessage());
            }
        }

        log.info("Checkout processed: button {} by customer {}", button.getButtonCode(), customer.getEmail());
        return transaction;
    }
}

