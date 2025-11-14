package com.zim.paypal.service;

import com.zim.paypal.model.dto.PaymentButtonDto;
import com.zim.paypal.model.entity.Currency;
import com.zim.paypal.model.entity.PaymentButton;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.PaymentButtonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for payment button management
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentButtonService {

    private final PaymentButtonRepository buttonRepository;
    private final CurrencyService currencyService;

    /**
     * Create a new payment button
     * 
     * @param buttonDto Button DTO
     * @param merchant Merchant user
     * @return Created payment button
     */
    public PaymentButton createPaymentButton(PaymentButtonDto buttonDto, User merchant) {
        String buttonCode = generateUniqueButtonCode();
        
        Currency currency = null;
        String currencyCode = "USD";
        if (buttonDto.getCurrencyId() != null) {
            currency = currencyService.getCurrencyById(buttonDto.getCurrencyId());
            currencyCode = currency.getCurrencyCode();
        } else {
            currency = currencyService.getBaseCurrency();
            currencyCode = currency.getCurrencyCode();
        }

        PaymentButton button = PaymentButton.builder()
                .buttonCode(buttonCode)
                .merchant(merchant)
                .buttonName(buttonDto.getButtonName())
                .description(buttonDto.getDescription())
                .amount(buttonDto.getAmount())
                .allowCustomAmount(buttonDto.getAllowCustomAmount() != null ? buttonDto.getAllowCustomAmount() : false)
                .currency(currency)
                .currencyCode(currencyCode)
                .buttonStyle(buttonDto.getButtonStyle() != null ? buttonDto.getButtonStyle() : PaymentButton.ButtonStyle.DEFAULT)
                .buttonSize(buttonDto.getButtonSize() != null ? buttonDto.getButtonSize() : PaymentButton.ButtonSize.MEDIUM)
                .buttonColor(buttonDto.getButtonColor() != null ? buttonDto.getButtonColor() : "#0070BA")
                .isActive(true)
                .successUrl(buttonDto.getSuccessUrl())
                .cancelUrl(buttonDto.getCancelUrl())
                .notifyUrl(buttonDto.getNotifyUrl())
                .build();

        PaymentButton saved = buttonRepository.save(button);
        log.info("Payment button created: {} by merchant: {}", buttonCode, merchant.getUsername());
        return saved;
    }

    /**
     * Get payment button by code
     * 
     * @param buttonCode Button code
     * @return PaymentButton entity
     */
    @Transactional(readOnly = true)
    public PaymentButton getPaymentButtonByCode(String buttonCode) {
        PaymentButton button = buttonRepository.findByButtonCode(buttonCode)
                .orElseThrow(() -> new IllegalArgumentException("Payment button not found: " + buttonCode));
        
        // Increment click count
        button.incrementClicks();
        buttonRepository.save(button);
        
        return button;
    }

    /**
     * Get payment buttons by merchant
     * 
     * @param merchant Merchant user
     * @return List of payment buttons
     */
    @Transactional(readOnly = true)
    public List<PaymentButton> getPaymentButtonsByMerchant(User merchant) {
        return buttonRepository.findByMerchantOrderByCreatedAtDesc(merchant);
    }

    /**
     * Get payment button by ID
     * 
     * @param buttonId Button ID
     * @return PaymentButton entity
     */
    @Transactional(readOnly = true)
    public PaymentButton getPaymentButtonById(Long buttonId) {
        return buttonRepository.findById(buttonId)
                .orElseThrow(() -> new IllegalArgumentException("Payment button not found: " + buttonId));
    }

    /**
     * Update payment button
     * 
     * @param button Payment button
     * @return Updated button
     */
    public PaymentButton updatePaymentButton(PaymentButton button) {
        return buttonRepository.save(button);
    }

    /**
     * Generate embed code for payment button
     * 
     * @param button Payment button
     * @param baseUrl Base URL
     * @return Embed code HTML
     */
    public String generateEmbedCode(PaymentButton button, String baseUrl) {
        String checkoutUrl = baseUrl + "/checkout/" + button.getButtonCode();
        
        StringBuilder embedCode = new StringBuilder();
        embedCode.append("<!-- Zim PayPal Payment Button -->\n");
        embedCode.append("<div id=\"zim-paypal-button-").append(button.getButtonCode()).append("\">\n");
        embedCode.append("  <a href=\"").append(checkoutUrl).append("\" ");
        embedCode.append("style=\"display: inline-block; padding: 12px 24px; ");
        embedCode.append("background-color: ").append(button.getButtonColor()).append("; ");
        embedCode.append("color: white; text-decoration: none; border-radius: 4px; ");
        embedCode.append("font-weight: bold; font-size: 16px;\" ");
        embedCode.append("target=\"_blank\">\n");
        embedCode.append("    Pay with Zim PayPal\n");
        embedCode.append("  </a>\n");
        embedCode.append("</div>\n");
        embedCode.append("<!-- End Zim PayPal Payment Button -->");
        
        return embedCode.toString();
    }

    /**
     * Generate unique button code
     * 
     * @return Button code
     */
    private String generateUniqueButtonCode() {
        String buttonCode;
        do {
            buttonCode = "BTN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (buttonRepository.findByButtonCode(buttonCode).isPresent());
        return buttonCode;
    }
}

