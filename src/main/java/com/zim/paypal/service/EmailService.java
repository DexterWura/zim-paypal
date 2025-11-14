package com.zim.paypal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending email notifications
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name:Zim PayPal}")
    private String fromName;

    /**
     * Send email
     * 
     * @param to Recipient email
     * @param subject Subject
     * @param message Message body
     */
    public void sendEmail(String to, String subject, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromEmail);
            email.setTo(to);
            email.setSubject(subject);
            email.setText(message);
            
            mailSender.send(email);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send verification email
     * 
     * @param to Recipient email
     * @param verificationToken Verification token
     */
    public void sendVerificationEmail(String to, String verificationToken) {
        String subject = "Verify your Zim PayPal account";
        String message = "Hello,\n\n" +
                "Please verify your email address by clicking the following link:\n" +
                "http://localhost:8080/verify-email?token=" + verificationToken + "\n\n" +
                "If you did not create an account, please ignore this email.\n\n" +
                "Thank you,\n" +
                "Zim PayPal Team";
        
        sendEmail(to, subject, message);
    }
}

