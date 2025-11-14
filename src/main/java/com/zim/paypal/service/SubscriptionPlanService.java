package com.zim.paypal.service;

import com.zim.paypal.model.entity.Currency;
import com.zim.paypal.model.entity.SubscriptionPlan;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for subscription plan management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final CurrencyService currencyService;

    /**
     * Create a new subscription plan
     * 
     * @param planName Plan name
     * @param planCode Plan code
     * @param description Description
     * @param amount Amount
     * @param currencyId Currency ID
     * @param billingCycle Billing cycle
     * @param trialPeriodDays Trial period days
     * @param createdBy Creator user
     * @return Created plan
     */
    public SubscriptionPlan createPlan(String planName, String planCode, String description,
                                      java.math.BigDecimal amount, Long currencyId,
                                      SubscriptionPlan.BillingCycle billingCycle,
                                      Integer trialPeriodDays, User createdBy) {
        if (planRepository.findByPlanCode(planCode).isPresent()) {
            throw new IllegalArgumentException("Plan code already exists: " + planCode);
        }

        Currency currency = null;
        String currencyCode = "USD";
        if (currencyId != null) {
            currency = currencyService.getCurrencyById(currencyId);
            currencyCode = currency.getCurrencyCode();
        } else {
            currency = currencyService.getBaseCurrency();
            currencyCode = currency.getCurrencyCode();
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planName(planName)
                .planCode(planCode)
                .description(description)
                .amount(amount)
                .currency(currency)
                .currencyCode(currencyCode)
                .billingCycle(billingCycle)
                .trialPeriodDays(trialPeriodDays)
                .isActive(true)
                .createdBy(createdBy)
                .build();

        return planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan getPlanById(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> getAllActivePlans() {
        return planRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> getAllPlans() {
        return planRepository.findAll();
    }
}

