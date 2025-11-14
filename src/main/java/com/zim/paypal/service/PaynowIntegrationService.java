package com.zim.paypal.service;

import com.zim.paypal.model.entity.GatewayTransaction;
import com.zim.paypal.model.entity.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import zw.co.paynow.core.MobileMoneyMethod;
import zw.co.paynow.core.Paynow;
import zw.co.paynow.core.Payment;
import zw.co.paynow.core.MobileInitResponse;
import zw.co.paynow.core.WebInitResponse;
import zw.co.paynow.core.StatusResponse;

import java.math.BigDecimal;

/**
 * Service for integrating with Paynow payment gateway
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaynowIntegrationService {

    @Value("${app.base-url:http://localhost:80}")
    private String baseUrl;

    /**
     * Initialize a mobile money payment via Paynow
     * 
     * @param gateway Payment gateway configuration
     * @param gatewayTransaction Gateway transaction entity
     * @return MobileInitResponse from Paynow
     */
    public MobileInitResponse initiateMobilePayment(PaymentGateway gateway, GatewayTransaction gatewayTransaction) {
        try {
            // Get integration credentials from gateway configuration
            String integrationId = gateway.getMerchantId(); // Using merchant_id for integration ID
            String integrationKey = gateway.getApiKey(); // Using api_key for integration key

            if (integrationId == null || integrationKey == null) {
                throw new IllegalStateException("Paynow integration credentials not configured");
            }

            // Initialize Paynow instance
            Paynow paynow = new Paynow(integrationId, integrationKey);

            // Set result and return URLs
            String resultUrl = baseUrl + "/api/gateways/paynow/callback";
            String returnUrl = baseUrl + "/deposit?status=success";
            paynow.setResultUrl(resultUrl);
            paynow.setReturnUrl(returnUrl);

            // Create payment with merchant reference
            String merchantReference = "GTX" + gatewayTransaction.getId();
            Payment payment = paynow.createPayment(merchantReference, gatewayTransaction.getEmail());

            // Add the deposit amount as an item
            payment.add("Account Deposit", gatewayTransaction.getAmount().doubleValue());

            // Determine mobile money method
            MobileMoneyMethod method = MobileMoneyMethod.ECOCASH; // Default to EcoCash
            if (gatewayTransaction.getPhoneNumber() != null) {
                // You can determine method based on phone number prefix or gateway name
                String gatewayName = gateway.getGatewayName();
                if (gatewayName != null && gatewayName.contains("PAYNOW")) {
                    // For PayNow, default to EcoCash, but could be OneMoney too
                    method = MobileMoneyMethod.ECOCASH;
                }
            }

            // Send mobile payment request
            MobileInitResponse response = paynow.sendMobile(
                    payment, 
                    gatewayTransaction.getPhoneNumber(), 
                    method
            );

            if (response.success()) {
                log.info("Paynow mobile payment initiated successfully. Poll URL: {}", response.pollUrl());
            } else {
                log.error("Paynow mobile payment initiation failed: {}", response.errors());
            }

            return response;
        } catch (Exception e) {
            log.error("Error initiating Paynow mobile payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initiate Paynow payment: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize a web-based payment via Paynow
     * 
     * @param gateway Payment gateway configuration
     * @param gatewayTransaction Gateway transaction entity
     * @return WebInitResponse from Paynow
     */
    public WebInitResponse initiateWebPayment(PaymentGateway gateway, GatewayTransaction gatewayTransaction) {
        try {
            // Get integration credentials from gateway configuration
            String integrationId = gateway.getMerchantId();
            String integrationKey = gateway.getApiKey();

            if (integrationId == null || integrationKey == null) {
                throw new IllegalStateException("Paynow integration credentials not configured");
            }

            // Initialize Paynow instance
            Paynow paynow = new Paynow(integrationId, integrationKey);

            // Set result and return URLs
            String resultUrl = baseUrl + "/api/gateways/paynow/callback";
            String returnUrl = baseUrl + "/deposit?status=success";
            paynow.setResultUrl(resultUrl);
            paynow.setReturnUrl(returnUrl);

            // Create payment with merchant reference
            String merchantReference = "GTX" + gatewayTransaction.getId();
            Payment payment = paynow.createPayment(merchantReference);

            // Add the deposit amount as an item
            payment.add("Account Deposit", gatewayTransaction.getAmount().doubleValue());

            // Set cart description
            payment.setCartDescription("Deposit to Zim PayPal Account");

            // Send web payment request
            WebInitResponse response = paynow.send(payment);

            if (response.success()) {
                log.info("Paynow web payment initiated successfully. Redirect URL: {}", response.redirectURL());
            } else {
                log.error("Paynow web payment initiation failed: {}", response.errors());
            }

            return response;
        } catch (Exception e) {
            log.error("Error initiating Paynow web payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initiate Paynow payment: " + e.getMessage(), e);
        }
    }

    /**
     * Poll transaction status from Paynow
     * 
     * @param gateway Payment gateway configuration
     * @param pollUrl Poll URL from Paynow response
     * @return StatusResponse from Paynow
     */
    public StatusResponse pollTransactionStatus(PaymentGateway gateway, String pollUrl) {
        try {
            String integrationId = gateway.getMerchantId();
            String integrationKey = gateway.getApiKey();

            if (integrationId == null || integrationKey == null) {
                throw new IllegalStateException("Paynow integration credentials not configured");
            }

            Paynow paynow = new Paynow(integrationId, integrationKey);
            StatusResponse status = paynow.pollTransaction(pollUrl);

            return status;
        } catch (Exception e) {
            log.error("Error polling Paynow transaction status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to poll transaction status: " + e.getMessage(), e);
        }
    }
}

