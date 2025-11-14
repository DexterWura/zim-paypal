package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.model.entity.TransactionReversal;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TransactionReversal entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface TransactionReversalRepository extends JpaRepository<TransactionReversal, Long> {

    /**
     * Find reversal by reversal number
     * 
     * @param reversalNumber Reversal number
     * @return Optional TransactionReversal
     */
    Optional<TransactionReversal> findByReversalNumber(String reversalNumber);

    /**
     * Find reversal by transaction
     * 
     * @param transaction Transaction entity
     * @return Optional TransactionReversal
     */
    Optional<TransactionReversal> findByTransaction(Transaction transaction);

    /**
     * Find all reversals requested by user
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of reversals
     */
    Page<TransactionReversal> findByRequestedByOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find pending reversals
     * 
     * @param pageable Pageable object
     * @return Page of pending reversals
     */
    @Query("SELECT r FROM TransactionReversal r WHERE r.status = 'PENDING' ORDER BY r.createdAt DESC")
    Page<TransactionReversal> findPendingReversals(Pageable pageable);

    /**
     * Find reversals by status
     * 
     * @param status Reversal status
     * @param pageable Pageable object
     * @return Page of reversals
     */
    Page<TransactionReversal> findByStatusOrderByCreatedAtDesc(TransactionReversal.ReversalStatus status, Pageable pageable);

    /**
     * Count reversals by status
     * 
     * @param status Reversal status
     * @return Count of reversals
     */
    long countByStatus(TransactionReversal.ReversalStatus status);
}

