package com.zim.paypal.service;

import com.zim.paypal.model.dto.InvoiceDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for invoice management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CurrencyService currencyService;
    private final UserService userService;
    private final TransactionService transactionService;
    private final EmailService emailService;

    /**
     * Create a new invoice
     * 
     * @param invoiceDto Invoice DTO
     * @param merchant Merchant user
     * @return Created invoice
     */
    public Invoice createInvoice(InvoiceDto invoiceDto, User merchant) {
        String invoiceNumber = generateInvoiceNumber();
        
        Currency currency = null;
        String currencyCode = "USD";
        if (invoiceDto.getCurrencyId() != null) {
            currency = currencyService.getCurrencyById(invoiceDto.getCurrencyId());
            currencyCode = currency.getCurrencyCode();
        } else {
            currency = currencyService.getBaseCurrency();
            currencyCode = currency.getCurrencyCode();
        }

        // Try to find customer by email
        User customer = null;
        try {
            customer = userService.findByEmail(invoiceDto.getCustomerEmail());
        } catch (Exception e) {
            // Customer doesn't exist - invoice can still be created
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .merchant(merchant)
                .customer(customer)
                .customerEmail(invoiceDto.getCustomerEmail())
                .customerName(invoiceDto.getCustomerName())
                .customerAddress(invoiceDto.getCustomerAddress())
                .invoiceDate(invoiceDto.getInvoiceDate())
                .dueDate(invoiceDto.getDueDate())
                .status(Invoice.InvoiceStatus.DRAFT)
                .taxAmount(invoiceDto.getTaxAmount())
                .discountAmount(invoiceDto.getDiscountAmount())
                .totalAmount(invoiceDto.getTotalAmount())
                .currency(currency)
                .currencyCode(currencyCode)
                .notes(invoiceDto.getNotes())
                .terms(invoiceDto.getTerms())
                .build();

        // Add invoice items
        if (invoiceDto.getItems() != null && !invoiceDto.getItems().isEmpty()) {
            for (com.zim.paypal.model.dto.InvoiceItemDto itemDto : invoiceDto.getItems()) {
                InvoiceItem item = InvoiceItem.builder()
                        .invoice(invoice)
                        .description(itemDto.getDescription())
                        .quantity(itemDto.getQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .taxRate(itemDto.getTaxRate())
                        .build();
                item.calculatePrice();
                invoice.getItems().add(item);
            }
            invoice.calculateTotal();
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created: {} by merchant: {}", invoiceNumber, merchant.getUsername());
        return saved;
    }

    /**
     * Send invoice to customer
     * 
     * @param invoiceId Invoice ID
     * @return Updated invoice
     */
    public Invoice sendInvoice(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be sent");
        }

        invoice.setStatus(Invoice.InvoiceStatus.SENT);
        Invoice saved = invoiceRepository.save(invoice);

        // Send email to customer
        String emailBody = buildInvoiceEmail(invoice);
        emailService.sendEmail(invoice.getCustomerEmail(),
                "Invoice #" + invoice.getInvoiceNumber(),
                emailBody);

        log.info("Invoice sent: {} to: {}", invoice.getInvoiceNumber(), invoice.getCustomerEmail());
        return saved;
    }

    /**
     * Pay invoice
     * 
     * @param invoiceId Invoice ID
     * @param payerId Payer user ID
     * @return Transaction entity
     */
    public Transaction payInvoice(Long invoiceId, Long payerId) {
        Invoice invoice = getInvoiceById(invoiceId);
        User payer = userService.findById(payerId);

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already paid");
        }

        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice is cancelled");
        }

        // Process payment
        Transaction transaction = transactionService.createPaymentFromWallet(
                payerId, invoice.getTotalAmount(), 
                "Payment for invoice: " + invoice.getInvoiceNumber(),
                invoice.getMerchant().getId());

        // Link transaction to invoice
        invoice.getTransactions().add(transaction);
        invoice.markAsPaid();
        invoiceRepository.save(invoice);

        // Send confirmation emails
        emailService.sendEmail(payer.getEmail(),
                "Payment Confirmation - Invoice #" + invoice.getInvoiceNumber(),
                "Your payment of " + invoice.getTotalAmount() + " " + invoice.getCurrencyCode() + 
                " for invoice #" + invoice.getInvoiceNumber() + " has been processed.");

        emailService.sendEmail(invoice.getMerchant().getEmail(),
                "Invoice Paid - Invoice #" + invoice.getInvoiceNumber(),
                "Invoice #" + invoice.getInvoiceNumber() + " has been paid by " + payer.getEmail());

        log.info("Invoice paid: {} by user: {}", invoice.getInvoiceNumber(), payer.getUsername());
        return transaction;
    }

    /**
     * Get invoice by ID
     * 
     * @param invoiceId Invoice ID
     * @return Invoice entity
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
    }

    /**
     * Get invoice by invoice number
     * 
     * @param invoiceNumber Invoice number
     * @return Invoice entity
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceNumber));
    }

    /**
     * Generate unique invoice number
     * 
     * @return Invoice number
     */
    private String generateInvoiceNumber() {
        String invoiceNumber;
        do {
            invoiceNumber = "INV-" + LocalDate.now().getYear() + "-" + 
                    UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (invoiceRepository.findByInvoiceNumber(invoiceNumber).isPresent());
        return invoiceNumber;
    }

    /**
     * Get invoices by merchant
     * 
     * @param merchant Merchant user
     * @param pageable Pageable object
     * @return Page of invoices
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Invoice> getInvoicesByMerchant(
            User merchant, org.springframework.data.domain.Pageable pageable) {
        return invoiceRepository.findByMerchantOrderByCreatedAtDesc(merchant, pageable);
    }

    /**
     * Get invoices by customer
     * 
     * @param customer Customer user
     * @param pageable Pageable object
     * @return Page of invoices
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Invoice> getInvoicesByCustomer(
            User customer, org.springframework.data.domain.Pageable pageable) {
        return invoiceRepository.findByCustomerOrderByCreatedAtDesc(customer, pageable);
    }

    /**
     * Build invoice email body
     */
    private String buildInvoiceEmail(Invoice invoice) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(invoice.getCustomerName() != null ? invoice.getCustomerName() : "Customer").append(",\n\n");
        body.append("Please find your invoice details below:\n\n");
        body.append("Invoice Number: ").append(invoice.getInvoiceNumber()).append("\n");
        body.append("Invoice Date: ").append(invoice.getInvoiceDate()).append("\n");
        if (invoice.getDueDate() != null) {
            body.append("Due Date: ").append(invoice.getDueDate()).append("\n");
        }
        body.append("Total Amount: ").append(invoice.getTotalAmount()).append(" ").append(invoice.getCurrencyCode()).append("\n\n");
        body.append("Items:\n");
        for (InvoiceItem item : invoice.getItems()) {
            body.append("- ").append(item.getDescription())
                .append(" (Qty: ").append(item.getQuantity())
                .append(", Price: ").append(item.getPrice())
                .append(")\n");
        }
        body.append("\nThank you for your business!");
        return body.toString();
    }
}

