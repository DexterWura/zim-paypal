package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rewards entity representing user rewards points and cashback
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "rewards", indexes = {
    @Index(name = "idx_rewards_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = "user")
public class Rewards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @NotNull(message = "User is required")
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;

    @Column(name = "cash_back_value", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal cashBackValue = BigDecimal.ZERO;

    @Column(name = "total_earned", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_redeemed", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalRedeemed = BigDecimal.ZERO;

    @Column(name = "lifetime_points", nullable = false)
    @Builder.Default
    private Integer lifetimePoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    @Builder.Default
    private RewardsTier tier = RewardsTier.BRONZE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration for rewards tiers
     */
    public enum RewardsTier {
        BRONZE(1.0), SILVER(1.5), GOLD(2.0), PLATINUM(2.5);

        private final double multiplier;

        RewardsTier(double multiplier) {
            this.multiplier = multiplier;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }

    /**
     * Add points to rewards
     * 
     * @param points Points to add
     */
    public void addPoints(Integer points) {
        if (points != null && points > 0) {
            this.points += points;
            this.lifetimePoints += points;
            updateCashBackValue();
            updateTier();
        }
    }

    /**
     * Redeem points
     * 
     * @param points Points to redeem
     * @return Cash value of redeemed points
     * @throws IllegalArgumentException if insufficient points
     */
    public BigDecimal redeemPoints(Integer points) {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        if (this.points < points) {
            throw new IllegalArgumentException("Insufficient points");
        }
        
        BigDecimal cashValue = calculateCashValue(points);
        this.points -= points;
        this.totalRedeemed = this.totalRedeemed.add(cashValue);
        updateCashBackValue();
        
        return cashValue;
    }

    /**
     * Calculate cash value of points
     * 
     * @param points Points to calculate
     * @return Cash value (100 points = $1.00)
     */
    public BigDecimal calculateCashValue(Integer points) {
        if (points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(points).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Update cash back value based on current points
     */
    private void updateCashBackValue() {
        this.cashBackValue = calculateCashValue(this.points);
    }

    /**
     * Update tier based on lifetime points
     */
    private void updateTier() {
        if (this.lifetimePoints >= 100000) {
            this.tier = RewardsTier.PLATINUM;
        } else if (this.lifetimePoints >= 50000) {
            this.tier = RewardsTier.GOLD;
        } else if (this.lifetimePoints >= 10000) {
            this.tier = RewardsTier.SILVER;
        } else {
            this.tier = RewardsTier.BRONZE;
        }
    }

    /**
     * Calculate points earned from transaction amount
     * 
     * @param amount Transaction amount
     * @return Points earned
     */
    public Integer calculatePointsFromAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        // 1 point per dollar spent, multiplied by tier multiplier
        double points = amount.doubleValue() * this.tier.getMultiplier();
        return (int) Math.floor(points);
    }
}

