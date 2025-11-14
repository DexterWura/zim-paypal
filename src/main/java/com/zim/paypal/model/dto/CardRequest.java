package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.Card;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for card linking request
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13-19 digits")
    private String cardNumber;

    @NotBlank(message = "Cardholder name is required")
    @Size(max = 100, message = "Cardholder name must not exceed 100 characters")
    private String cardholderName;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3-4 digits")
    private String cvv;

    @NotNull(message = "Card type is required")
    private Card.CardType cardType;

    @NotNull(message = "Card brand is required")
    private Card.CardBrand cardBrand;

    private Boolean isDefault;

    private String billingAddress;
    private String billingCity;
    private String billingState;
    private String billingZip;
    private String billingCountry;
}

