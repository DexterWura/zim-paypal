package com.zim.paypal.service;

import com.zim.paypal.model.dto.PaymentLinkDto;
import com.zim.paypal.model.entity.Currency;
import com.zim.paypal.model.entity.PaymentLink;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.PaymentLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for payment link management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentLinkService {

    private final PaymentLinkRepository paymentLinkRepository;
    private final CurrencyService currencyService;

    /**
     * Create a new payment link
     * 
     * @param linkDto Payment link DTO
     * @param creator Creator user
     * @return Created payment link
     */
    public PaymentLink createPaymentLink(PaymentLinkDto linkDto, User creator) {
        String linkCode = generateUniqueLinkCode();
        
        Currency currency = null;
        String currencyCode = "USD";
        if (linkDto.getCurrencyId() != null) {
            currency = currencyService.getCurrencyById(linkDto.getCurrencyId());
            currencyCode = currency.getCurrencyCode();
        } else {
            currency = currencyService.getBaseCurrency();
            currencyCode = currency.getCurrencyCode();
        }

        PaymentLink paymentLink = PaymentLink.builder()
                .linkCode(linkCode)
                .creator(creator)
                .title(linkDto.getTitle())
                .description(linkDto.getDescription())
                .amount(linkDto.getAmount())
                .currency(currency)
                .currencyCode(currencyCode)
                .status(PaymentLink.Status.ACTIVE)
                .linkType(linkDto.getLinkType())
                .maxUses(linkDto.getMaxUses())
                .expiresAt(linkDto.getExpiresAt())
                .allowPartialPayment(linkDto.getAllowPartialPayment())
                .collectShippingAddress(linkDto.getCollectShippingAddress())
                .collectPhoneNumber(linkDto.getCollectPhoneNumber())
                .emailNotification(linkDto.getEmailNotification())
                .returnUrl(linkDto.getReturnUrl())
                .cancelUrl(linkDto.getCancelUrl())
                .imageUrl(linkDto.getImageUrl())
                .build();

        PaymentLink saved = paymentLinkRepository.save(paymentLink);
        log.info("Payment link created: {} by user: {}", linkCode, creator.getUsername());
        return saved;
    }

    /**
     * Get payment link by code
     * 
     * @param linkCode Link code
     * @return PaymentLink entity
     */
    @Transactional(readOnly = true)
    public PaymentLink getPaymentLinkByCode(String linkCode) {
        return paymentLinkRepository.findByLinkCode(linkCode)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + linkCode));
    }

    /**
     * Get payment link by ID
     * 
     * @param linkId Link ID
     * @return PaymentLink entity
     */
    @Transactional(readOnly = true)
    public PaymentLink getPaymentLinkById(Long linkId) {
        return paymentLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + linkId));
    }

    /**
     * Get payment links by creator
     * 
     * @param creator Creator user
     * @param pageable Pageable object
     * @return Page of payment links
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PaymentLink> getPaymentLinksByCreator(
            User creator, org.springframework.data.domain.Pageable pageable) {
        return paymentLinkRepository.findByCreatorOrderByCreatedAtDesc(creator, pageable);
    }

    /**
     * Process payment through payment link
     * 
     * @param linkCode Link code
     * @param payerEmail Payer email
     * @param amount Payment amount (can be partial)
     * @return Transaction entity
     */
    public com.zim.paypal.model.entity.Transaction processPaymentLink(String linkCode, 
                                                                      String payerEmail, 
                                                                      java.math.BigDecimal amount) {
        PaymentLink paymentLink = getPaymentLinkByCode(linkCode);
        
        if (!paymentLink.isValid()) {
            throw new IllegalStateException("Payment link is no longer valid");
        }

        // Find or create payer user
        com.zim.paypal.model.entity.User payer = null;
        try {
            payer = userService.findByEmail(payerEmail);
        } catch (Exception e) {
            // Payer doesn't exist - in production, might create guest user or require registration
            throw new IllegalArgumentException("Payer must have an account to pay");
        }

        // Check if partial payment is allowed
        if (!paymentLink.getAllowPartialPayment() && 
            amount.compareTo(paymentLink.getAmount()) != 0) {
            throw new IllegalArgumentException("Partial payment not allowed for this link");
        }

        // Process payment
        com.zim.paypal.model.entity.Transaction transaction = transactionService.createPaymentFromWallet(
                payer.getId(), amount, "Payment via link: " + paymentLink.getTitle(), 
                paymentLink.getCreator().getId());

        // Update payment link
        paymentLink.incrementUses();
        paymentLinkRepository.save(paymentLink);

        // Send notification to creator
        if (paymentLink.getEmailNotification()) {
            emailService.sendEmail(paymentLink.getCreator().getEmail(),
                    "Payment Received via Payment Link",
                    "You received a payment of " + amount + " " + paymentLink.getCurrencyCode() + 
                    " via payment link: " + paymentLink.getTitle());
        }

        log.info("Payment processed via link: {} by user: {}", linkCode, payer.getUsername());
        return transaction;
    }

    /**
     * Generate unique link code
     * 
     * @return Link code
     */
    private String generateUniqueLinkCode() {
        String linkCode;
        do {
            linkCode = "PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (paymentLinkRepository.findByLinkCode(linkCode).isPresent());
        return linkCode;
    }

    // Inject required services
    private final UserService userService;
    private final TransactionService transactionService;
    private final EmailService emailService;
}

