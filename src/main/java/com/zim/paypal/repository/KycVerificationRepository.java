package com.zim.paypal.repository;

import com.zim.paypal.model.entity.KycVerification;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for KycVerification entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    /**
     * Find verification by user
     * 
     * @param user User entity
     * @return Optional KycVerification
     */
    Optional<KycVerification> findByUser(User user);

    /**
     * Find latest verification by user
     * 
     * @param user User entity
     * @return Optional KycVerification
     */
    @Query("SELECT kv FROM KycVerification kv WHERE kv.user = :user ORDER BY kv.createdAt DESC")
    Optional<KycVerification> findLatestByUser(@Param("user") User user);

    /**
     * Find verifications by status
     * 
     * @param status Verification status
     * @return List of verifications
     */
    List<KycVerification> findByVerificationStatusOrderByCreatedAtDesc(KycVerification.VerificationStatus status);

    /**
     * Find verifications by level
     * 
     * @param level Verification level
     * @return List of verifications
     */
    List<KycVerification> findByVerificationLevelOrderByCreatedAtDesc(KycVerification.VerificationLevel level);

    /**
     * Find expired verifications
     * 
     * @param now Current date time
     * @return List of verifications
     */
    @Query("SELECT kv FROM KycVerification kv WHERE kv.expiresAt IS NOT NULL AND kv.expiresAt < :now AND kv.verificationStatus = 'APPROVED'")
    List<KycVerification> findExpiredVerifications(@Param("now") LocalDateTime now);
}

