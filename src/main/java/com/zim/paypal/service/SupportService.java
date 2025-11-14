package com.zim.paypal.service;

import com.zim.paypal.model.dto.SupportTicketDto;
import com.zim.paypal.model.dto.TicketMessageDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.SupportTicketRepository;
import com.zim.paypal.repository.TicketMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for support ticket management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * Create a new support ticket
     * 
     * @param userId User ID
     * @param ticketDto Ticket DTO
     * @return Created ticket
     */
    public SupportTicket createTicket(Long userId, SupportTicketDto ticketDto) {
        User user = userService.findById(userId);
        
        String ticketNumber = generateTicketNumber();
        
        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(ticketNumber)
                .user(user)
                .subject(ticketDto.getSubject())
                .description(ticketDto.getDescription())
                .category(ticketDto.getCategory())
                .priority(ticketDto.getPriority() != null ? ticketDto.getPriority() : SupportTicket.TicketPriority.MEDIUM)
                .status(SupportTicket.TicketStatus.OPEN)
                .build();
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        
        // Send notification to admins (in a real system)
        log.info("Support ticket created: {} by user: {}", ticketNumber, user.getUsername());
        
        return savedTicket;
    }

    /**
     * Get ticket by ticket number
     * 
     * @param ticketNumber Ticket number
     * @return SupportTicket entity
     */
    @Transactional(readOnly = true)
    public SupportTicket getTicketByNumber(String ticketNumber) {
        return supportTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketNumber));
    }

    /**
     * Get ticket by ID
     * 
     * @param ticketId Ticket ID
     * @return SupportTicket entity
     */
    @Transactional(readOnly = true)
    public SupportTicket getTicketById(Long ticketId) {
        return supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
    }

    /**
     * Get tickets for user
     * 
     * @param userId User ID
     * @param pageable Pageable object
     * @return Page of tickets
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> getTicketsByUser(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return supportTicketRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get all open tickets (for admin)
     * 
     * @param pageable Pageable object
     * @return Page of tickets
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> getOpenTickets(Pageable pageable) {
        return supportTicketRepository.findOpenTickets(pageable);
    }

    /**
     * Get all tickets (for admin)
     * 
     * @param pageable Pageable object
     * @return Page of tickets
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> getAllTickets(Pageable pageable) {
        return supportTicketRepository.findAll(pageable);
    }

    /**
     * Get tickets by status
     * 
     * @param status Ticket status
     * @param pageable Pageable object
     * @return Page of tickets
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> getTicketsByStatus(SupportTicket.TicketStatus status, Pageable pageable) {
        return supportTicketRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    /**
     * Assign ticket to admin
     * 
     * @param ticketId Ticket ID
     * @param adminId Admin user ID
     */
    public void assignTicket(Long ticketId, Long adminId) {
        SupportTicket ticket = getTicketById(ticketId);
        User admin = userService.findById(adminId);
        
        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new IllegalArgumentException("User is not an admin");
        }
        
        ticket.setAssignedTo(admin);
        ticket.setStatus(SupportTicket.TicketStatus.IN_PROGRESS);
        supportTicketRepository.save(ticket);
        
        log.info("Ticket {} assigned to admin: {}", ticket.getTicketNumber(), admin.getUsername());
    }

    /**
     * Add message to ticket
     * 
     * @param ticketId Ticket ID
     * @param senderId Sender user ID
     * @param messageDto Message DTO
     * @return Created message
     */
    public TicketMessage addMessage(Long ticketId, Long senderId, TicketMessageDto messageDto) {
        SupportTicket ticket = getTicketById(ticketId);
        User sender = userService.findById(senderId);
        
        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .sender(sender)
                .message(messageDto.getMessage())
                .isInternal(messageDto.getIsInternal() != null ? messageDto.getIsInternal() : false)
                .build();
        
        TicketMessage savedMessage = ticketMessageRepository.save(message);
        
        // Update ticket last response time
        ticket.updateLastResponse();
        supportTicketRepository.save(ticket);
        
        log.info("Message added to ticket: {}", ticket.getTicketNumber());
        return savedMessage;
    }

    /**
     * Get messages for ticket
     * 
     * @param ticketId Ticket ID
     * @param includeInternal Include internal messages (for admin)
     * @return List of messages
     */
    @Transactional(readOnly = true)
    public List<TicketMessage> getTicketMessages(Long ticketId, boolean includeInternal) {
        SupportTicket ticket = getTicketById(ticketId);
        if (includeInternal) {
            return ticketMessageRepository.findByTicketOrderByCreatedAtAsc(ticket);
        } else {
            return ticketMessageRepository.findPublicMessagesByTicket(ticket);
        }
    }

    /**
     * Update ticket status
     * 
     * @param ticketId Ticket ID
     * @param status New status
     */
    public void updateTicketStatus(Long ticketId, SupportTicket.TicketStatus status) {
        SupportTicket ticket = getTicketById(ticketId);
        ticket.setStatus(status);
        if (status == SupportTicket.TicketStatus.RESOLVED || status == SupportTicket.TicketStatus.CLOSED) {
            ticket.setClosedAt(java.time.LocalDateTime.now());
        }
        supportTicketRepository.save(ticket);
        
        log.info("Ticket {} status updated to {}", ticket.getTicketNumber(), status);
    }

    /**
     * Resolve ticket
     * 
     * @param ticketId Ticket ID
     * @param resolution Resolution text
     */
    public void resolveTicket(Long ticketId, String resolution) {
        SupportTicket ticket = getTicketById(ticketId);
        ticket.markAsResolved(resolution);
        supportTicketRepository.save(ticket);
        
        // Send notification to user
        notificationService.sendTicketResolvedNotification(ticket);
        
        log.info("Ticket {} resolved", ticket.getTicketNumber());
    }

    /**
     * Close ticket
     * 
     * @param ticketId Ticket ID
     */
    public void closeTicket(Long ticketId) {
        SupportTicket ticket = getTicketById(ticketId);
        ticket.markAsClosed();
        supportTicketRepository.save(ticket);
        
        log.info("Ticket {} closed", ticket.getTicketNumber());
    }

    /**
     * Get ticket statistics (for admin)
     * 
     * @return Map of statistics
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getTicketStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalTickets", supportTicketRepository.count());
        stats.put("openTickets", supportTicketRepository.countByStatus(SupportTicket.TicketStatus.OPEN));
        stats.put("inProgressTickets", supportTicketRepository.countByStatus(SupportTicket.TicketStatus.IN_PROGRESS));
        stats.put("resolvedTickets", supportTicketRepository.countByStatus(SupportTicket.TicketStatus.RESOLVED));
        stats.put("closedTickets", supportTicketRepository.countByStatus(SupportTicket.TicketStatus.CLOSED));
        return stats;
    }

    /**
     * Generate unique ticket number
     * 
     * @return Ticket number
     */
    private String generateTicketNumber() {
        String ticketNumber;
        do {
            ticketNumber = "TKT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (supportTicketRepository.findByTicketNumber(ticketNumber).isPresent());
        return ticketNumber;
    }
}

