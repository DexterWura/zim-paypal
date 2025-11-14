package com.zim.paypal.service;

import com.zim.paypal.model.dto.SubscriptionDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.RecurringPaymentRepository;
import com.zim.paypal.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for subscription and recurring payment management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {

    private final RecurringPaymentRepository recurringPaymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final EmailService emailService;

    /**
     * Create a new subscription
     * 
     * @param subscriptionDto Subscription DTO
     * @param subscriber Subscriber user
     * @return Created subscription
     */
    public RecurringPayment createSubscription(SubscriptionDto subscriptionDto, User subscriber) {
        User merchant = userService.findById(subscriptionDto.getMerchantId());
        Account account = subscriptionDto.getAccountId() != null ?
                accountService.findById(subscriptionDto.getAccountId()) :
                accountService.findActiveAccountByUser(subscriber);

        SubscriptionPlan plan = null;
        if (subscriptionDto.getPlanId() != null) {
            plan = planRepository.findById(subscriptionDto.getPlanId())
                    .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
        }

        String subscriptionId = generateSubscriptionId();
        LocalDateTime startDate = subscriptionDto.getStartDate() != null ?
                subscriptionDto.getStartDate() : LocalDateTime.now();
        LocalDateTime nextPaymentDate = calculateNextPaymentDate(startDate, subscriptionDto.getBillingCycle());

        LocalDateTime trialEndDate = null;
        if (subscriptionDto.getTrialPeriodDays() != null && subscriptionDto.getTrialPeriodDays() > 0) {
            trialEndDate = startDate.plusDays(subscriptionDto.getTrialPeriodDays());
        }

        RecurringPayment subscription = RecurringPayment.builder()
                .subscriptionId(subscriptionId)
                .subscriber(subscriber)
                .merchant(merchant)
                .subscriptionPlan(plan)
                .account(account)
                .amount(subscriptionDto.getAmount())
                .billingCycle(subscriptionDto.getBillingCycle())
                .status(RecurringPayment.Status.ACTIVE)
                .startDate(startDate)
                .nextPaymentDate(nextPaymentDate)
                .endDate(subscriptionDto.getEndDate())
                .trialEndDate(trialEndDate)
                .autoRenew(subscriptionDto.getAutoRenew())
                .description(subscriptionDto.getDescription())
                .build();

        RecurringPayment saved = recurringPaymentRepository.save(subscription);
        log.info("Subscription created: {} by user: {}", subscriptionId, subscriber.getUsername());
        return saved;
    }

    /**
     * Process due subscriptions (scheduled job)
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void processDueSubscriptions() {
        List<RecurringPayment> dueSubscriptions = recurringPaymentRepository.findDueSubscriptions(LocalDateTime.now());
        
        for (RecurringPayment subscription : dueSubscriptions) {
            try {
                processSubscriptionPayment(subscription);
            } catch (Exception e) {
                log.error("Failed to process subscription payment: {}", subscription.getSubscriptionId(), e);
                subscription.setFailedPayments(subscription.getFailedPayments() + 1);
                
                // Suspend after 3 failed payments
                if (subscription.getFailedPayments() >= 3) {
                    subscription.setStatus(RecurringPayment.Status.SUSPENDED);
                    emailService.sendEmail(subscription.getSubscriber().getEmail(),
                            "Subscription Suspended",
                            "Your subscription " + subscription.getSubscriptionId() + 
                            " has been suspended due to failed payments.");
                }
                
                recurringPaymentRepository.save(subscription);
            }
        }
    }

    /**
     * Process a single subscription payment
     * 
     * @param subscription Subscription to process
     * @return Transaction entity
     */
    public Transaction processSubscriptionPayment(RecurringPayment subscription) {
        // Check if in trial period
        if (subscription.isInTrial()) {
            subscription.calculateNextPaymentDate();
            recurringPaymentRepository.save(subscription);
            return null; // No payment during trial
        }

        // Process payment
        Transaction transaction = transactionService.createPaymentFromWallet(
                subscription.getSubscriber().getId(),
                subscription.getAmount(),
                "Recurring payment: " + subscription.getDescription(),
                subscription.getMerchant().getId());

        // Update subscription
        subscription.setTotalPayments(subscription.getTotalPayments() + 1);
        subscription.calculateNextPaymentDate();
        
        // Check if subscription should end
        if (subscription.getEndDate() != null && 
            subscription.getNextPaymentDate().isAfter(subscription.getEndDate())) {
            subscription.setStatus(RecurringPayment.Status.EXPIRED);
        }

        recurringPaymentRepository.save(subscription);

        // Send notification
        emailService.sendEmail(subscription.getSubscriber().getEmail(),
                "Recurring Payment Processed",
                "Your recurring payment of " + subscription.getAmount() + 
                " " + subscription.getAccount().getCurrencyCode() + 
                " has been processed.");

        log.info("Subscription payment processed: {}", subscription.getSubscriptionId());
        return transaction;
    }

    /**
     * Cancel subscription
     * 
     * @param subscriptionId Subscription ID
     * @param subscriber Subscriber user
     */
    public void cancelSubscription(String subscriptionId, User subscriber) {
        RecurringPayment subscription = recurringPaymentRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (!subscription.getSubscriber().getId().equals(subscriber.getId())) {
            throw new IllegalArgumentException("Subscription does not belong to user");
        }

        subscription.setStatus(RecurringPayment.Status.CANCELLED);
        subscription.setAutoRenew(false);
        recurringPaymentRepository.save(subscription);

        emailService.sendEmail(subscriber.getEmail(),
                "Subscription Cancelled",
                "Your subscription " + subscriptionId + " has been cancelled.");

        log.info("Subscription cancelled: {} by user: {}", subscriptionId, subscriber.getUsername());
    }

    /**
     * Pause subscription
     * 
     * @param subscriptionId Subscription ID
     * @param subscriber Subscriber user
     */
    public void pauseSubscription(String subscriptionId, User subscriber) {
        RecurringPayment subscription = recurringPaymentRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (!subscription.getSubscriber().getId().equals(subscriber.getId())) {
            throw new IllegalArgumentException("Subscription does not belong to user");
        }

        subscription.setStatus(RecurringPayment.Status.PAUSED);
        recurringPaymentRepository.save(subscription);

        log.info("Subscription paused: {} by user: {}", subscriptionId, subscriber.getUsername());
    }

    /**
     * Resume subscription
     * 
     * @param subscriptionId Subscription ID
     * @param subscriber Subscriber user
     */
    public void resumeSubscription(String subscriptionId, User subscriber) {
        RecurringPayment subscription = recurringPaymentRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (!subscription.getSubscriber().getId().equals(subscriber.getId())) {
            throw new IllegalArgumentException("Subscription does not belong to user");
        }

        subscription.setStatus(RecurringPayment.Status.ACTIVE);
        subscription.calculateNextPaymentDate();
        recurringPaymentRepository.save(subscription);

        log.info("Subscription resumed: {} by user: {}", subscriptionId, subscriber.getUsername());
    }

    /**
     * Get subscription by ID
     * 
     * @param subscriptionId Subscription ID
     * @return RecurringPayment entity
     */
    @Transactional(readOnly = true)
    public RecurringPayment getSubscriptionById(String subscriptionId) {
        return recurringPaymentRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
    }

    /**
     * Get subscriptions by subscriber
     * 
     * @param subscriber Subscriber user
     * @return List of subscriptions
     */
    @Transactional(readOnly = true)
    public List<RecurringPayment> getSubscriptionsBySubscriber(User subscriber) {
        return recurringPaymentRepository.findBySubscriberOrderByCreatedAtDesc(subscriber);
    }

    /**
     * Calculate next payment date
     */
    private LocalDateTime calculateNextPaymentDate(LocalDateTime startDate, SubscriptionPlan.BillingCycle cycle) {
        switch (cycle) {
            case DAILY:
                return startDate.plusDays(1);
            case WEEKLY:
                return startDate.plusWeeks(1);
            case MONTHLY:
                return startDate.plusMonths(1);
            case QUARTERLY:
                return startDate.plusMonths(3);
            case YEARLY:
                return startDate.plusYears(1);
            default:
                return startDate.plusMonths(1);
        }
    }

    /**
     * Generate unique subscription ID
     */
    private String generateSubscriptionId() {
        String subscriptionId;
        do {
            subscriptionId = "SUB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (recurringPaymentRepository.findBySubscriptionId(subscriptionId).isPresent());
        return subscriptionId;
    }
}

