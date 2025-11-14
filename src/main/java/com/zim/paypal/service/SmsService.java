package com.zim.paypal.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for sending SMS notifications via Twilio
 * 
 * @author Zim Development Team
 */
@Service
@Slf4j
public class SmsService {

    @Value("${app.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.twilio.auth-token:}")
    private String authToken;

    @Value("${app.twilio.phone-number:}")
    private String phoneNumber;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isEmpty() && 
            authToken != null && !authToken.isEmpty()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized");
        } else {
            log.warn("Twilio credentials not configured. SMS functionality will be disabled.");
        }
    }

    /**
     * Send SMS
     * 
     * @param to Recipient phone number
     * @param message Message text
     */
    public void sendSms(String to, String message) {
        if (accountSid == null || accountSid.isEmpty() || 
            authToken == null || authToken.isEmpty() ||
            phoneNumber == null || phoneNumber.isEmpty()) {
            log.warn("Twilio not configured. SMS not sent to: {}", to);
            return;
        }

        try {
            Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(phoneNumber),
                    message
            ).create();
            
            log.info("SMS sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Send verification SMS
     * 
     * @param to Recipient phone number
     * @param verificationCode Verification code
     */
    public void sendVerificationSms(String to, String verificationCode) {
        String message = "Your Zim PayPal verification code is: " + verificationCode + 
                        ". This code will expire in 10 minutes.";
        sendSms(to, message);
    }
}

