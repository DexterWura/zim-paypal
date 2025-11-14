package com.zim.paypal.repository;

import com.zim.paypal.model.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for InvoiceItem entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    /**
     * Find items by invoice
     * 
     * @param invoiceId Invoice ID
     * @return List of invoice items
     */
    List<InvoiceItem> findByInvoiceId(Long invoiceId);
}

