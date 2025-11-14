package com.zim.paypal.repository;

import com.zim.paypal.model.entity.MerchantApiKey;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MerchantApiKey entity
 * 
 * @author dexterwura
 */
@Repository
public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, Long> {

    /**
     * Find API key by key string
     * 
     * @param apiKey API key
     * @return Optional MerchantApiKey
     */
    Optional<MerchantApiKey> findByApiKey(String apiKey);

    /**
     * Find API keys by merchant
     * 
     * @param merchant Merchant user
     * @return List of API keys
     */
    List<MerchantApiKey> findByMerchantOrderByCreatedAtDesc(User merchant);

    /**
     * Find active API keys by merchant
     * 
     * @param merchant Merchant user
     * @return List of active API keys
     */
    List<MerchantApiKey> findByMerchantAndIsActiveTrueOrderByCreatedAtDesc(User merchant);
}

