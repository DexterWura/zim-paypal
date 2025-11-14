package com.zim.paypal.service;

import com.zim.paypal.model.dto.ServicePurchaseDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.AccountRepository;
import com.zim.paypal.repository.ServiceProviderRepository;
import com.zim.paypal.repository.ServicePurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Service for service purchase management (airtime, data, ZESA tokens)
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ServicePurchaseService {

    private final ServicePurchaseRepository purchaseRepository;
    private final ServiceProviderRepository providerRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    /**
     * Purchase service (airtime, data, or ZESA token)
     * 
     * @param userId User ID
     * @param purchaseDto Purchase DTO
     * @return Created service purchase
     */
    public ServicePurchase purchaseService(Long userId, ServicePurchaseDto purchaseDto) {
        User user = userService.findById(userId);
        Account account = accountRepository.findByUser(user).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user"));

        ServiceProvider provider = providerRepository.findById(purchaseDto.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Service provider not found"));

        if (!provider.getIsActive()) {
            throw new IllegalStateException("Service provider is not active");
        }

        if (!provider.supportsService(purchaseDto.getServiceType())) {
            throw new IllegalStateException("Provider does not support this service type");
        }

        // Validate amount
        if (provider.getMinAmount() != null && purchaseDto.getAmount().compareTo(provider.getMinAmount()) < 0) {
            throw new IllegalArgumentException("Amount is below minimum: " + provider.getMinAmount());
        }
        if (provider.getMaxAmount() != null && purchaseDto.getAmount().compareTo(provider.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum: " + provider.getMaxAmount());
        }

        // Calculate service fee
        BigDecimal serviceFee = BigDecimal.ZERO;
        if (provider.getServiceFeePercentage() != null && provider.getServiceFeePercentage().compareTo(BigDecimal.ZERO) > 0) {
            serviceFee = purchaseDto.getAmount()
                    .multiply(provider.getServiceFeePercentage())
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = purchaseDto.getAmount().add(serviceFee);

        // Check account balance
        if (account.getBalance().compareTo(totalAmount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        String referenceNumber = generateReferenceNumber();

        ServicePurchase purchase = ServicePurchase.builder()
                .referenceNumber(referenceNumber)
                .user(user)
                .serviceProvider(provider)
                .serviceType(purchaseDto.getServiceType())
                .recipientNumber(purchaseDto.getRecipientNumber())
                .amount(purchaseDto.getAmount())
                .serviceFee(serviceFee)
                .totalAmount(totalAmount)
                .status(ServicePurchase.PurchaseStatus.PENDING)
                .build();

        ServicePurchase savedPurchase = purchaseRepository.save(purchase);

        // Create transaction
        Transaction transaction = transactionService.createPaymentFromWallet(
                userId,
                totalAmount,
                "Service Purchase: " + purchaseDto.getServiceType() + " - " + provider.getProviderName(),
                null
        );

        savedPurchase.setTransaction(transaction);
        savedPurchase.markAsProcessing();
        purchaseRepository.save(savedPurchase);

        // Process with provider (async in production)
        try {
            processWithProvider(savedPurchase);
        } catch (Exception e) {
            log.error("Error processing service purchase with provider: {}", e.getMessage(), e);
            savedPurchase.markAsFailed("Provider processing error: " + e.getMessage());
            purchaseRepository.save(savedPurchase);
            // Refund transaction
            refundPurchase(savedPurchase);
        }

        // Send notification
        notificationService.sendServicePurchaseNotification(savedPurchase);

        log.info("Service purchase created: {} by user: {}", referenceNumber, user.getUsername());
        return savedPurchase;
    }

    /**
     * Process purchase with provider (extensible for different providers)
     */
    private void processWithProvider(ServicePurchase purchase) {
        ServiceProvider provider = purchase.getServiceProvider();
        String providerCode = provider.getProviderCode().toUpperCase();

        // This is a placeholder - in production, this would call actual provider APIs
        // The structure allows easy extension for different providers
        switch (providerCode) {
            case "ECONET":
                processEconetPurchase(purchase);
                break;
            case "NETONE":
                processNetOnePurchase(purchase);
                break;
            case "TELECASH":
                processTelecashPurchase(purchase);
                break;
            case "ZESA":
                processZesaPurchase(purchase);
                break;
            default:
                // Generic provider processing
                processGenericProvider(purchase);
                break;
        }
    }

    /**
     * Process Econet purchase (placeholder - implement actual API call)
     */
    private void processEconetPurchase(ServicePurchase purchase) {
        // TODO: Implement Econet API integration
        // Example structure:
        // EconetApiClient client = new EconetApiClient(provider.getApiEndpoint(), provider.getApiKey());
        // EconetResponse response = client.purchaseAirtime(purchase.getRecipientNumber(), purchase.getAmount());
        
        // Simulate success for now
        String providerRef = "ECONET-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        purchase.markAsCompleted(providerRef, "Purchase successful via Econet");
        purchaseRepository.save(purchase);
    }

    /**
     * Process NetOne purchase (placeholder - implement actual API call)
     */
    private void processNetOnePurchase(ServicePurchase purchase) {
        // TODO: Implement NetOne API integration
        String providerRef = "NETONE-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        purchase.markAsCompleted(providerRef, "Purchase successful via NetOne");
        purchaseRepository.save(purchase);
    }

    /**
     * Process Telecash purchase (placeholder - implement actual API call)
     */
    private void processTelecashPurchase(ServicePurchase purchase) {
        // TODO: Implement Telecash API integration
        String providerRef = "TELECASH-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        purchase.markAsCompleted(providerRef, "Purchase successful via Telecash");
        purchaseRepository.save(purchase);
    }

    /**
     * Process ZESA purchase (placeholder - implement actual API call)
     */
    private void processZesaPurchase(ServicePurchase purchase) {
        // TODO: Implement ZESA API integration
        String providerRef = "ZESA-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        purchase.markAsCompleted(providerRef, "Token purchase successful via ZESA");
        purchaseRepository.save(purchase);
    }

    /**
     * Process generic provider
     */
    private void processGenericProvider(ServicePurchase purchase) {
        // Generic processing for providers without specific implementation
        String providerRef = purchase.getServiceProvider().getProviderCode() + "-" + 
                            UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        purchase.markAsCompleted(providerRef, "Purchase successful");
        purchaseRepository.save(purchase);
    }

    /**
     * Refund a failed purchase
     */
    private void refundPurchase(ServicePurchase purchase) {
        try {
            // Create refund transaction
            transactionService.createDeposit(
                    purchase.getUser().getId(),
                    purchase.getTotalAmount(),
                    "Refund: Failed service purchase - " + purchase.getReferenceNumber()
            );
            purchase.setStatus(ServicePurchase.PurchaseStatus.REFUNDED);
            purchaseRepository.save(purchase);
        } catch (Exception e) {
            log.error("Error refunding purchase: {}", e.getMessage(), e);
        }
    }

    /**
     * Get purchase by reference number
     * 
     * @param referenceNumber Reference number
     * @return ServicePurchase entity
     */
    @Transactional(readOnly = true)
    public ServicePurchase getPurchaseByReference(String referenceNumber) {
        return purchaseRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + referenceNumber));
    }

    /**
     * Get purchases by user
     * 
     * @param userId User ID
     * @param pageable Pageable object
     * @return Page of purchases
     */
    @Transactional(readOnly = true)
    public Page<ServicePurchase> getPurchasesByUser(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return purchaseRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get all active providers
     * 
     * @return List of providers
     */
    @Transactional(readOnly = true)
    public List<ServiceProvider> getActiveProviders() {
        return providerRepository.findByIsActiveTrue();
    }

    /**
     * Get providers by service type
     * 
     * @param serviceType Service type
     * @return List of providers
     */
    @Transactional(readOnly = true)
    public List<ServiceProvider> getProvidersByServiceType(ServicePurchase.ServiceType serviceType) {
        switch (serviceType) {
            case AIRTIME:
                return providerRepository.findBySupportsAirtimeTrueAndIsActiveTrue();
            case DATA:
                return providerRepository.findBySupportsDataTrueAndIsActiveTrue();
            case ZESA_TOKEN:
                return providerRepository.findBySupportsTokensTrueAndIsActiveTrue();
            default:
                return getActiveProviders();
        }
    }

    /**
     * Generate unique reference number
     * 
     * @return Reference number
     */
    private String generateReferenceNumber() {
        String referenceNumber;
        do {
            referenceNumber = "SRV" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (purchaseRepository.findByReferenceNumber(referenceNumber).isPresent());
        return referenceNumber;
    }
}

