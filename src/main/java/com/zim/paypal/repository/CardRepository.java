package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Card;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Card entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Find all cards by user
     * 
     * @param user User entity
     * @return List of cards
     */
    List<Card> findByUser(User user);

    /**
     * Find active cards by user
     * 
     * @param user User entity
     * @return List of active cards
     */
    @Query("SELECT c FROM Card c WHERE c.user = :user AND c.status = 'ACTIVE' ORDER BY c.isDefault DESC, c.createdAt ASC")
    List<Card> findActiveCardsByUser(@Param("user") User user);

    /**
     * Find default card by user
     * 
     * @param user User entity
     * @return Optional Card
     */
    @Query("SELECT c FROM Card c WHERE c.user = :user AND c.isDefault = true AND c.status = 'ACTIVE'")
    Optional<Card> findDefaultCardByUser(@Param("user") User user);

    /**
     * Count active cards by user
     * 
     * @param user User entity
     * @return Count of active cards
     */
    long countByUserAndStatus(User user, Card.CardStatus status);
}

