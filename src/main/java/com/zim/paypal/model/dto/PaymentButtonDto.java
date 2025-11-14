package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.PaymentButton;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for payment button creation
 * 
 * @author dexterwura
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentButtonDto {

    private String buttonName;

    private String description;

    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    private Boolean allowCustomAmount;

    private Long currencyId;

    private PaymentButton.ButtonStyle buttonStyle;

    private PaymentButton.ButtonSize buttonSize;

    private String buttonColor;

    private String successUrl;

    private String cancelUrl;

    private String notifyUrl;
}

