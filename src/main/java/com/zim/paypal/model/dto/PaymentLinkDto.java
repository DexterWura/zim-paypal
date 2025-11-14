package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.PaymentLink;
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
 * DTO for payment link creation
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkDto {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    private Long currencyId;

    @Builder.Default
    private PaymentLink.LinkType linkType = PaymentLink.LinkType.ONE_TIME;

    private Integer maxUses;

    private LocalDateTime expiresAt;

    @Builder.Default
    private Boolean allowPartialPayment = false;

    @Builder.Default
    private Boolean collectShippingAddress = false;

    @Builder.Default
    private Boolean collectPhoneNumber = false;

    @Builder.Default
    private Boolean emailNotification = true;

    private String returnUrl;

    private String cancelUrl;

    private String imageUrl;
}

