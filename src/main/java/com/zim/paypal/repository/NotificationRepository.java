package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Notification;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Notification entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications by user
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of notifications
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find pending notifications
     * 
     * @return List of pending notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotifications();

    /**
     * Find failed notifications for retry
     * 
     * @param maxRetries Maximum retry count
     * @return List of failed notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetries ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotificationsForRetry(@Param("maxRetries") int maxRetries);
}

