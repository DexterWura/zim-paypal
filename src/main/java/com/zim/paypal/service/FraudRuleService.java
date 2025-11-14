package com.zim.paypal.service;

import com.zim.paypal.model.dto.FraudRuleDto;
import com.zim.paypal.model.entity.FraudRule;
import com.zim.paypal.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for fraud rule management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FraudRuleService {

    private final FraudRuleRepository fraudRuleRepository;

    public FraudRule createRule(FraudRuleDto ruleDto, com.zim.paypal.model.entity.User createdBy) {
        if (fraudRuleRepository.findByRuleCode(ruleDto.getRuleCode()).isPresent()) {
            throw new IllegalArgumentException("Rule code already exists: " + ruleDto.getRuleCode());
        }

        FraudRule rule = FraudRule.builder()
                .ruleName(ruleDto.getRuleName())
                .ruleCode(ruleDto.getRuleCode())
                .ruleType(ruleDto.getRuleType())
                .actionType(ruleDto.getActionType())
                .thresholdAmount(ruleDto.getThresholdAmount())
                .thresholdCount(ruleDto.getThresholdCount())
                .timeWindowMinutes(ruleDto.getTimeWindowMinutes())
                .riskScoreThreshold(ruleDto.getRiskScoreThreshold())
                .isActive(ruleDto.getIsActive())
                .description(ruleDto.getDescription())
                .ruleConditions(ruleDto.getRuleConditions())
                .createdBy(createdBy)
                .build();

        return fraudRuleRepository.save(rule);
    }

    public FraudRule updateRule(Long ruleId, FraudRuleDto ruleDto, com.zim.paypal.model.entity.User updatedBy) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        if (!rule.getRuleCode().equals(ruleDto.getRuleCode())) {
            if (fraudRuleRepository.findByRuleCode(ruleDto.getRuleCode()).isPresent()) {
                throw new IllegalArgumentException("Rule code already exists: " + ruleDto.getRuleCode());
            }
        }

        rule.setRuleName(ruleDto.getRuleName());
        rule.setRuleCode(ruleDto.getRuleCode());
        rule.setRuleType(ruleDto.getRuleType());
        rule.setActionType(ruleDto.getActionType());
        rule.setThresholdAmount(ruleDto.getThresholdAmount());
        rule.setThresholdCount(ruleDto.getThresholdCount());
        rule.setTimeWindowMinutes(ruleDto.getTimeWindowMinutes());
        rule.setRiskScoreThreshold(ruleDto.getRiskScoreThreshold());
        rule.setIsActive(ruleDto.getIsActive());
        rule.setDescription(ruleDto.getDescription());
        rule.setRuleConditions(ruleDto.getRuleConditions());
        rule.setUpdatedBy(updatedBy);

        return fraudRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public FraudRule getRuleById(Long ruleId) {
        return fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
    }

    @Transactional(readOnly = true)
    public List<FraudRule> getAllRules() {
        return fraudRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FraudRule> getActiveRules() {
        return fraudRuleRepository.findByIsActiveTrue();
    }
}

