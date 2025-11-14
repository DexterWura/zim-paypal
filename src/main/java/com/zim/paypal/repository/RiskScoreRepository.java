package com.zim.paypal.repository;

import com.zim.paypal.model.entity.RiskScore;
import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RiskScore entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {

    /**
     * Find risk scores by user
     * 
     * @param user User entity
     * @return List of risk scores
     */
    List<RiskScore> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find risk score by transaction
     * 
     * @param transaction Transaction entity
     * @return Optional RiskScore
     */
    Optional<RiskScore> findByTransaction(Transaction transaction);

    /**
     * Find latest risk score for user
     * 
     * @param user User entity
     * @return Optional RiskScore
     */
    @Query("SELECT rs FROM RiskScore rs WHERE rs.user = :user AND rs.scoreType = 'USER_PROFILE' ORDER BY rs.createdAt DESC")
    Optional<RiskScore> findLatestUserRiskScore(@Param("user") User user);

    /**
     * Find high risk scores
     * 
     * @param riskLevel Risk level
     * @param startDate Start date
     * @return List of risk scores
     */
    @Query("SELECT rs FROM RiskScore rs WHERE rs.riskLevel = :riskLevel AND rs.createdAt >= :startDate ORDER BY rs.createdAt DESC")
    List<RiskScore> findHighRiskScores(@Param("riskLevel") RiskScore.RiskLevel riskLevel,
                                       @Param("startDate") LocalDateTime startDate);
}

