package com.zim.paypal.repository;

import com.zim.paypal.model.entity.SupportTicket;
import com.zim.paypal.model.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for TicketMessage entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    /**
     * Find all messages for a ticket
     * 
     * @param ticket SupportTicket entity
     * @return List of messages ordered by creation date
     */
    @Query("SELECT m FROM TicketMessage m WHERE m.ticket = :ticket ORDER BY m.createdAt ASC")
    List<TicketMessage> findByTicketOrderByCreatedAtAsc(@Param("ticket") SupportTicket ticket);

    /**
     * Find public messages for a ticket (non-internal)
     * 
     * @param ticket SupportTicket entity
     * @return List of public messages
     */
    @Query("SELECT m FROM TicketMessage m WHERE m.ticket = :ticket AND m.isInternal = false ORDER BY m.createdAt ASC")
    List<TicketMessage> findPublicMessagesByTicket(@Param("ticket") SupportTicket ticket);
}

