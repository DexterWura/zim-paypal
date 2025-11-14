package com.zim.paypal.repository;

import com.zim.paypal.model.entity.SupportTicket;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SupportTicket entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    /**
     * Find ticket by ticket number
     * 
     * @param ticketNumber Ticket number
     * @return Optional SupportTicket
     */
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    /**
     * Find all tickets by user
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of tickets
     */
    Page<SupportTicket> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find all tickets assigned to admin
     * 
     * @param assignedTo Assigned admin user
     * @param pageable Pageable object
     * @return Page of tickets
     */
    Page<SupportTicket> findByAssignedToOrderByCreatedAtDesc(User assignedTo, Pageable pageable);

    /**
     * Find open tickets
     * 
     * @param pageable Pageable object
     * @return Page of open tickets
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.status IN ('OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER') ORDER BY t.priority DESC, t.createdAt DESC")
    Page<SupportTicket> findOpenTickets(Pageable pageable);

    /**
     * Find tickets by status
     * 
     * @param status Ticket status
     * @param pageable Pageable object
     * @return Page of tickets
     */
    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicket.TicketStatus status, Pageable pageable);

    /**
     * Count tickets by status
     * 
     * @param status Ticket status
     * @return Count of tickets
     */
    long countByStatus(SupportTicket.TicketStatus status);

    /**
     * Find tickets by priority
     * 
     * @param priority Ticket priority
     * @return List of tickets
     */
    List<SupportTicket> findByPriorityAndStatusIn(SupportTicket.TicketPriority priority, 
                                                   List<SupportTicket.TicketStatus> statuses);
}

