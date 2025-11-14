package com.zim.paypal.repository;

import com.zim.paypal.model.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SubscriptionPlan entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    /**
     * Find plan by plan code
     * 
     * @param planCode Plan code
     * @return Optional SubscriptionPlan
     */
    Optional<SubscriptionPlan> findByPlanCode(String planCode);

    /**
     * Find all active plans
     * 
     * @return List of active plans
     */
    List<SubscriptionPlan> findByIsActiveTrue();
}

