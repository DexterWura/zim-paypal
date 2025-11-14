package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.FraudRule;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for fraud rule creation/update
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRuleDto {

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    @NotBlank(message = "Rule code is required")
    private String ruleCode;

    @NotNull(message = "Rule type is required")
    private FraudRule.RuleType ruleType;

    @NotNull(message = "Action type is required")
    private FraudRule.ActionType actionType;

    @DecimalMin(value = "0.00", message = "Amount must be non-negative")
    private BigDecimal thresholdAmount;

    private Integer thresholdCount;

    private Integer timeWindowMinutes;

    @DecimalMin(value = "0.00", message = "Risk score threshold must be non-negative")
    private BigDecimal riskScoreThreshold;

    @Builder.Default
    private Boolean isActive = true;

    private String description;

    private String ruleConditions;
}

