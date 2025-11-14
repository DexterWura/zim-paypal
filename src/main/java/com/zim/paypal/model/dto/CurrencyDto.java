package com.zim.paypal.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for currency creation/update
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyDto {

    @NotBlank(message = "Currency code is required")
    private String currencyCode;

    @NotBlank(message = "Currency name is required")
    private String currencyName;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isBaseCurrency = false;

    @Builder.Default
    private Integer decimalPlaces = 2;
}

