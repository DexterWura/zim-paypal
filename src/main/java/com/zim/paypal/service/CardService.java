package com.zim.paypal.service;

import com.zim.paypal.model.entity.Card;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for card management operations
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CardService {

    private final CardRepository cardRepository;
    private final UserService userService;

    /**
     * Link a new card to user
     * 
     * @param userId User ID
     * @param card Card entity
     * @return Saved card
     */
    public Card linkCard(Long userId, Card card) {
        User user = userService.findById(userId);
        card.setUser(user);
        
        // Extract last four digits
        String cardNumber = card.getCardNumber();
        if (cardNumber != null && cardNumber.length() >= 4) {
            card.setLastFourDigits(cardNumber.substring(cardNumber.length() - 4));
        }
        
        // If this is the first card, set it as default
        long activeCardCount = cardRepository.countByUserAndStatus(user, Card.CardStatus.ACTIVE);
        if (activeCardCount == 0) {
            card.setIsDefault(true);
        }
        
        // If setting as default, unset other default cards
        if (card.getIsDefault()) {
            cardRepository.findDefaultCardByUser(user).ifPresent(defaultCard -> {
                defaultCard.setIsDefault(false);
                cardRepository.save(defaultCard);
            });
        }
        
        Card savedCard = cardRepository.save(card);
        log.info("Card linked for user: {}", user.getUsername());
        return savedCard;
    }

    /**
     * Find card by ID
     * 
     * @param cardId Card ID
     * @return Card entity
     * @throws IllegalArgumentException if card not found
     */
    @Transactional(readOnly = true)
    public Card findById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
    }

    /**
     * Find all active cards by user
     * 
     * @param userId User ID
     * @return List of cards
     */
    @Transactional(readOnly = true)
    public List<Card> findActiveCardsByUser(Long userId) {
        User user = userService.findById(userId);
        return cardRepository.findActiveCardsByUser(user);
    }

    /**
     * Find default card by user
     * 
     * @param userId User ID
     * @return Card entity
     */
    @Transactional(readOnly = true)
    public Card findDefaultCardByUser(Long userId) {
        User user = userService.findById(userId);
        return cardRepository.findDefaultCardByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No default card found for user: " + user.getUsername()));
    }

    /**
     * Set card as default
     * 
     * @param userId User ID
     * @param cardId Card ID
     * @return Updated card
     */
    public Card setDefaultCard(Long userId, Long cardId) {
        User user = userService.findById(userId);
        Card card = findById(cardId);
        
        if (!card.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Card does not belong to user");
        }
        
        // Unset current default card
        cardRepository.findDefaultCardByUser(user).ifPresent(defaultCard -> {
            defaultCard.setIsDefault(false);
            cardRepository.save(defaultCard);
        });
        
        // Set new default card
        card.setIsDefault(true);
        Card savedCard = cardRepository.save(card);
        log.info("Default card set for user: {}", user.getUsername());
        return savedCard;
    }

    /**
     * Delete card (soft delete)
     * 
     * @param userId User ID
     * @param cardId Card ID
     */
    public void deleteCard(Long userId, Long cardId) {
        Card card = findById(cardId);
        
        if (!card.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Card does not belong to user");
        }
        
        card.setStatus(Card.CardStatus.DELETED);
        cardRepository.save(card);
        log.info("Card deleted for user: {}", userId);
    }
}

