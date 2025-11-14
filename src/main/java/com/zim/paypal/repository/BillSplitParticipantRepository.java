package com.zim.paypal.repository;

import com.zim.paypal.model.entity.BillSplit;
import com.zim.paypal.model.entity.BillSplitParticipant;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BillSplitParticipant entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface BillSplitParticipantRepository extends JpaRepository<BillSplitParticipant, Long> {

    /**
     * Find all participants for a bill split
     * 
     * @param billSplit BillSplit entity
     * @return List of participants
     */
    List<BillSplitParticipant> findByBillSplit(BillSplit billSplit);

    /**
     * Find participant by split and user
     * 
     * @param billSplit BillSplit entity
     * @param user User entity
     * @return Optional participant
     */
    Optional<BillSplitParticipant> findByBillSplitAndUser(BillSplit billSplit, User user);

    /**
     * Find pending participants for user
     * 
     * @param user User entity
     * @return List of pending participants
     */
    @Query("SELECT p FROM BillSplitParticipant p WHERE p.user = :user AND p.paymentStatus = 'PENDING' ORDER BY p.createdAt DESC")
    List<BillSplitParticipant> findPendingParticipantsByUser(@Param("user") User user);

    /**
     * Count pending participants for a split
     * 
     * @param billSplit BillSplit entity
     * @return Count of pending participants
     */
    long countByBillSplitAndPaymentStatus(BillSplit billSplit, BillSplitParticipant.PaymentStatus status);
}

