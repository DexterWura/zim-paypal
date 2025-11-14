package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Card entity representing a linked payment card
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_user", columnList = "user_id"),
    @Index(name = "idx_card_last_four", columnList = "last_four_digits")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = {"cardNumber", "cvv", "user"})
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "card_number", nullable = false, length = 19)
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13-19 digits")
    private String cardNumber;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @Column(name = "cardholder_name", nullable = false, length = 100)
    @NotBlank(message = "Cardholder name is required")
    @Size(max = 100, message = "Cardholder name must not exceed 100 characters")
    private String cardholderName;

    @Column(name = "expiry_date", nullable = false)
    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @Column(name = "cvv", nullable = false, length = 4)
    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3-4 digits")
    private String cvv;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    @NotNull(message = "Card type is required")
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand", nullable = false)
    @NotNull(message = "Card brand is required")
    private CardBrand cardBrand;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "billing_address", length = 255)
    @Size(max = 255, message = "Billing address must not exceed 255 characters")
    private String billingAddress;

    @Column(name = "billing_city", length = 100)
    @Size(max = 100, message = "Billing city must not exceed 100 characters")
    private String billingCity;

    @Column(name = "billing_state", length = 50)
    @Size(max = 50, message = "Billing state must not exceed 50 characters")
    private String billingState;

    @Column(name = "billing_zip", length = 20)
    @Size(max = 20, message = "Billing zip must not exceed 20 characters")
    private String billingZip;

    @Column(name = "billing_country", length = 50)
    @Size(max = 50, message = "Billing country must not exceed 50 characters")
    private String billingCountry;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for card types
     */
    public enum CardType {
        DEBIT, CREDIT, PREPAID
    }

    /**
     * Enumeration for card brands
     */
    public enum CardBrand {
        VISA, MASTERCARD, AMEX, DISCOVER
    }

    /**
     * Enumeration for card status
     */
    public enum CardStatus {
        ACTIVE, EXPIRED, SUSPENDED, DELETED
    }

    /**
     * Check if card is expired
     * 
     * @return true if card is expired
     */
    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Check if card is active and valid
     * 
     * @return true if card is active and not expired
     */
    public boolean isValid() {
        return status == CardStatus.ACTIVE && !isExpired();
    }

    /**
     * Mask card number for display
     * 
     * @return Masked card number
     */
    public String getMaskedCardNumber() {
        if (lastFourDigits == null || lastFourDigits.length() != 4) {
            return "****";
        }
        return "****" + lastFourDigits;
    }
}

