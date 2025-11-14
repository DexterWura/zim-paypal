package com.zim.paypal.service;

import com.zim.paypal.model.entity.FeatureFlag;
import com.zim.paypal.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for feature flag management
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;

    /**
     * Check if a feature is enabled
     * 
     * @param featureName Feature name
     * @return true if enabled, false otherwise
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "featureFlags", key = "#featureName")
    public boolean isFeatureEnabled(String featureName) {
        return featureFlagRepository.findByFeatureName(featureName)
                .map(FeatureFlag::getIsEnabled)
                .orElse(true); // Default to enabled if not found
    }

    /**
     * Get feature flag by name
     * 
     * @param featureName Feature name
     * @return FeatureFlag entity
     */
    @Transactional(readOnly = true)
    public FeatureFlag getFeatureFlag(String featureName) {
        return featureFlagRepository.findByFeatureName(featureName)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found: " + featureName));
    }

    /**
     * Get all feature flags
     * 
     * @return List of all feature flags
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getAllFeatureFlags() {
        return featureFlagRepository.findAll();
    }

    /**
     * Get feature flags by category
     * 
     * @param category Category
     * @return List of feature flags
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> getFeatureFlagsByCategory(String category) {
        return featureFlagRepository.findByCategoryOrderByDisplayNameAsc(category);
    }

    /**
     * Toggle feature flag
     * 
     * @param featureName Feature name
     * @param enabled Enabled status
     * @param updatedBy Admin username
     * @return Updated FeatureFlag
     */
    @CacheEvict(value = "featureFlags", key = "#featureName")
    public FeatureFlag toggleFeature(String featureName, Boolean enabled, String updatedBy) {
        FeatureFlag flag = getFeatureFlag(featureName);
        flag.setIsEnabled(enabled);
        flag.setUpdatedBy(updatedBy);
        FeatureFlag updated = featureFlagRepository.save(flag);
        log.info("Feature flag {} toggled to {} by {}", featureName, enabled, updatedBy);
        return updated;
    }

    /**
     * Create or update feature flag
     * 
     * @param featureFlag FeatureFlag entity
     * @param updatedBy Admin username
     * @return Saved FeatureFlag
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public FeatureFlag saveFeatureFlag(FeatureFlag featureFlag, String updatedBy) {
        featureFlag.setUpdatedBy(updatedBy);
        FeatureFlag saved = featureFlagRepository.save(featureFlag);
        log.info("Feature flag {} saved by {}", saved.getFeatureName(), updatedBy);
        return saved;
    }
}

