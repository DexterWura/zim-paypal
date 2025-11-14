package com.zim.paypal.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for exchange rate creation/update
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDto {

    @NotNull(message = "From currency is required")
    private Long fromCurrencyId;

    @NotNull(message = "To currency is required")
    private Long toCurrencyId;

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.000001", message = "Rate must be greater than zero")
    private BigDecimal rate;

    @NotNull(message = "Effective from date is required")
    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    @Builder.Default
    private Boolean isActive = true;
}

