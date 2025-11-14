package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.SubscriptionPlan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for subscription creation
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {

    @NotNull(message = "Merchant ID is required")
    private Long merchantId;

    private Long planId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @NotNull(message = "Billing cycle is required")
    private SubscriptionPlan.BillingCycle billingCycle;

    private Long accountId;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer trialPeriodDays;

    @Builder.Default
    private Boolean autoRenew = true;

    private String description;
}

