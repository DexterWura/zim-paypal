package com.zim.paypal.service;

import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.GatewayTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

/**
 * Service for managing gateway transactions
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GatewayTransactionService {

    private final GatewayTransactionRepository gatewayTransactionRepository;
    private final PaymentGatewayService gatewayService;
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final PaynowIntegrationService paynowIntegrationService;

    /**
     * Initiate a deposit via payment gateway
     */
    public GatewayTransaction initiateDeposit(Long userId, Long gatewayId, BigDecimal amount, 
                                             String phoneNumber, String email) {
        User user = userService.findById(userId);
        Account account = accountService.findActiveAccountByUser(user);
        PaymentGateway gateway = gatewayService.getGatewayById(gatewayId);

        if (!gateway.getIsEnabled()) {
            throw new IllegalStateException("Payment gateway is not enabled");
        }

        GatewayTransaction gatewayTransaction = GatewayTransaction.builder()
                .user(user)
                .account(account)
                .gateway(gateway)
                .amount(amount)
                .currencyCode(account.getCurrencyCode())
                .status(GatewayTransaction.TransactionStatus.PENDING)
                .phoneNumber(phoneNumber)
                .email(email)
                .build();

        gatewayTransaction = gatewayTransactionRepository.save(gatewayTransaction);
        log.info("Gateway transaction initiated: {} for user: {}", gatewayTransaction.getId(), user.getUsername());

        // Process the payment
        processGatewayPayment(gatewayTransaction);

        return gatewayTransaction;
    }

    /**
     * Process gateway payment using actual gateway APIs
     */
    private void processGatewayPayment(GatewayTransaction gatewayTransaction) {
        PaymentGateway gateway = gatewayTransaction.getGateway();
        String gatewayName = gateway.getGatewayName();

        gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.PROCESSING);
        gatewayTransactionRepository.save(gatewayTransaction);

        try {
            if ("PAYNOW_ZIM".equalsIgnoreCase(gatewayName)) {
                processPaynowPayment(gatewayTransaction);
            } else if ("ECOCASH".equalsIgnoreCase(gatewayName)) {
                processPaynowMobilePayment(gatewayTransaction);
            } else if ("PAYPAL".equalsIgnoreCase(gatewayName)) {
                processPayPalPayment(gatewayTransaction);
            } else {
                log.warn("Gateway {} not yet fully integrated, using fallback", gatewayName);
                processFallbackPayment(gatewayTransaction);
            }
        } catch (Exception e) {
            log.error("Error processing gateway payment: {}", e.getMessage(), e);
            gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.FAILED);
            gatewayTransaction.setGatewayResponse("Error: " + e.getMessage());
            gatewayTransactionRepository.save(gatewayTransaction);
        }
    }

    /**
     * Process Paynow web payment
     */
    private void processPaynowPayment(GatewayTransaction gatewayTransaction) {
        try {
            Object response = paynowIntegrationService.initiateWebPayment(
                    gatewayTransaction.getGateway(),
                    gatewayTransaction
            );

            // Use reflection to get response data
            Method success = response.getClass().getMethod("success");
            Boolean isSuccess = (Boolean) success.invoke(response);

            if (isSuccess) {
                Method pollUrl = response.getClass().getMethod("pollUrl");
                String pollUrlStr = (String) pollUrl.invoke(response);
                Method redirectURL = response.getClass().getMethod("redirectURL");
                String redirectUrlStr = (String) redirectURL.invoke(response);

                gatewayTransaction.setGatewayTransactionId(pollUrlStr);
                gatewayTransaction.setGatewayResponse("Redirect URL: " + redirectUrlStr);
                gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.PROCESSING);
                log.info("Paynow web payment initiated. Redirect URL: {}", redirectUrlStr);
            } else {
                Method errors = response.getClass().getMethod("errors");
                String errorStr = (String) errors.invoke(response);
                gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.FAILED);
                gatewayTransaction.setGatewayResponse("Paynow error: " + errorStr);
            }
            gatewayTransactionRepository.save(gatewayTransaction);
        } catch (Exception e) {
            log.error("Error processing Paynow payment: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Process Paynow mobile payment (EcoCash, OneMoney)
     */
    private void processPaynowMobilePayment(GatewayTransaction gatewayTransaction) {
        try {
            if (gatewayTransaction.getPhoneNumber() == null || gatewayTransaction.getPhoneNumber().isEmpty()) {
                throw new IllegalArgumentException("Phone number is required for mobile money payments");
            }

            Object response = paynowIntegrationService.initiateMobilePayment(
                    gatewayTransaction.getGateway(),
                    gatewayTransaction
            );

            // Use reflection to get response data
            Method success = response.getClass().getMethod("success");
            Boolean isSuccess = (Boolean) success.invoke(response);

            if (isSuccess) {
                Method pollUrl = response.getClass().getMethod("pollUrl");
                String pollUrlStr = (String) pollUrl.invoke(response);
                Method instructions = response.getClass().getMethod("instructions");
                String instructionsStr = (String) instructions.invoke(response);

                gatewayTransaction.setGatewayTransactionId(pollUrlStr);
                gatewayTransaction.setGatewayResponse("Instructions: " + instructionsStr);
                gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.PROCESSING);
                log.info("Paynow mobile payment initiated. Poll URL: {}", pollUrlStr);
            } else {
                Method errors = response.getClass().getMethod("errors");
                String errorStr = (String) errors.invoke(response);
                gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.FAILED);
                gatewayTransaction.setGatewayResponse("Paynow error: " + errorStr);
            }
            gatewayTransactionRepository.save(gatewayTransaction);
        } catch (Exception e) {
            log.error("Error processing Paynow mobile payment: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Process PayPal payment (placeholder for future implementation)
     */
    private void processPayPalPayment(GatewayTransaction gatewayTransaction) {
        log.warn("PayPal integration not yet implemented");
        processFallbackPayment(gatewayTransaction);
    }

    /**
     * Fallback payment processing (for testing or unsupported gateways)
     */
    private void processFallbackPayment(GatewayTransaction gatewayTransaction) {
        try {
            Transaction depositTransaction = transactionService.createDeposit(
                    gatewayTransaction.getUser().getId(),
                    gatewayTransaction.getAmount(),
                    "Deposit via " + gatewayTransaction.getGateway().getDisplayName()
            );

            gatewayTransaction.setTransaction(depositTransaction);
            gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.COMPLETED);
            gatewayTransaction.setGatewayTransactionId("GTX" + gatewayTransaction.getId());
            gatewayTransactionRepository.save(gatewayTransaction);

            log.info("Fallback payment processed successfully: {}", gatewayTransaction.getId());
        } catch (Exception e) {
            log.error("Error in fallback payment processing: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get gateway transactions for user
     */
    @Transactional(readOnly = true)
    public List<GatewayTransaction> getTransactionsByUser(Long userId) {
        User user = userService.findById(userId);
        return gatewayTransactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get gateway transaction by gateway transaction ID
     */
    @Transactional(readOnly = true)
    public GatewayTransaction getByGatewayTransactionId(String gatewayTransactionId) {
        return gatewayTransactionRepository.findByGatewayTransactionId(gatewayTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("Gateway transaction not found"));
    }

    /**
     * Poll and update transaction status from gateway
     */
    public void pollAndUpdateTransactionStatus(GatewayTransaction gatewayTransaction) {
        try {
            PaymentGateway gateway = gatewayTransaction.getGateway();
            String pollUrl = gatewayTransaction.getGatewayTransactionId();

            if (pollUrl == null || !pollUrl.startsWith("http")) {
                log.warn("Invalid poll URL for transaction: {}", gatewayTransaction.getId());
                return;
            }

            if ("PAYNOW_ZIM".equalsIgnoreCase(gateway.getGatewayName()) || 
                "ECOCASH".equalsIgnoreCase(gateway.getGatewayName())) {
                Object status = paynowIntegrationService.pollTransactionStatus(gateway, pollUrl);

                // Use reflection to check if paid
                Method isPaid = status.getClass().getMethod("isPaid");
                Boolean paid = (Boolean) isPaid.invoke(status);

                if (paid) {
                    if (gatewayTransaction.getTransaction() == null) {
                        Transaction depositTransaction = transactionService.createDeposit(
                                gatewayTransaction.getUser().getId(),
                                gatewayTransaction.getAmount(),
                                "Deposit via " + gateway.getDisplayName()
                        );
                        gatewayTransaction.setTransaction(depositTransaction);
                    }
                    gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.COMPLETED);
                    log.info("Transaction {} confirmed as paid", gatewayTransaction.getId());
                } else {
                    gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.PENDING);
                }
                gatewayTransactionRepository.save(gatewayTransaction);
            }
        } catch (Exception e) {
            log.error("Error polling transaction status: {}", e.getMessage(), e);
        }
    }
}
