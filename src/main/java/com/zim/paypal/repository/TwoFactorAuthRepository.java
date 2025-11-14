package com.zim.paypal.repository;

import com.zim.paypal.model.entity.TwoFactorAuth;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for TwoFactorAuth entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, Long> {

    /**
     * Find 2FA settings by user
     * 
     * @param user User entity
     * @return Optional TwoFactorAuth
     */
    Optional<TwoFactorAuth> findByUser(User user);

    /**
     * Find 2FA settings by user ID
     * 
     * @param userId User ID
     * @return Optional TwoFactorAuth
     */
    Optional<TwoFactorAuth> findByUserId(Long userId);
}

