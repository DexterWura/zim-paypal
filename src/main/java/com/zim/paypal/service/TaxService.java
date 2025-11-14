package com.zim.paypal.service;

import com.zim.paypal.model.dto.TaxDto;
import com.zim.paypal.model.entity.Tax;
import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.repository.TaxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for tax management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaxService {

    private final TaxRepository taxRepository;

    /**
     * Create a new tax
     * 
     * @param taxDto Tax DTO
     * @param createdBy User who created the tax
     * @return Created tax
     */
    public Tax createTax(TaxDto taxDto, com.zim.paypal.model.entity.User createdBy) {
        // Check if tax code already exists
        if (taxRepository.findByTaxCode(taxDto.getTaxCode()).isPresent()) {
            throw new IllegalArgumentException("Tax code already exists: " + taxDto.getTaxCode());
        }

        Tax tax = Tax.builder()
                .taxName(taxDto.getTaxName())
                .taxCode(taxDto.getTaxCode())
                .taxType(taxDto.getTaxType())
                .taxRate(taxDto.getTaxRate())
                .transactionType(taxDto.getTransactionType())
                .isActive(taxDto.getIsActive())
                .description(taxDto.getDescription())
                .regulationReference(taxDto.getRegulationReference())
                .effectiveFrom(taxDto.getEffectiveFrom())
                .effectiveTo(taxDto.getEffectiveTo())
                .createdBy(createdBy)
                .build();

        Tax savedTax = taxRepository.save(tax);
        log.info("Tax created: {} by user: {}", taxDto.getTaxCode(), createdBy.getUsername());
        return savedTax;
    }

    /**
     * Update an existing tax
     * 
     * @param taxId Tax ID
     * @param taxDto Tax DTO
     * @param updatedBy User who updated the tax
     * @return Updated tax
     */
    public Tax updateTax(Long taxId, TaxDto taxDto, com.zim.paypal.model.entity.User updatedBy) {
        Tax tax = taxRepository.findById(taxId)
                .orElseThrow(() -> new IllegalArgumentException("Tax not found: " + taxId));

        // Check if tax code is being changed and if new code already exists
        if (!tax.getTaxCode().equals(taxDto.getTaxCode())) {
            if (taxRepository.findByTaxCode(taxDto.getTaxCode()).isPresent()) {
                throw new IllegalArgumentException("Tax code already exists: " + taxDto.getTaxCode());
            }
        }

        tax.setTaxName(taxDto.getTaxName());
        tax.setTaxCode(taxDto.getTaxCode());
        tax.setTaxType(taxDto.getTaxType());
        tax.setTaxRate(taxDto.getTaxRate());
        tax.setTransactionType(taxDto.getTransactionType());
        tax.setIsActive(taxDto.getIsActive());
        tax.setDescription(taxDto.getDescription());
        tax.setRegulationReference(taxDto.getRegulationReference());
        tax.setEffectiveFrom(taxDto.getEffectiveFrom());
        tax.setEffectiveTo(taxDto.getEffectiveTo());
        tax.setUpdatedBy(updatedBy);

        Tax savedTax = taxRepository.save(tax);
        log.info("Tax updated: {} by user: {}", taxDto.getTaxCode(), updatedBy.getUsername());
        return savedTax;
    }

    /**
     * Get tax by ID
     * 
     * @param taxId Tax ID
     * @return Tax entity
     */
    @Transactional(readOnly = true)
    public Tax getTaxById(Long taxId) {
        return taxRepository.findById(taxId)
                .orElseThrow(() -> new IllegalArgumentException("Tax not found: " + taxId));
    }

    /**
     * Get all active taxes
     * 
     * @return List of taxes
     */
    @Transactional(readOnly = true)
    public List<Tax> getAllActiveTaxes() {
        return taxRepository.findByIsActiveTrue();
    }

    /**
     * Get all taxes
     * 
     * @return List of taxes
     */
    @Transactional(readOnly = true)
    public List<Tax> getAllTaxes() {
        return taxRepository.findAll();
    }

    /**
     * Get effective taxes for a transaction type
     * 
     * @param transactionType Transaction type
     * @return List of effective taxes
     */
    @Transactional(readOnly = true)
    public List<Tax> getEffectiveTaxesForTransaction(Transaction.TransactionType transactionType) {
        return taxRepository.findEffectiveTaxesByTransactionType(transactionType, LocalDateTime.now());
    }

    /**
     * Calculate total taxes for a transaction
     * 
     * @param transactionType Transaction type
     * @param transactionAmount Transaction amount
     * @return Total tax amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalTaxes(Transaction.TransactionType transactionType, BigDecimal transactionAmount) {
        List<Tax> taxes = getEffectiveTaxesForTransaction(transactionType);
        BigDecimal totalTaxes = BigDecimal.ZERO;

        for (Tax tax : taxes) {
            if (tax.isEffective()) {
                BigDecimal taxAmount = tax.calculateTax(transactionAmount);
                totalTaxes = totalTaxes.add(taxAmount);
            }
        }

        return totalTaxes;
    }

    /**
     * Delete a tax (soft delete by deactivating)
     * 
     * @param taxId Tax ID
     * @param updatedBy User who deleted the tax
     */
    public void deleteTax(Long taxId, com.zim.paypal.model.entity.User updatedBy) {
        Tax tax = getTaxById(taxId);
        tax.setIsActive(false);
        tax.setUpdatedBy(updatedBy);
        taxRepository.save(tax);
        log.info("Tax deactivated: {} by user: {}", tax.getTaxCode(), updatedBy.getUsername());
    }
}

