package com.zim.paypal.service;

import com.zim.paypal.model.entity.MerchantApiKey;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.MerchantApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service for merchant API key management
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MerchantApiKeyService {

    private final MerchantApiKeyRepository apiKeyRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Create a new API key
     * 
     * @param keyName Key name
     * @param description Description
     * @param merchant Merchant user
     * @return Created API key (with plain text secret for display)
     */
    public MerchantApiKey createApiKey(String keyName, String description, User merchant) {
        String apiKey = generateApiKey();
        String apiSecret = generateApiSecret();
        String hashedSecret = passwordEncoder.encode(apiSecret);

        MerchantApiKey key = MerchantApiKey.builder()
                .merchant(merchant)
                .apiKey(apiKey)
                .apiSecret(hashedSecret)
                .keyName(keyName)
                .description(description)
                .isActive(true)
                .build();

        MerchantApiKey saved = apiKeyRepository.save(key);
        
        // Return with plain text secret for one-time display
        saved.setApiSecret(apiSecret);
        
        log.info("API key created: {} for merchant: {}", apiKey, merchant.getUsername());
        return saved;
    }

    /**
     * Validate API key and secret
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @return MerchantApiKey if valid
     */
    @Transactional(readOnly = true)
    public MerchantApiKey validateApiKey(String apiKey, String apiSecret) {
        MerchantApiKey key = apiKeyRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        if (!key.getIsActive()) {
            throw new IllegalStateException("API key is inactive");
        }

        if (!passwordEncoder.matches(apiSecret, key.getApiSecret())) {
            throw new IllegalArgumentException("Invalid API secret");
        }

        key.recordUsage();
        apiKeyRepository.save(key);

        return key;
    }

    /**
     * Get API keys by merchant
     * 
     * @param merchant Merchant user
     * @return List of API keys (without secrets)
     */
    @Transactional(readOnly = true)
    public List<MerchantApiKey> getApiKeysByMerchant(User merchant) {
        List<MerchantApiKey> keys = apiKeyRepository.findByMerchantOrderByCreatedAtDesc(merchant);
        // Clear secrets for security
        keys.forEach(k -> k.setApiSecret("***"));
        return keys;
    }

    /**
     * Revoke API key
     * 
     * @param keyId Key ID
     * @param merchant Merchant user
     */
    public void revokeApiKey(Long keyId, User merchant) {
        MerchantApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        if (!key.getMerchant().getId().equals(merchant.getId())) {
            throw new IllegalArgumentException("Access denied");
        }

        key.setIsActive(false);
        apiKeyRepository.save(key);
        log.info("API key revoked: {}", key.getApiKey());
    }

    /**
     * Generate API key
     * 
     * @return API key string
     */
    private String generateApiKey() {
        String apiKey;
        do {
            apiKey = "pk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        } while (apiKeyRepository.findByApiKey(apiKey).isPresent());
        return apiKey;
    }

    /**
     * Generate API secret
     * 
     * @return API secret string
     */
    private String generateApiSecret() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}

