package com.zim.paypal.repository;

import com.zim.paypal.model.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FraudRule entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

    /**
     * Find rule by rule code
     * 
     * @param ruleCode Rule code
     * @return Optional FraudRule
     */
    Optional<FraudRule> findByRuleCode(String ruleCode);

    /**
     * Find all active rules
     * 
     * @return List of rules
     */
    List<FraudRule> findByIsActiveTrue();

    /**
     * Find rules by rule type
     * 
     * @param ruleType Rule type
     * @return List of rules
     */
    List<FraudRule> findByRuleTypeAndIsActiveTrue(FraudRule.RuleType ruleType);
}

