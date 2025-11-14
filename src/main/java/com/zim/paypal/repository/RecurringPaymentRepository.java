package com.zim.paypal.repository;

import com.zim.paypal.model.entity.RecurringPayment;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RecurringPayment entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {

    /**
     * Find subscription by subscription ID
     * 
     * @param subscriptionId Subscription ID
     * @return Optional RecurringPayment
     */
    Optional<RecurringPayment> findBySubscriptionId(String subscriptionId);

    /**
     * Find subscriptions by subscriber
     * 
     * @param subscriber Subscriber user
     * @return List of subscriptions
     */
    List<RecurringPayment> findBySubscriberOrderByCreatedAtDesc(User subscriber);

    /**
     * Find subscriptions by merchant
     * 
     * @param merchant Merchant user
     * @return List of subscriptions
     */
    List<RecurringPayment> findByMerchantOrderByCreatedAtDesc(User merchant);

    /**
     * Find active subscriptions due for payment
     * 
     * @param now Current date time
     * @return List of subscriptions
     */
    @Query("SELECT rp FROM RecurringPayment rp WHERE rp.status = 'ACTIVE' " +
           "AND rp.nextPaymentDate <= :now AND rp.autoRenew = true " +
           "AND (rp.endDate IS NULL OR rp.endDate >= :now)")
    List<RecurringPayment> findDueSubscriptions(@Param("now") LocalDateTime now);

    /**
     * Find subscriptions by status
     * 
     * @param status Subscription status
     * @return List of subscriptions
     */
    List<RecurringPayment> findByStatusOrderByCreatedAtDesc(RecurringPayment.Status status);
}

