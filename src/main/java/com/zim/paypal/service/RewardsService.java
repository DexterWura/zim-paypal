package com.zim.paypal.service;

import com.zim.paypal.model.entity.Rewards;
import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.RewardsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for rewards management operations
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RewardsService {

    private final RewardsRepository rewardsRepository;
    private final UserService userService;

    /**
     * Initialize rewards for user
     * 
     * @param user User entity
     * @return Created rewards
     */
    public Rewards initializeRewards(User user) {
        if (rewardsRepository.findByUser(user).isPresent()) {
            throw new IllegalArgumentException("Rewards already initialized for user: " + user.getUsername());
        }

        Rewards rewards = Rewards.builder()
                .user(user)
                .points(0)
                .cashBackValue(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalRedeemed(BigDecimal.ZERO)
                .lifetimePoints(0)
                .tier(Rewards.RewardsTier.BRONZE)
                .build();

        Rewards savedRewards = rewardsRepository.save(rewards);
        log.info("Rewards initialized for user: {}", user.getUsername());
        return savedRewards;
    }

    /**
     * Get rewards for user
     * 
     * @param userId User ID
     * @return Rewards entity
     */
    @Transactional(readOnly = true)
    public Rewards getRewardsByUserId(Long userId) {
        return rewardsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userService.findById(userId);
                    return initializeRewards(user);
                });
    }

    /**
     * Earn points from transaction
     * 
     * @param userId User ID
     * @param transaction Transaction entity
     */
    public void earnPointsFromTransaction(Long userId, Transaction transaction) {
        Rewards rewards = getRewardsByUserId(userId);
        
        // Only earn points for certain transaction types
        if (transaction.getTransactionType() == Transaction.TransactionType.PAYMENT ||
            transaction.getTransactionType() == Transaction.TransactionType.TRANSFER) {
            
            // Calculate points based on transaction amount
            Integer pointsEarned = rewards.calculatePointsFromAmount(transaction.getAmount());
            
            if (pointsEarned > 0) {
                rewards.addPoints(pointsEarned);
                BigDecimal cashValue = rewards.calculateCashValue(pointsEarned);
                rewards.setTotalEarned(rewards.getTotalEarned().add(cashValue));
                rewardsRepository.save(rewards);
                
                log.info("User {} earned {} points (${}) from transaction {}", 
                        userId, pointsEarned, cashValue, transaction.getTransactionNumber());
            }
        }
    }

    /**
     * Add bonus points
     * 
     * @param userId User ID
     * @param points Points to add
     * @param reason Reason for bonus
     */
    public void addBonusPoints(Long userId, Integer points, String reason) {
        Rewards rewards = getRewardsByUserId(userId);
        rewards.addPoints(points);
        BigDecimal cashValue = rewards.calculateCashValue(points);
        rewards.setTotalEarned(rewards.getTotalEarned().add(cashValue));
        rewardsRepository.save(rewards);
        
        log.info("Added {} bonus points (${}) to user {} - Reason: {}", 
                points, cashValue, userId, reason);
    }

    /**
     * Redeem points for cash
     * 
     * @param userId User ID
     * @param points Points to redeem
     * @return Cash value redeemed
     */
    public BigDecimal redeemPoints(Long userId, Integer points) {
        Rewards rewards = getRewardsByUserId(userId);
        BigDecimal cashValue = rewards.redeemPoints(points);
        rewardsRepository.save(rewards);
        
        log.info("User {} redeemed {} points for ${}", userId, points, cashValue);
        return cashValue;
    }

    /**
     * Get available cash back value
     * 
     * @param userId User ID
     * @return Cash back value
     */
    @Transactional(readOnly = true)
    public BigDecimal getCashBackValue(Long userId) {
        Rewards rewards = getRewardsByUserId(userId);
        return rewards.getCashBackValue();
    }

    /**
     * Get current points
     * 
     * @param userId User ID
     * @return Current points
     */
    @Transactional(readOnly = true)
    public Integer getPoints(Long userId) {
        Rewards rewards = getRewardsByUserId(userId);
        return rewards.getPoints();
    }

    /**
     * Get rewards tier
     * 
     * @param userId User ID
     * @return Rewards tier
     */
    @Transactional(readOnly = true)
    public Rewards.RewardsTier getTier(Long userId) {
        Rewards rewards = getRewardsByUserId(userId);
        return rewards.getTier();
    }
}

