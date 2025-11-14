package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.AccountLimit;
import com.zim.paypal.model.entity.User;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for account limit creation/update
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLimitDto {

    @NotBlank(message = "Limit name is required")
    private String limitName;

    @NotBlank(message = "Limit code is required")
    private String limitCode;

    @NotNull(message = "Limit type is required")
    private AccountLimit.LimitType limitType;

    private AccountLimit.PeriodType periodType;

    private Integer maxAccountsPerUser;

    @DecimalMin(value = "0.00", message = "Amount must be non-negative")
    private BigDecimal maxTransactionAmount;

    @DecimalMin(value = "0.00", message = "Amount must be non-negative")
    private BigDecimal maxDailyAmount;

    @DecimalMin(value = "0.00", message = "Amount must be non-negative")
    private BigDecimal maxWeeklyAmount;

    @DecimalMin(value = "0.00", message = "Amount must be non-negative")
    private BigDecimal maxMonthlyAmount;

    private Integer maxDailyCount;

    private Integer maxWeeklyCount;

    private Integer maxMonthlyCount;

    private User.UserRole userRole;

    @Builder.Default
    private Boolean isActive = true;

    private String description;
}

