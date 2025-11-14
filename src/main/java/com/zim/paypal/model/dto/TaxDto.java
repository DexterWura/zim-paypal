package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.Tax;
import com.zim.paypal.model.entity.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for tax creation/update
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDto {

    @NotBlank(message = "Tax name is required")
    private String taxName;

    @NotBlank(message = "Tax code is required")
    private String taxCode;

    @NotNull(message = "Tax type is required")
    private Tax.TaxType taxType;

    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.00", message = "Tax rate must be non-negative")
    private BigDecimal taxRate;

    private Transaction.TransactionType transactionType;

    @Builder.Default
    private Boolean isActive = true;

    private String description;

    private String regulationReference;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
}

