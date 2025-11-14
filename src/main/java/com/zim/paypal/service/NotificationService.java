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

    /**
     * Send money request notification to recipient
     * 
     * @param request MoneyRequest entity
     */
    @Async
    public void sendMoneyRequestNotification(com.zim.paypal.model.entity.MoneyRequest request) {
        try {
            User recipient = request.getRecipient();
            String subject = "Money Request from " + request.getRequester().getFullName();
            String message = buildMoneyRequestMessage(recipient, request);
            
            // Send email
            if (recipient.getEmail() != null && recipient.getEmailVerified()) {
                Notification emailNotification = Notification.builder()
                        .user(recipient)
                        .notificationType(Notification.NotificationType.PAYMENT_REQUEST)
                        .channel(Notification.NotificationChannel.EMAIL)
                        .recipient(recipient.getEmail())
                        .subject(subject)
                        .message(message)
                        .status(Notification.NotificationStatus.PENDING)
                        .referenceId(request.getRequestNumber())
                        .build();
                
                emailNotification = notificationRepository.save(emailNotification);
                
                try {
                    emailService.sendEmail(recipient.getEmail(), subject, message);
                    emailNotification.markAsSent();
                    notificationRepository.save(emailNotification);
                } catch (Exception e) {
                    emailNotification.markAsFailed(e.getMessage());
                    notificationRepository.save(emailNotification);
                    log.error("Failed to send email notification: {}", e.getMessage());
                }
            }
            
            // Send SMS
            if (recipient.getPhoneNumber() != null && recipient.getPhoneVerified()) {
                Notification smsNotification = Notification.builder()
                        .user(recipient)
                        .notificationType(Notification.NotificationType.PAYMENT_REQUEST)
                        .channel(Notification.NotificationChannel.SMS)
                        .recipient(recipient.getPhoneNumber())
                        .message(message)
                        .status(Notification.NotificationStatus.PENDING)
                        .referenceId(request.getRequestNumber())
                        .build();
                
                smsNotification = notificationRepository.save(smsNotification);
                
                try {
                    smsService.sendSms(recipient.getPhoneNumber(), message);
                    smsNotification.markAsSent();
                    notificationRepository.save(smsNotification);
                } catch (Exception e) {
                    smsNotification.markAsFailed(e.getMessage());
                    notificationRepository.save(smsNotification);
                    log.error("Failed to send SMS notification: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error sending money request notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send money request declined notification to requester
     * 
     * @param request MoneyRequest entity
     */
    @Async
    public void sendMoneyRequestDeclinedNotification(com.zim.paypal.model.entity.MoneyRequest request) {
        try {
            User requester = request.getRequester();
            String subject = "Money Request Declined";
            String message = buildMoneyRequestDeclinedMessage(requester, request);
            
            // Send email
            if (requester.getEmail() != null && requester.getEmailVerified()) {
                Notification emailNotification = Notification.builder()
                        .user(requester)
                        .notificationType(Notification.NotificationType.PAYMENT_REQUEST)
                        .channel(Notification.NotificationChannel.EMAIL)
                        .recipient(requester.getEmail())
                        .subject(subject)
                        .message(message)
                        .status(Notification.NotificationStatus.PENDING)
                        .referenceId(request.getRequestNumber())
                        .build();
                
                emailNotification = notificationRepository.save(emailNotification);
                
                try {
                    emailService.sendEmail(requester.getEmail(), subject, message);
                    emailNotification.markAsSent();
                    notificationRepository.save(emailNotification);
                } catch (Exception e) {
                    emailNotification.markAsFailed(e.getMessage());
                    notificationRepository.save(emailNotification);
                    log.error("Failed to send email notification: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error sending money request declined notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Build money request message
     */
    private String buildMoneyRequestMessage(User recipient, com.zim.paypal.model.entity.MoneyRequest request) {
        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(recipient.getFirstName()).append(",\n\n");
        message.append(request.getRequester().getFullName()).append(" has requested ")
               .append(request.getAmount()).append(" ").append(request.getCurrencyCode());
        
        if (request.getMessage() != null && !request.getMessage().isEmpty()) {
            message.append(" for: ").append(request.getMessage());
        }
        
        message.append(".\n\n");
        message.append("Request Number: ").append(request.getRequestNumber()).append("\n");
        if (request.getNote() != null && !request.getNote().isEmpty()) {
            message.append("Note: ").append(request.getNote()).append("\n");
        }
        message.append("Please log in to approve or decline this request.\n\n");
        message.append("Thank you for using Zim PayPal!");
        
        return message.toString();
    }

    /**
     * Build money request declined message
     */
    private String buildMoneyRequestDeclinedMessage(User requester, com.zim.paypal.model.entity.MoneyRequest request) {
        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(requester.getFirstName()).append(",\n\n");
        message.append("Your money request of ").append(request.getAmount())
               .append(" ").append(request.getCurrencyCode())
               .append(" from ").append(request.getRecipient().getFullName())
               .append(" has been declined.\n\n");
        message.append("Request Number: ").append(request.getRequestNumber()).append("\n\n");
        message.append("Thank you for using Zim PayPal!");
        
        return message.toString();
    }

    /**
     * Send ticket resolved notification
     * 
     * @param ticket SupportTicket entity
     */
    @Async
    public void sendTicketResolvedNotification(com.zim.paypal.model.entity.SupportTicket ticket) {
        try {
            User user = ticket.getUser();
            String subject = "Support Ticket Resolved: " + ticket.getTicketNumber();
            String message = buildTicketResolvedMessage(user, ticket);
            
            // Send email
            if (user.getEmail() != null && user.getEmailVerified()) {
                Notification emailNotification = Notification.builder()
                        .user(user)
                        .notificationType(Notification.NotificationType.ACCOUNT_UPDATE)
                        .channel(Notification.NotificationChannel.EMAIL)
                        .recipient(user.getEmail())
                        .subject(subject)
                        .message(message)
                        .status(Notification.NotificationStatus.PENDING)
                        .referenceId(ticket.getTicketNumber())
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
        } catch (Exception e) {
            log.error("Error sending ticket resolved notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Build ticket resolved message
     */
    private String buildTicketResolvedMessage(User user, com.zim.paypal.model.entity.SupportTicket ticket) {
        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(user.getFirstName()).append(",\n\n");
        message.append("Your support ticket has been resolved.\n\n");
        message.append("Ticket Number: ").append(ticket.getTicketNumber()).append("\n");
        message.append("Subject: ").append(ticket.getSubject()).append("\n");
        if (ticket.getResolution() != null && !ticket.getResolution().isEmpty()) {
            message.append("Resolution: ").append(ticket.getResolution()).append("\n");
        }
        message.append("\nThank you for contacting Zim PayPal support!");
        
        return message.toString();
    }

    /**
     * Send bill split notification to participant
     * 
     * @param participant BillSplitParticipant entity
     */
    @Async
    public void sendBillSplitNotification(com.zim.paypal.model.entity.BillSplitParticipant participant) {
        try {
            User user = participant.getUser();
            com.zim.paypal.model.entity.BillSplit billSplit = participant.getBillSplit();
            String subject = "Bill Split Request: " + billSplit.getDescription();
            String message = buildBillSplitMessage(user, participant);
            
            // Send email
            if (user.getEmail() != null && user.getEmailVerified()) {
                Notification emailNotification = Notification.builder()
                        .user(user)
                        .notificationType(Notification.NotificationType.PAYMENT_REQUEST)
                        .channel(Notification.NotificationChannel.EMAIL)
                        .recipient(user.getEmail())
                        .subject(subject)
                        .message(message)
                        .status(Notification.NotificationStatus.PENDING)
                        .referenceId(billSplit.getSplitNumber())
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
        } catch (Exception e) {
            log.error("Error sending bill split notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Build bill split message
     */
    private String buildBillSplitMessage(User user, com.zim.paypal.model.entity.BillSplitParticipant participant) {
        com.zim.paypal.model.entity.BillSplit billSplit = participant.getBillSplit();
        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(user.getFirstName()).append(",\n\n");
        message.append(billSplit.getCreator().getFullName()).append(" has split a bill with you.\n\n");
        message.append("Description: ").append(billSplit.getDescription()).append("\n");
        message.append("Your share: ").append(participant.getAmount())
               .append(" ").append(billSplit.getCurrencyCode()).append("\n");
        message.append("Total amount: ").append(billSplit.getTotalAmount())
               .append(" ").append(billSplit.getCurrencyCode()).append("\n");
        message.append("Split Number: ").append(billSplit.getSplitNumber()).append("\n\n");
        message.append("Please log in to pay your share.\n\n");
        message.append("Thank you for using Zim PayPal!");
        
        return message.toString();
    }

    /**
     * Send reversal rejected notification
     * 
     * @param reversal TransactionReversal entity
     */
    @Async
    public void sendReversalRejectedNotification(com.zim.paypal.model.entity.TransactionReversal reversal) {
        try {
            User user = reversal.getRequestedBy();
            String subject = "Transaction Reversal Request Rejected: " + reversal.getReversalNumber();
            String message = buildReversalRejectedMessage(user, reversal);
            
            // Send email
            if (user.getEmail() != null && user.getEmailVerified()) {
                Notification emailNotification = Notification.builder()
                        .user(user)
                        .notificationType(Notification.NotificationType.ACCOUNT_UPDATE)
                        .channel(Notification.NotificationChannel.EMAIL)
                        .recipient(user.getEmail())
                        .subject(subject)
                        .message(message)
                        .status(Notification.NotificationStatus.PENDING)
                        .referenceId(reversal.getReversalNumber())
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
        } catch (Exception e) {
            log.error("Error sending reversal rejected notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send reversal processed notification
     * 
     * @param reversal TransactionReversal entity
     */
    @Async
    public void sendReversalProcessedNotification(com.zim.paypal.model.entity.TransactionReversal reversal) {
        try {
            User user = reversal.getRequestedBy();
            String subject = "Transaction Reversal Processed: " + reversal.getReversalNumber();
            String message = buildReversalProcessedMessage(user, reversal);
            
            // Send email
            if (user.getEmail() != null && user.getEmailVerified()) {
                Notification emailNotification = Notification.builder()
                        .user(user)
                        .notificationType(Notification.NotificationType.PAYMENT_RECEIVED)
                        .channel(Notification.NotificationChannel.EMAIL)
                        .recipient(user.getEmail())
                        .subject(subject)
                        .message(message)
                        .status(Notification.NotificationStatus.PENDING)
                        .referenceId(reversal.getReversalNumber())
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
        } catch (Exception e) {
            log.error("Error sending reversal processed notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Build reversal rejected message
     */
    private String buildReversalRejectedMessage(User user, com.zim.paypal.model.entity.TransactionReversal reversal) {
        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(user.getFirstName()).append(",\n\n");
        message.append("Your transaction reversal request has been rejected.\n\n");
        message.append("Reversal Number: ").append(reversal.getReversalNumber()).append("\n");
        message.append("Transaction ID: ").append(reversal.getTransaction().getId()).append("\n");
        message.append("Amount: ").append(reversal.getReversalAmount()).append("\n");
        if (reversal.getAdminNotes() != null && !reversal.getAdminNotes().isEmpty()) {
            message.append("Reason: ").append(reversal.getAdminNotes()).append("\n");
        }
        message.append("\nIf you have any questions, please contact support.\n\n");
        message.append("Thank you for using Zim PayPal!");
        
        return message.toString();
    }

    /**
     * Build reversal processed message
     */
    private String buildReversalProcessedMessage(User user, com.zim.paypal.model.entity.TransactionReversal reversal) {
        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(user.getFirstName()).append(",\n\n");
        message.append("Your transaction reversal has been processed successfully.\n\n");
        message.append("Reversal Number: ").append(reversal.getReversalNumber()).append("\n");
        message.append("Transaction ID: ").append(reversal.getTransaction().getId()).append("\n");
        message.append("Reversal Amount: ").append(reversal.getReversalAmount()).append("\n");
        if (reversal.getReversalTransaction() != null) {
            message.append("Reversal Transaction ID: ").append(reversal.getReversalTransaction().getId()).append("\n");
        }
        message.append("\nThe funds have been returned to your account.\n\n");
        message.append("Thank you for using Zim PayPal!");
        
        return message.toString();
    }
}

