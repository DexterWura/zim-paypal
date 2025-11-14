package com.zim.paypal.service;

import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.MoneyRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for money request management operations
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final UserService userService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private static final int DEFAULT_EXPIRY_DAYS = 30;

    /**
     * Create a money request
     * 
     * @param requesterId Requester user ID
     * @param recipientEmail Recipient email
     * @param amount Amount requested
     * @param message Message/description
     * @param note Optional note
     * @return Created money request
     */
    public MoneyRequest createRequest(Long requesterId, String recipientEmail, 
                                     BigDecimal amount, String message, String note) {
        User requester = userService.findById(requesterId);
        User recipient = userService.findByEmail(recipientEmail);

        if (requester.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Cannot request money from yourself");
        }

        String requestNumber = generateRequestNumber();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS);

        MoneyRequest request = MoneyRequest.builder()
                .requestNumber(requestNumber)
                .requester(requester)
                .recipient(recipient)
                .amount(amount)
                .currencyCode("USD")
                .status(MoneyRequest.RequestStatus.PENDING)
                .message(message)
                .note(note)
                .expiresAt(expiresAt)
                .build();

        MoneyRequest savedRequest = moneyRequestRepository.save(request);

        // Send notification to recipient
        notificationService.sendMoneyRequestNotification(savedRequest);

        log.info("Money request created: {} from {} to {}", requestNumber, requester.getUsername(), recipient.getUsername());
        return savedRequest;
    }

    /**
     * Approve a money request
     * 
     * @param requestId Request ID
     * @param recipientId Recipient user ID (for security)
     * @return Created transaction
     */
    public Transaction approveRequest(Long requestId, Long recipientId) {
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found: " + requestId));

        if (!request.getRecipient().getId().equals(recipientId)) {
            throw new IllegalArgumentException("You are not authorized to approve this request");
        }

        if (!request.isPending()) {
            throw new IllegalStateException("Request is not pending");
        }

        if (request.isExpired()) {
            request.markAsExpired();
            moneyRequestRepository.save(request);
            throw new IllegalStateException("Request has expired");
        }

        // Create transfer transaction
        Transaction transaction = transactionService.createTransfer(
                request.getRecipient().getId(),
                request.getRequester().getEmail(),
                request.getAmount(),
                request.getMessage() != null ? request.getMessage() : "Payment for money request " + request.getRequestNumber()
        );

        // Link transaction to request (bidirectional)
        request.setTransaction(transaction);
        transaction.setMoneyRequest(request);
        request.markAsApproved();
        moneyRequestRepository.save(request);

        log.info("Money request approved: {}", request.getRequestNumber());
        return transaction;
    }

    /**
     * Decline a money request
     * 
     * @param requestId Request ID
     * @param recipientId Recipient user ID (for security)
     */
    public void declineRequest(Long requestId, Long recipientId) {
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found: " + requestId));

        if (!request.getRecipient().getId().equals(recipientId)) {
            throw new IllegalArgumentException("You are not authorized to decline this request");
        }

        if (!request.isPending()) {
            throw new IllegalStateException("Request is not pending");
        }

        request.markAsDeclined();
        moneyRequestRepository.save(request);

        // Send notification to requester
        notificationService.sendMoneyRequestDeclinedNotification(request);

        log.info("Money request declined: {}", request.getRequestNumber());
    }

    /**
     * Cancel a money request (by requester)
     * 
     * @param requestId Request ID
     * @param requesterId Requester user ID (for security)
     */
    public void cancelRequest(Long requestId, Long requesterId) {
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found: " + requestId));

        if (!request.getRequester().getId().equals(requesterId)) {
            throw new IllegalArgumentException("You are not authorized to cancel this request");
        }

        if (!request.isPending()) {
            throw new IllegalStateException("Request is not pending");
        }

        request.markAsCancelled();
        moneyRequestRepository.save(request);

        log.info("Money request cancelled: {}", request.getRequestNumber());
    }

    /**
     * Find request by request number
     * 
     * @param requestNumber Request number
     * @return MoneyRequest entity
     */
    @Transactional(readOnly = true)
    public MoneyRequest findByRequestNumber(String requestNumber) {
        return moneyRequestRepository.findByRequestNumber(requestNumber)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found: " + requestNumber));
    }

    /**
     * Get pending requests for recipient
     * 
     * @param recipientId Recipient user ID
     * @return List of pending requests
     */
    @Transactional(readOnly = true)
    public List<MoneyRequest> getPendingRequestsForRecipient(Long recipientId) {
        User recipient = userService.findById(recipientId);
        return moneyRequestRepository.findPendingRequestsByRecipient(recipient);
    }

    /**
     * Get pending requests for requester
     * 
     * @param requesterId Requester user ID
     * @return List of pending requests
     */
    @Transactional(readOnly = true)
    public List<MoneyRequest> getPendingRequestsForRequester(Long requesterId) {
        User requester = userService.findById(requesterId);
        return moneyRequestRepository.findPendingRequestsByRequester(requester);
    }

    /**
     * Get all requests for user (as requester or recipient)
     * 
     * @param userId User ID
     * @param pageable Pageable object
     * @return Page of requests
     */
    @Transactional(readOnly = true)
    public Page<MoneyRequest> getRequestsByUser(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        // This would need a custom query to get both requester and recipient requests
        // For now, return recipient requests
        return moneyRequestRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get count of pending requests for recipient
     * 
     * @param recipientId Recipient user ID
     * @return Count of pending requests
     */
    @Transactional(readOnly = true)
    public long getPendingRequestCount(Long recipientId) {
        User recipient = userService.findById(recipientId);
        return moneyRequestRepository.countByRecipientAndStatus(recipient, MoneyRequest.RequestStatus.PENDING);
    }

    /**
     * Process expired requests (scheduled job)
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    public void processExpiredRequests() {
        List<MoneyRequest> expiredRequests = moneyRequestRepository.findExpiredRequests(LocalDateTime.now());
        for (MoneyRequest request : expiredRequests) {
            request.markAsExpired();
            moneyRequestRepository.save(request);
            log.info("Marked request as expired: {}", request.getRequestNumber());
        }
    }

    /**
     * Generate unique request number
     * 
     * @return Request number
     */
    private String generateRequestNumber() {
        String requestNumber;
        do {
            requestNumber = "REQ" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();
        } while (moneyRequestRepository.findByRequestNumber(requestNumber).isPresent());
        return requestNumber;
    }
}

