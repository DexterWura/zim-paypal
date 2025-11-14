package com.zim.paypal.repository;

import com.zim.paypal.model.entity.BillSplit;
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
 * Repository interface for BillSplit entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface BillSplitRepository extends JpaRepository<BillSplit, Long> {

    /**
     * Find split by split number
     * 
     * @param splitNumber Split number
     * @return Optional BillSplit
     */
    Optional<BillSplit> findBySplitNumber(String splitNumber);

    /**
     * Find all splits created by user
     * 
     * @param creator Creator user
     * @param pageable Pageable object
     * @return Page of splits
     */
    Page<BillSplit> findByCreatorOrderByCreatedAtDesc(User creator, Pageable pageable);

    /**
     * Find splits where user is a participant
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of splits
     */
    @Query("SELECT DISTINCT bs FROM BillSplit bs JOIN bs.participants p WHERE p.user = :user ORDER BY bs.createdAt DESC")
    Page<BillSplit> findSplitsByParticipant(@Param("user") User user, Pageable pageable);

    /**
     * Find pending splits
     * 
     * @param pageable Pageable object
     * @return Page of pending splits
     */
    @Query("SELECT bs FROM BillSplit bs WHERE bs.status IN ('PENDING', 'PARTIALLY_PAID') ORDER BY bs.createdAt DESC")
    Page<BillSplit> findPendingSplits(Pageable pageable);

    /**
     * Find expired splits
     * 
     * @param now Current date time
     * @return List of expired splits
     */
    @Query("SELECT bs FROM BillSplit bs WHERE bs.status = 'PENDING' AND bs.expiresAt IS NOT NULL AND bs.expiresAt < :now")
    List<BillSplit> findExpiredSplits(@Param("now") LocalDateTime now);
}

