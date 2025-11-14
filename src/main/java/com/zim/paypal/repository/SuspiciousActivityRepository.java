package com.zim.paypal.repository;

import com.zim.paypal.model.entity.SuspiciousActivity;
import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for SuspiciousActivity entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivity, Long> {

    /**
     * Find suspicious activities by user
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of activities
     */
    Page<SuspiciousActivity> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find suspicious activities by transaction
     * 
     * @param transaction Transaction entity
     * @return List of activities
     */
    List<SuspiciousActivity> findByTransaction(Transaction transaction);

    /**
     * Find pending activities
     * 
     * @param pageable Pageable object
     * @return Page of activities
     */
    Page<SuspiciousActivity> findByStatusOrderByCreatedAtDesc(SuspiciousActivity.Status status, Pageable pageable);

    /**
     * Find activities by severity
     * 
     * @param severity Severity level
     * @param pageable Pageable object
     * @return Page of activities
     */
    Page<SuspiciousActivity> findBySeverityOrderByCreatedAtDesc(SuspiciousActivity.Severity severity, Pageable pageable);

    /**
     * Count pending activities
     * 
     * @return Count
     */
    long countByStatus(SuspiciousActivity.Status status);

    /**
     * Find activities created after date
     * 
     * @param date Date
     * @return List of activities
     */
    List<SuspiciousActivity> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);
}

