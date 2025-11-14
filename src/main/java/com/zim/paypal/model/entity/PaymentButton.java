package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentButton entity for merchant payment buttons
 * 
 * @author dexterwura
 */
@Entity
@Table(name = "payment_buttons", indexes = {
    @Index(name = "idx_button_code", columnList = "button_code"),
    @Index(name = "idx_button_merchant", columnList = "merchant_id"),
    @Index(name = "idx_button_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"merchant", "currency"})
@ToString(exclude = {"merchant", "currency"})
public class PaymentButton {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "button_code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Button code is required")
    private String buttonCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @NotNull(message = "Merchant is required")
    private User merchant;

    @Column(name = "button_name", nullable = false, length = 200)
    @NotBlank(message = "Button name is required")
    private String buttonName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "allow_custom_amount", nullable = false)
    @Builder.Default
    private Boolean allowCustomAmount = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "button_style", nullable = false)
    @Builder.Default
    private ButtonStyle buttonStyle = ButtonStyle.DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(name = "button_size", nullable = false)
    @Builder.Default
    private ButtonSize buttonSize = ButtonSize.MEDIUM;

    @Column(name = "button_color", length = 7)
    @Builder.Default
    private String buttonColor = "#0070BA"; // PayPal blue

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "success_url", length = 500)
    private String successUrl;

    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    @Column(name = "notify_url", length = 500)
    private String notifyUrl;

    @Column(name = "total_clicks", nullable = false)
    @Builder.Default
    private Integer totalClicks = 0;

    @Column(name = "total_payments", nullable = false)
    @Builder.Default
    private Integer totalPayments = 0;

    @Column(name = "total_revenue", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for button styles
     */
    public enum ButtonStyle {
        DEFAULT, RECTANGULAR, PILL, MINIMAL
    }

    /**
     * Enumeration for button sizes
     */
    public enum ButtonSize {
        SMALL, MEDIUM, LARGE, RESPONSIVE
    }

    /**
     * Increment click count
     */
    public void incrementClicks() {
        this.totalClicks++;
    }

    /**
     * Record successful payment
     */
    public void recordPayment(BigDecimal amount) {
        this.totalPayments++;
        this.totalRevenue = this.totalRevenue.add(amount);
    }
}

