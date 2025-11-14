package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Account;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Account entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find account by account number
     * 
     * @param accountNumber Account number
     * @return Optional Account
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find all accounts by user
     * 
     * @param user User entity
     * @return List of accounts
     */
    List<Account> findByUser(User user);

    /**
     * Find active account by user
     * 
     * @param user User entity
     * @return Optional Account
     */
    @Query("SELECT a FROM Account a WHERE a.user = :user AND a.status = 'ACTIVE' ORDER BY a.createdAt ASC")
    Optional<Account> findActiveAccountByUser(@Param("user") User user);

    /**
     * Check if account number exists
     * 
     * @param accountNumber Account number
     * @return true if exists
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Count accounts by user ID
     * 
     * @param userId User ID
     * @return Count of accounts
     */
    @Query("SELECT COUNT(a) FROM Account a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}

