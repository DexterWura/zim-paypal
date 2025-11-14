package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Invoice;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Invoice entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Find invoice by invoice number
     * 
     * @param invoiceNumber Invoice number
     * @return Optional Invoice
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoices by merchant
     * 
     * @param merchant Merchant user
     * @param pageable Pageable object
     * @return Page of invoices
     */
    Page<Invoice> findByMerchantOrderByCreatedAtDesc(User merchant, Pageable pageable);

    /**
     * Find invoices by customer
     * 
     * @param customer Customer user
     * @param pageable Pageable object
     * @return Page of invoices
     */
    Page<Invoice> findByCustomerOrderByCreatedAtDesc(User customer, Pageable pageable);

    /**
     * Find invoices by merchant and status
     * 
     * @param merchant Merchant user
     * @param status Invoice status
     * @param pageable Pageable object
     * @return Page of invoices
     */
    Page<Invoice> findByMerchantAndStatusOrderByCreatedAtDesc(User merchant, 
                                                              Invoice.InvoiceStatus status, 
                                                              Pageable pageable);

    /**
     * Find overdue invoices
     * 
     * @param today Today's date
     * @return List of overdue invoices
     */
    List<Invoice> findByDueDateBeforeAndStatusNotIn(LocalDate today, 
                                                     List<Invoice.InvoiceStatus> excludedStatuses);
}

