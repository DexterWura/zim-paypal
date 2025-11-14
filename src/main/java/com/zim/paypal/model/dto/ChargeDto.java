package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.Charge;
import com.zim.paypal.model.entity.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for charge creation/update
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeDto {

    @NotBlank(message = "Charge name is required")
    private String chargeName;

    @NotBlank(message = "Charge code is required")
    private String chargeCode;

    @NotNull(message = "Charge type is required")
    private Charge.ChargeType chargeType;

    private Transaction.TransactionType transactionType;

    @NotNull(message = "Charge method is required")
    private Charge.ChargeMethod chargeMethod;

    private BigDecimal fixedAmount;

    @DecimalMin(value = "0.00", message = "Percentage rate must be non-negative")
    private BigDecimal percentageRate;

    @DecimalMin(value = "0.00", message = "Min amount must be non-negative")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.00", message = "Max amount must be non-negative")
    private BigDecimal maxAmount;

    @Builder.Default
    private Boolean isActive = true;

    private String description;

    private String regulationReference;
}

