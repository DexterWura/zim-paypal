package com.zim.paypal.service;

import com.zim.paypal.model.entity.Notification;
import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for email and SMS notifications
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * Send transaction notification
     * 
     * @param transaction Transaction entity
     */
    @Async
    public void sendTransactionNotification(Transaction transaction) {
        try {
            if (transaction.getSender() != null) {
                sendTransactionNotificationToUser(transaction.getSender(), transaction, "sender");
            }
            if (transaction.getReceiver() != null) {
                sendTransactionNotificationToUser(transaction.getReceiver(), transaction, "receiver");
            }
        } catch (Exception e) {
            log.error("Error sending transaction notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send transaction notification to user
     * 
     * @param user User entity
     * @param transaction Transaction entity
     * @param role Role (sender/receiver)
     */
    private void sendTransactionNotificationToUser(User user, Transaction transaction, String role) {
        String subject = "Transaction " + transaction.getTransactionNumber();
        String message = buildTransactionMessage(user, transaction, role);
        
        // Send email
        if (user.getEmail() != null && user.getEmailVerified()) {
            Notification emailNotification = Notification.builder()
                    .user(user)
                    .notificationType(Notification.NotificationType.TRANSACTION)
                    .channel(Notification.NotificationChannel.EMAIL)
                    .recipient(user.getEmail())
                    .subject(subject)
                    .message(message)
                    .status(Notification.NotificationStatus.PENDING)
                    .referenceId(transaction.getTransactionNumber())
                    .build();
            
            emailNotification = notificationRepository.save(emailNotification);
            
            try {
                emailService.sendEmail(user.getEmail(), subject, message);
                emailNotification.markAsSent();
                notificationRepository.save(emailNotification);
            } catch (Exception e) {
                emailNotification.markAsFailed(e.getMessage());
                notificationRepository.save(emailNotification);
                log.error("Failed to send email notification: {}", e.getMessage());
            }
        }
        
        // Send SMS
        if (user.getPhoneNumber() != null && user.getPhoneVerified()) {
            Notification smsNotification = Notification.builder()
                    .user(user)
                    .notificationType(Notification.NotificationType.TRANSACTION)
                    .channel(Notification.NotificationChannel.SMS)
                    .recipient(user.getPhoneNumber())
                    .message(message)
                    .status(Notification.NotificationStatus.PENDING)
                    .referenceId(transaction.getTransactionNumber())
                    .build();
            
            smsNotification = notificationRepository.save(smsNotification);
            
            try {
                smsService.sendSms(user.getPhoneNumber(), message);
                smsNotification.markAsSent();
                notificationRepository.save(smsNotification);
            } catch (Exception e) {
                smsNotification.markAsFailed(e.getMessage());
                notificationRepository.save(smsNotification);
                log.error("Failed to send SMS notification: {}", e.getMessage());
            }
        }
    }

    /**
     * Build transaction message
     * 
     * @param user User entity
     * @param transaction Transaction entity
     * @param role Role (sender/receiver)
     * @return Message text
     */
    private String buildTransactionMessage(User user, Transaction transaction, String role) {
        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(user.getFirstName()).append(",\n\n");
        
        if ("sender".equals(role)) {
            message.append("You sent ").append(transaction.getAmount())
                   .append(" ").append(transaction.getCurrencyCode());
            if (transaction.getReceiver() != null) {
                message.append(" to ").append(transaction.getReceiver().getFullName());
            }
        } else {
            message.append("You received ").append(transaction.getAmount())
                   .append(" ").append(transaction.getCurrencyCode());
            if (transaction.getSender() != null) {
                message.append(" from ").append(transaction.getSender().getFullName());
            }
        }
        
        message.append(".\n\n");
        message.append("Transaction Number: ").append(transaction.getTransactionNumber()).append("\n");
        if (transaction.getDescription() != null) {
            message.append("Description: ").append(transaction.getDescription()).append("\n");
        }
        message.append("Status: ").append(transaction.getStatus()).append("\n");
        message.append("Date: ").append(transaction.getCreatedAt()).append("\n\n");
        message.append("Thank you for using Zim PayPal!");
        
        return message.toString();
    }
}

