package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Account;
import com.zim.paypal.model.entity.Transaction;
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
 * Repository interface for Transaction entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find transaction by transaction number
     * 
     * @param transactionNumber Transaction number
     * @return Optional Transaction
     */
    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    /**
     * Find all transactions by sender
     * 
     * @param sender Sender user
     * @param pageable Pageable object
     * @return Page of transactions
     */
    Page<Transaction> findBySenderOrderByCreatedAtDesc(User sender, Pageable pageable);

    /**
     * Find all transactions by receiver
     * 
     * @param receiver Receiver user
     * @param pageable Pageable object
     * @return Page of transactions
     */
    Page<Transaction> findByReceiverOrderByCreatedAtDesc(User receiver, Pageable pageable);

    /**
     * Find all transactions by account
     * 
     * @param account Account entity
     * @param pageable Pageable object
     * @return Page of transactions
     */
    Page<Transaction> findByAccountOrderByCreatedAtDesc(Account account, Pageable pageable);

    /**
     * Find transactions by user (sender or receiver)
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.sender = :user OR t.receiver = :user ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    /**
     * Find transactions by user and date range
     * 
     * @param user User entity
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pageable object
     * @return Page of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE (t.sender = :user OR t.receiver = :user) " +
           "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserAndDateRange(@Param("user") User user, 
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              Pageable pageable);

    /**
     * Find transactions by account and date range
     * 
     * @param account Account entity
     * @param startDate Start date
     * @param endDate End date
     * @return List of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.account = :account " +
           "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountAndDateRange(@Param("account") Account account,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
}

