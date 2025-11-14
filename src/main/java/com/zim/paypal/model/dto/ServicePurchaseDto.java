package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.ServicePurchase;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for service purchase
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePurchaseDto {

    @NotNull(message = "Service provider is required")
    private Long providerId;

    @NotNull(message = "Service type is required")
    private ServicePurchase.ServiceType serviceType;

    @NotBlank(message = "Recipient number is required")
    private String recipientNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    // For data bundles - bundle size in MB/GB
    private String bundleSize;

    // For ZESA tokens - meter number
    private String meterNumber;
}

