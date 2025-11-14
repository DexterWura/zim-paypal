package com.zim.paypal.service;

import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.GatewayTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Process the payment (this would integrate with actual gateway APIs)
        processGatewayPayment(gatewayTransaction);

        return gatewayTransaction;
    }

    /**
     * Process gateway payment (placeholder for actual gateway integration)
     */
    private void processGatewayPayment(GatewayTransaction gatewayTransaction) {
        // This is a placeholder - in production, this would:
        // 1. Call the actual gateway API (EcoCash, PayNow, PayPal)
        // 2. Handle the response
        // 3. Update transaction status
        // 4. Create deposit transaction if successful

        gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.PROCESSING);
        gatewayTransactionRepository.save(gatewayTransaction);

        // Simulate successful payment for now
        // In production, this would be handled via webhook/callback
        try {
            // Create deposit transaction
            Transaction depositTransaction = transactionService.createDeposit(
                    gatewayTransaction.getUser().getId(),
                    gatewayTransaction.getAmount(),
                    "Deposit via " + gatewayTransaction.getGateway().getDisplayName()
            );

            gatewayTransaction.setTransaction(depositTransaction);
            gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.COMPLETED);
            gatewayTransaction.setGatewayTransactionId("GTX" + gatewayTransaction.getId());
            gatewayTransactionRepository.save(gatewayTransaction);

            log.info("Gateway transaction completed: {}", gatewayTransaction.getId());
        } catch (Exception e) {
            log.error("Error processing gateway payment: {}", e.getMessage());
            gatewayTransaction.setStatus(GatewayTransaction.TransactionStatus.FAILED);
            gatewayTransaction.setGatewayResponse("Error: " + e.getMessage());
            gatewayTransactionRepository.save(gatewayTransaction);
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
}

