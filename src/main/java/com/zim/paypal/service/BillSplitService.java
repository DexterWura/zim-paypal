package com.zim.paypal.service;

import com.zim.paypal.model.dto.BillSplitDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.BillSplitParticipantRepository;
import com.zim.paypal.repository.BillSplitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for bill split management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillSplitService {

    private final BillSplitRepository billSplitRepository;
    private final BillSplitParticipantRepository participantRepository;
    private final UserService userService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private static final int DEFAULT_EXPIRY_DAYS = 30;

    /**
     * Create a bill split
     * 
     * @param creatorId Creator user ID
     * @param splitDto Split DTO
     * @return Created bill split
     */
    public BillSplit createBillSplit(Long creatorId, BillSplitDto splitDto) {
        User creator = userService.findById(creatorId);
        
        String splitNumber = generateSplitNumber();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS);
        
        BillSplit billSplit = BillSplit.builder()
                .splitNumber(splitNumber)
                .creator(creator)
                .description(splitDto.getDescription())
                .totalAmount(splitDto.getTotalAmount())
                .currencyCode("USD")
                .splitMethod(splitDto.getSplitMethod())
                .status(BillSplit.SplitStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
        
        BillSplit savedSplit = billSplitRepository.save(billSplit);
        
        // Create participants
        List<BillSplitParticipant> participants = createParticipants(savedSplit, splitDto);
        savedSplit.setParticipants(new java.util.HashSet<>(participants));
        
        // Send notifications to participants
        for (BillSplitParticipant participant : participants) {
            notificationService.sendBillSplitNotification(participant);
        }
        
        log.info("Bill split created: {} by user: {}", splitNumber, creator.getUsername());
        return savedSplit;
    }

    /**
     * Create participants based on split method
     */
    private List<BillSplitParticipant> createParticipants(BillSplit billSplit, BillSplitDto splitDto) {
        List<BillSplitParticipant> participants = new ArrayList<>();
        BigDecimal totalAmount = billSplit.getTotalAmount();
        int participantCount = splitDto.getParticipants().size();
        
        switch (billSplit.getSplitMethod()) {
            case EQUAL:
                BigDecimal equalAmount = totalAmount.divide(
                        new BigDecimal(participantCount), 2, RoundingMode.HALF_UP);
                BigDecimal remainder = totalAmount.subtract(equalAmount.multiply(new BigDecimal(participantCount)));
                
                for (int i = 0; i < splitDto.getParticipants().size(); i++) {
                    BillSplitDto.ParticipantDto participantDto = splitDto.getParticipants().get(i);
                    User user = userService.findByEmail(participantDto.getEmail());
                    
                    BigDecimal amount = equalAmount;
                    // Add remainder to first participant to handle rounding
                    if (i == 0 && remainder.compareTo(BigDecimal.ZERO) != 0) {
                        amount = amount.add(remainder);
                    }
                    
                    BillSplitParticipant participant = BillSplitParticipant.builder()
                            .billSplit(billSplit)
                            .user(user)
                            .amount(amount)
                            .paymentStatus(BillSplitParticipant.PaymentStatus.PENDING)
                            .build();
                    
                    participants.add(participantRepository.save(participant));
                }
                break;
                
            case PERCENTAGE:
                BigDecimal totalPercentage = BigDecimal.ZERO;
                for (BillSplitDto.ParticipantDto participantDto : splitDto.getParticipants()) {
                    if (participantDto.getPercentage() != null) {
                        totalPercentage = totalPercentage.add(participantDto.getPercentage());
                    }
                }
                
                if (totalPercentage.compareTo(new BigDecimal(100)) != 0) {
                    throw new IllegalArgumentException("Percentages must sum to 100%");
                }
                
                for (BillSplitDto.ParticipantDto participantDto : splitDto.getParticipants()) {
                    User user = userService.findByEmail(participantDto.getEmail());
                    BigDecimal amount = totalAmount.multiply(participantDto.getPercentage())
                            .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                    
                    BillSplitParticipant participant = BillSplitParticipant.builder()
                            .billSplit(billSplit)
                            .user(user)
                            .amount(amount)
                            .paymentStatus(BillSplitParticipant.PaymentStatus.PENDING)
                            .build();
                    
                    participants.add(participantRepository.save(participant));
                }
                break;
                
            case CUSTOM:
                BigDecimal totalCustomAmount = BigDecimal.ZERO;
                for (BillSplitDto.ParticipantDto participantDto : splitDto.getParticipants()) {
                    if (participantDto.getAmount() != null) {
                        totalCustomAmount = totalCustomAmount.add(participantDto.getAmount());
                    }
                }
                
                if (totalCustomAmount.compareTo(totalAmount) != 0) {
                    throw new IllegalArgumentException("Custom amounts must sum to total amount");
                }
                
                for (BillSplitDto.ParticipantDto participantDto : splitDto.getParticipants()) {
                    User user = userService.findByEmail(participantDto.getEmail());
                    
                    BillSplitParticipant participant = BillSplitParticipant.builder()
                            .billSplit(billSplit)
                            .user(user)
                            .amount(participantDto.getAmount())
                            .paymentStatus(BillSplitParticipant.PaymentStatus.PENDING)
                            .build();
                    
                    participants.add(participantRepository.save(participant));
                }
                break;
        }
        
        return participants;
    }

    /**
     * Pay participant's share
     * 
     * @param participantId Participant ID
     * @param payerId Payer user ID
     * @return Created transaction
     */
    public Transaction payParticipantShare(Long participantId, Long payerId) {
        BillSplitParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + participantId));
        
        if (!participant.getUser().getId().equals(payerId)) {
            throw new IllegalArgumentException("You are not authorized to pay this share");
        }
        
        if (participant.isPaid()) {
            throw new IllegalStateException("This share has already been paid");
        }
        
        BillSplit billSplit = participant.getBillSplit();
        if (billSplit.isExpired()) {
            throw new IllegalStateException("This bill split has expired");
        }
        
        // Create transfer transaction
        Transaction transaction = transactionService.createTransfer(
                payerId,
                billSplit.getCreator().getEmail(),
                participant.getAmount(),
                "Payment for bill split: " + billSplit.getDescription()
        );
        
        // Update participant
        participant.setTransaction(transaction);
        participant.markAsPaid();
        participantRepository.save(participant);
        
        // Update bill split
        billSplit.addPaidAmount(participant.getAmount());
        billSplitRepository.save(billSplit);
        
        log.info("Participant share paid: {} for split: {}", participantId, billSplit.getSplitNumber());
        return transaction;
    }

    /**
     * Get split by split number
     * 
     * @param splitNumber Split number
     * @return BillSplit entity
     */
    @Transactional(readOnly = true)
    public BillSplit getSplitByNumber(String splitNumber) {
        return billSplitRepository.findBySplitNumber(splitNumber)
                .orElseThrow(() -> new IllegalArgumentException("Bill split not found: " + splitNumber));
    }

    /**
     * Get split by ID
     * 
     * @param splitId Split ID
     * @return BillSplit entity
     */
    @Transactional(readOnly = true)
    public BillSplit getSplitById(Long splitId) {
        return billSplitRepository.findById(splitId)
                .orElseThrow(() -> new IllegalArgumentException("Bill split not found: " + splitId));
    }

    /**
     * Get splits created by user
     * 
     * @param userId User ID
     * @param pageable Pageable object
     * @return Page of splits
     */
    @Transactional(readOnly = true)
    public Page<BillSplit> getSplitsByCreator(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return billSplitRepository.findByCreatorOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get splits where user is participant
     * 
     * @param userId User ID
     * @param pageable Pageable object
     * @return Page of splits
     */
    @Transactional(readOnly = true)
    public Page<BillSplit> getSplitsByParticipant(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return billSplitRepository.findSplitsByParticipant(user, pageable);
    }

    /**
     * Get pending participants for user
     * 
     * @param userId User ID
     * @return List of pending participants
     */
    @Transactional(readOnly = true)
    public List<BillSplitParticipant> getPendingParticipants(Long userId) {
        User user = userService.findById(userId);
        return participantRepository.findPendingParticipantsByUser(user);
    }

    /**
     * Get participants for a split
     * 
     * @param splitId Split ID
     * @return List of participants
     */
    @Transactional(readOnly = true)
    public List<BillSplitParticipant> getParticipants(Long splitId) {
        BillSplit billSplit = getSplitById(splitId);
        return participantRepository.findByBillSplit(billSplit);
    }

    /**
     * Cancel a bill split (by creator)
     * 
     * @param splitId Split ID
     * @param creatorId Creator user ID
     */
    public void cancelBillSplit(Long splitId, Long creatorId) {
        BillSplit billSplit = getSplitById(splitId);
        
        if (!billSplit.getCreator().getId().equals(creatorId)) {
            throw new IllegalArgumentException("Only the creator can cancel this bill split");
        }
        
        if (billSplit.getStatus() == BillSplit.SplitStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid bill split");
        }
        
        billSplit.setStatus(BillSplit.SplitStatus.CANCELLED);
        billSplitRepository.save(billSplit);
        
        log.info("Bill split cancelled: {}", billSplit.getSplitNumber());
    }

    /**
     * Process expired splits (scheduled job)
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    public void processExpiredSplits() {
        List<BillSplit> expiredSplits = billSplitRepository.findExpiredSplits(LocalDateTime.now());
        for (BillSplit split : expiredSplits) {
            split.setStatus(BillSplit.SplitStatus.EXPIRED);
            billSplitRepository.save(split);
            log.info("Marked bill split as expired: {}", split.getSplitNumber());
        }
    }

    /**
     * Generate unique split number
     * 
     * @return Split number
     */
    private String generateSplitNumber() {
        String splitNumber;
        do {
            splitNumber = "SPLIT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (billSplitRepository.findBySplitNumber(splitNumber).isPresent());
        return splitNumber;
    }
}

