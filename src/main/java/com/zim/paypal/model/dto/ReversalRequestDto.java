package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.TransactionReversal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for transaction reversal request
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReversalRequestDto {

    @NotNull(message = "Transaction ID is required")
    private Long transactionId;

    @NotNull(message = "Reversal type is required")
    private TransactionReversal.ReversalType reversalType;

    @NotNull(message = "Reversal amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal reversalAmount;

    @NotBlank(message = "Reason is required")
    private String reason;
}

