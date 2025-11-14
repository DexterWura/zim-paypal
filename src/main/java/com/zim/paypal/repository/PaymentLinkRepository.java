package com.zim.paypal.repository;

import com.zim.paypal.model.entity.PaymentLink;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for PaymentLink entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {

    /**
     * Find payment link by link code
     * 
     * @param linkCode Link code
     * @return Optional PaymentLink
     */
    Optional<PaymentLink> findByLinkCode(String linkCode);

    /**
     * Find payment links by creator
     * 
     * @param creator Creator user
     * @param pageable Pageable object
     * @return Page of payment links
     */
    Page<PaymentLink> findByCreatorOrderByCreatedAtDesc(User creator, Pageable pageable);

    /**
     * Find active payment links by creator
     * 
     * @param creator Creator user
     * @param pageable Pageable object
     * @return Page of payment links
     */
    Page<PaymentLink> findByCreatorAndStatusOrderByCreatedAtDesc(User creator, 
                                                                  PaymentLink.Status status, 
                                                                  Pageable pageable);
}

