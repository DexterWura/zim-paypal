package com.zim.paypal.service;

import com.zim.paypal.model.entity.PaymentGateway;
import com.zim.paypal.repository.PaymentGatewayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing payment gateways
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentGatewayService {

    private final PaymentGatewayRepository gatewayRepository;

    /**
     * Get all enabled payment gateways
     */
    @Transactional(readOnly = true)
    public List<PaymentGateway> getEnabledGateways() {
        return gatewayRepository.findByIsEnabledTrue();
    }

    /**
     * Get all payment gateways
     */
    @Transactional(readOnly = true)
    public List<PaymentGateway> getAllGateways() {
        return gatewayRepository.findAll();
    }

    /**
     * Get gateway by name
     */
    @Transactional(readOnly = true)
    public PaymentGateway getGatewayByName(String gatewayName) {
        return gatewayRepository.findByGatewayName(gatewayName)
                .orElseThrow(() -> new IllegalArgumentException("Payment gateway not found: " + gatewayName));
    }

    /**
     * Get gateway by ID
     */
    @Transactional(readOnly = true)
    public PaymentGateway getGatewayById(Long id) {
        return gatewayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment gateway not found: " + id));
    }

    /**
     * Update gateway configuration
     */
    public PaymentGateway updateGateway(PaymentGateway gateway) {
        PaymentGateway existing = getGatewayById(gateway.getId());
        existing.setDisplayName(gateway.getDisplayName());
        existing.setIsEnabled(gateway.getIsEnabled());
        existing.setApiKey(gateway.getApiKey());
        existing.setApiSecret(gateway.getApiSecret());
        existing.setMerchantId(gateway.getMerchantId());
        existing.setWebhookUrl(gateway.getWebhookUrl());
        existing.setCallbackUrl(gateway.getCallbackUrl());
        existing.setAdditionalConfig(gateway.getAdditionalConfig());
        return gatewayRepository.save(existing);
    }

    /**
     * Enable/disable gateway
     */
    public PaymentGateway toggleGateway(Long id, boolean enabled) {
        PaymentGateway gateway = getGatewayById(id);
        gateway.setIsEnabled(enabled);
        return gatewayRepository.save(gateway);
    }
}

