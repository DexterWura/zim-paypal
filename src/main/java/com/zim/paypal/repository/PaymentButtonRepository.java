package com.zim.paypal.repository;

import com.zim.paypal.model.entity.PaymentButton;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PaymentButton entity
 * 
 * @author dexterwura
 */
@Repository
public interface PaymentButtonRepository extends JpaRepository<PaymentButton, Long> {

    /**
     * Find button by button code
     * 
     * @param buttonCode Button code
     * @return Optional PaymentButton
     */
    Optional<PaymentButton> findByButtonCode(String buttonCode);

    /**
     * Find buttons by merchant
     * 
     * @param merchant Merchant user
     * @return List of payment buttons
     */
    List<PaymentButton> findByMerchantOrderByCreatedAtDesc(User merchant);

    /**
     * Find active buttons by merchant
     * 
     * @param merchant Merchant user
     * @return List of active payment buttons
     */
    List<PaymentButton> findByMerchantAndIsActiveTrueOrderByCreatedAtDesc(User merchant);
}

