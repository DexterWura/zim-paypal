package com.zim.paypal.repository;

import com.zim.paypal.model.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FeatureFlag entity
 * 
 * @author dexterwura
 */
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    /**
     * Find feature flag by feature name
     * 
     * @param featureName Feature name
     * @return Optional FeatureFlag
     */
    Optional<FeatureFlag> findByFeatureName(String featureName);

    /**
     * Find all enabled feature flags
     * 
     * @return List of enabled feature flags
     */
    List<FeatureFlag> findByIsEnabledTrue();

    /**
     * Find feature flags by category
     * 
     * @param category Category
     * @return List of feature flags
     */
    List<FeatureFlag> findByCategoryOrderByDisplayNameAsc(String category);
}

