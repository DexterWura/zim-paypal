package com.zim.paypal.service;

import com.zim.paypal.model.entity.GatewayTransaction;
import com.zim.paypal.model.entity.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * Service for integrating with Paynow payment gateway
 * Uses reflection to handle Paynow SDK classes (in case SDK is not available)
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaynowIntegrationService {

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    private static final boolean PAYNOW_SDK_AVAILABLE = checkPaynowSdkAvailable();

    private static boolean checkPaynowSdkAvailable() {
        try {
            Class.forName("zw.co.paynow.core.Paynow");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Initialize a mobile money payment via Paynow
     */
    public Object initiateMobilePayment(PaymentGateway gateway, GatewayTransaction gatewayTransaction) {
        if (!PAYNOW_SDK_AVAILABLE) {
            log.warn("Paynow SDK not available. Please ensure zw.co.paynow:java-sdk:1.1.2 is in your classpath.");
            throw new IllegalStateException("Paynow SDK is not available. Please add the dependency to pom.xml");
        }

        try {
            // Get integration credentials
            String integrationId = gateway.getMerchantId();
            String integrationKey = gateway.getApiKey();

            if (integrationId == null || integrationKey == null) {
                throw new IllegalStateException("Paynow integration credentials not configured");
            }

            // Use reflection to call Paynow SDK
            Class<?> paynowClass = Class.forName("zw.co.paynow.core.Paynow");
            Object paynow = paynowClass.getConstructor(String.class, String.class)
                    .newInstance(integrationId, integrationKey);

            // Set URLs
            Method setResultUrl = paynowClass.getMethod("setResultUrl", String.class);
            Method setReturnUrl = paynowClass.getMethod("setReturnUrl", String.class);
            setResultUrl.invoke(paynow, baseUrl + "/api/gateways/paynow/callback");
            setReturnUrl.invoke(paynow, baseUrl + "/deposit?status=success");

            // Create payment
            Method createPayment = paynowClass.getMethod("createPayment", String.class, String.class);
            Object payment = createPayment.invoke(paynow, "GTX" + gatewayTransaction.getId(), 
                    gatewayTransaction.getEmail());

            // Add item
            Method add = payment.getClass().getMethod("add", String.class, double.class);
            add.invoke(payment, "Account Deposit", gatewayTransaction.getAmount().doubleValue());

            // Get mobile money method enum
            Class<?> mobileMoneyMethodClass = Class.forName("zw.co.paynow.core.MobileMoneyMethod");
            Object ecoCashMethod = Enum.valueOf((Class<Enum>) mobileMoneyMethodClass, "ECOCASH");

            // Send mobile payment
            Method sendMobile = paynowClass.getMethod("sendMobile", payment.getClass(), 
                    String.class, mobileMoneyMethodClass);
            Object response = sendMobile.invoke(paynow, payment, gatewayTransaction.getPhoneNumber(), ecoCashMethod);

            // Check if successful
            Method success = response.getClass().getMethod("success");
            Boolean isSuccess = (Boolean) success.invoke(response);

            if (isSuccess) {
                Method pollUrl = response.getClass().getMethod("pollUrl");
                String pollUrlStr = (String) pollUrl.invoke(response);
                log.info("Paynow mobile payment initiated successfully. Poll URL: {}", pollUrlStr);
            } else {
                Method errors = response.getClass().getMethod("errors");
                String errorStr = (String) errors.invoke(response);
                log.error("Paynow mobile payment initiation failed: {}", errorStr);
            }

            return response;
        } catch (Exception e) {
            log.error("Error initiating Paynow mobile payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initiate Paynow payment: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize a web-based payment via Paynow
     */
    public Object initiateWebPayment(PaymentGateway gateway, GatewayTransaction gatewayTransaction) {
        if (!PAYNOW_SDK_AVAILABLE) {
            log.warn("Paynow SDK not available. Please ensure zw.co.paynow:java-sdk:1.1.2 is in your classpath.");
            throw new IllegalStateException("Paynow SDK is not available. Please add the dependency to pom.xml");
        }

        try {
            String integrationId = gateway.getMerchantId();
            String integrationKey = gateway.getApiKey();

            if (integrationId == null || integrationKey == null) {
                throw new IllegalStateException("Paynow integration credentials not configured");
            }

            // Use reflection to call Paynow SDK
            Class<?> paynowClass = Class.forName("zw.co.paynow.core.Paynow");
            Object paynow = paynowClass.getConstructor(String.class, String.class)
                    .newInstance(integrationId, integrationKey);

            // Set URLs
            Method setResultUrl = paynowClass.getMethod("setResultUrl", String.class);
            Method setReturnUrl = paynowClass.getMethod("setReturnUrl", String.class);
            setResultUrl.invoke(paynow, baseUrl + "/api/gateways/paynow/callback");
            setReturnUrl.invoke(paynow, baseUrl + "/deposit?status=success");

            // Create payment
            Method createPayment = paynowClass.getMethod("createPayment", String.class);
            Object payment = createPayment.invoke(paynow, "GTX" + gatewayTransaction.getId());

            // Add item
            Method add = payment.getClass().getMethod("add", String.class, double.class);
            add.invoke(payment, "Account Deposit", gatewayTransaction.getAmount().doubleValue());

            // Set cart description
            Method setCartDescription = payment.getClass().getMethod("setCartDescription", String.class);
            setCartDescription.invoke(payment, "Deposit to Zim PayPal Account");

            // Send web payment
            Method send = paynowClass.getMethod("send", payment.getClass());
            Object response = send.invoke(paynow, payment);

            // Check if successful
            Method success = response.getClass().getMethod("success");
            Boolean isSuccess = (Boolean) success.invoke(response);

            if (isSuccess) {
                Method redirectURL = response.getClass().getMethod("redirectURL");
                String redirectUrlStr = (String) redirectURL.invoke(response);
                log.info("Paynow web payment initiated successfully. Redirect URL: {}", redirectUrlStr);
            } else {
                Method errors = response.getClass().getMethod("errors");
                String errorStr = (String) errors.invoke(response);
                log.error("Paynow web payment initiation failed: {}", errorStr);
            }

            return response;
        } catch (Exception e) {
            log.error("Error initiating Paynow web payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initiate Paynow payment: " + e.getMessage(), e);
        }
    }

    /**
     * Poll transaction status from Paynow
     */
    public Object pollTransactionStatus(PaymentGateway gateway, String pollUrl) {
        if (!PAYNOW_SDK_AVAILABLE) {
            log.warn("Paynow SDK not available");
            throw new IllegalStateException("Paynow SDK is not available");
        }

        try {
            String integrationId = gateway.getMerchantId();
            String integrationKey = gateway.getApiKey();

            if (integrationId == null || integrationKey == null) {
                throw new IllegalStateException("Paynow integration credentials not configured");
            }

            Class<?> paynowClass = Class.forName("zw.co.paynow.core.Paynow");
            Object paynow = paynowClass.getConstructor(String.class, String.class)
                    .newInstance(integrationId, integrationKey);

            Method pollTransaction = paynowClass.getMethod("pollTransaction", String.class);
            return pollTransaction.invoke(paynow, pollUrl);
        } catch (Exception e) {
            log.error("Error polling Paynow transaction status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to poll transaction status: " + e.getMessage(), e);
        }
    }
}
