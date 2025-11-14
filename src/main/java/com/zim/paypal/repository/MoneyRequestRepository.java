package com.zim.paypal.repository;

import com.zim.paypal.model.entity.MoneyRequest;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MoneyRequest entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, Long> {

    /**
     * Find request by request number
     * 
     * @param requestNumber Request number
     * @return Optional MoneyRequest
     */
    Optional<MoneyRequest> findByRequestNumber(String requestNumber);

    /**
     * Find all requests where user is the requester
     * 
     * @param requester Requester user
     * @param pageable Pageable object
     * @return Page of requests
     */
    Page<MoneyRequest> findByRequesterOrderByCreatedAtDesc(User requester, Pageable pageable);

    /**
     * Find all requests where user is the recipient
     * 
     * @param recipient Recipient user
     * @param pageable Pageable object
     * @return Page of requests
     */
    Page<MoneyRequest> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    /**
     * Find pending requests for recipient
     * 
     * @param recipient Recipient user
     * @return List of pending requests
     */
    @Query("SELECT mr FROM MoneyRequest mr WHERE mr.recipient = :recipient " +
           "AND mr.status = 'PENDING' ORDER BY mr.createdAt DESC")
    List<MoneyRequest> findPendingRequestsByRecipient(@Param("recipient") User recipient);

    /**
     * Find pending requests for requester
     * 
     * @param requester Requester user
     * @return List of pending requests
     */
    @Query("SELECT mr FROM MoneyRequest mr WHERE mr.requester = :requester " +
           "AND mr.status = 'PENDING' ORDER BY mr.createdAt DESC")
    List<MoneyRequest> findPendingRequestsByRequester(@Param("requester") User requester);

    /**
     * Find expired requests
     * 
     * @param now Current date time
     * @return List of expired requests
     */
    @Query("SELECT mr FROM MoneyRequest mr WHERE mr.status = 'PENDING' " +
           "AND mr.expiresAt IS NOT NULL AND mr.expiresAt < :now")
    List<MoneyRequest> findExpiredRequests(@Param("now") LocalDateTime now);

    /**
     * Count pending requests for recipient
     * 
     * @param recipient Recipient user
     * @return Count of pending requests
     */
    long countByRecipientAndStatus(User recipient, MoneyRequest.RequestStatus status);
}

