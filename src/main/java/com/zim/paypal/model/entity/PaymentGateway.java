package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Payment Gateway entity
 * 
 * @author dexterwura
 */
@Entity
@Table(name = "payment_gateways")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gateway_name", nullable = false, unique = true, length = 50)
    private String gatewayName;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", nullable = false, length = 50)
    private GatewayType gatewayType;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "api_secret", length = 500)
    private String apiSecret;

    @Column(name = "merchant_id", length = 200)
    private String merchantId;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "additional_config", columnDefinition = "TEXT")
    private String additionalConfig;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum GatewayType {
        MOBILE_MONEY,
        ONLINE,
        BANK_TRANSFER,
        CRYPTO
    }
}

