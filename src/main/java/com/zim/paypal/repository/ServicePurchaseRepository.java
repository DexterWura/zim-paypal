package com.zim.paypal.repository;

import com.zim.paypal.model.entity.ServicePurchase;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ServicePurchase entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface ServicePurchaseRepository extends JpaRepository<ServicePurchase, Long> {

    /**
     * Find purchase by reference number
     * 
     * @param referenceNumber Reference number
     * @return Optional ServicePurchase
     */
    Optional<ServicePurchase> findByReferenceNumber(String referenceNumber);

    /**
     * Find all purchases by user
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of purchases
     */
    Page<ServicePurchase> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find purchases by status
     * 
     * @param status Purchase status
     * @param pageable Pageable object
     * @return Page of purchases
     */
    Page<ServicePurchase> findByStatusOrderByCreatedAtDesc(ServicePurchase.PurchaseStatus status, Pageable pageable);

    /**
     * Count purchases by status
     * 
     * @param status Purchase status
     * @return Count of purchases
     */
    long countByStatus(ServicePurchase.PurchaseStatus status);
}

