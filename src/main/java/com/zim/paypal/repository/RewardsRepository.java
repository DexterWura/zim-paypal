package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Rewards;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Rewards entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface RewardsRepository extends JpaRepository<Rewards, Long> {

    /**
     * Find rewards by user
     * 
     * @param user User entity
     * @return Optional Rewards
     */
    Optional<Rewards> findByUser(User user);

    /**
     * Find rewards by user ID
     * 
     * @param userId User ID
     * @return Optional Rewards
     */
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Rewards r WHERE r.user.id = :userId")
    Optional<Rewards> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}

