package com.zim.paypal.service;

import com.zim.paypal.model.dto.ChargeDto;
import com.zim.paypal.model.entity.Charge;
import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.repository.ChargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for charge management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChargeService {

    private final ChargeRepository chargeRepository;

    /**
     * Create a new charge
     * 
     * @param chargeDto Charge DTO
     * @param createdBy User who created the charge
     * @return Created charge
     */
    public Charge createCharge(ChargeDto chargeDto, com.zim.paypal.model.entity.User createdBy) {
        // Check if charge code already exists
        if (chargeRepository.findByChargeCode(chargeDto.getChargeCode()).isPresent()) {
            throw new IllegalArgumentException("Charge code already exists: " + chargeDto.getChargeCode());
        }

        Charge charge = Charge.builder()
                .chargeName(chargeDto.getChargeName())
                .chargeCode(chargeDto.getChargeCode())
                .chargeType(chargeDto.getChargeType())
                .transactionType(chargeDto.getTransactionType())
                .chargeMethod(chargeDto.getChargeMethod())
                .fixedAmount(chargeDto.getFixedAmount())
                .percentageRate(chargeDto.getPercentageRate())
                .minAmount(chargeDto.getMinAmount())
                .maxAmount(chargeDto.getMaxAmount())
                .isActive(chargeDto.getIsActive())
                .description(chargeDto.getDescription())
                .regulationReference(chargeDto.getRegulationReference())
                .createdBy(createdBy)
                .build();

        Charge savedCharge = chargeRepository.save(charge);
        log.info("Charge created: {} by user: {}", chargeDto.getChargeCode(), createdBy.getUsername());
        return savedCharge;
    }

    /**
     * Update an existing charge
     * 
     * @param chargeId Charge ID
     * @param chargeDto Charge DTO
     * @param updatedBy User who updated the charge
     * @return Updated charge
     */
    public Charge updateCharge(Long chargeId, ChargeDto chargeDto, com.zim.paypal.model.entity.User updatedBy) {
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found: " + chargeId));

        // Check if charge code is being changed and if new code already exists
        if (!charge.getChargeCode().equals(chargeDto.getChargeCode())) {
            if (chargeRepository.findByChargeCode(chargeDto.getChargeCode()).isPresent()) {
                throw new IllegalArgumentException("Charge code already exists: " + chargeDto.getChargeCode());
            }
        }

        charge.setChargeName(chargeDto.getChargeName());
        charge.setChargeCode(chargeDto.getChargeCode());
        charge.setChargeType(chargeDto.getChargeType());
        charge.setTransactionType(chargeDto.getTransactionType());
        charge.setChargeMethod(chargeDto.getChargeMethod());
        charge.setFixedAmount(chargeDto.getFixedAmount());
        charge.setPercentageRate(chargeDto.getPercentageRate());
        charge.setMinAmount(chargeDto.getMinAmount());
        charge.setMaxAmount(chargeDto.getMaxAmount());
        charge.setIsActive(chargeDto.getIsActive());
        charge.setDescription(chargeDto.getDescription());
        charge.setRegulationReference(chargeDto.getRegulationReference());
        charge.setUpdatedBy(updatedBy);

        Charge savedCharge = chargeRepository.save(charge);
        log.info("Charge updated: {} by user: {}", chargeDto.getChargeCode(), updatedBy.getUsername());
        return savedCharge;
    }

    /**
     * Get charge by ID
     * 
     * @param chargeId Charge ID
     * @return Charge entity
     */
    @Transactional(readOnly = true)
    public Charge getChargeById(Long chargeId) {
        return chargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found: " + chargeId));
    }

    /**
     * Get all active charges
     * 
     * @return List of charges
     */
    @Transactional(readOnly = true)
    public List<Charge> getAllActiveCharges() {
        return chargeRepository.findByIsActiveTrue();
    }

    /**
     * Get all charges
     * 
     * @return List of charges
     */
    @Transactional(readOnly = true)
    public List<Charge> getAllCharges() {
        return chargeRepository.findAll();
    }

    /**
     * Get charges applicable to a transaction type
     * 
     * @param transactionType Transaction type
     * @return List of charges
     */
    @Transactional(readOnly = true)
    public List<Charge> getChargesForTransaction(Transaction.TransactionType transactionType) {
        return chargeRepository.findByTransactionTypeAndIsActiveTrue(transactionType);
    }

    /**
     * Calculate total charges for a transaction
     * 
     * @param transactionType Transaction type
     * @param transactionAmount Transaction amount
     * @return Total charge amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalCharges(Transaction.TransactionType transactionType, BigDecimal transactionAmount) {
        List<Charge> charges = getChargesForTransaction(transactionType);
        BigDecimal totalCharges = BigDecimal.ZERO;

        for (Charge charge : charges) {
            BigDecimal chargeAmount = charge.calculateCharge(transactionAmount);
            totalCharges = totalCharges.add(chargeAmount);
        }

        return totalCharges;
    }

    /**
     * Delete a charge (soft delete by deactivating)
     * 
     * @param chargeId Charge ID
     * @param updatedBy User who deleted the charge
     */
    public void deleteCharge(Long chargeId, com.zim.paypal.model.entity.User updatedBy) {
        Charge charge = getChargeById(chargeId);
        charge.setIsActive(false);
        charge.setUpdatedBy(updatedBy);
        chargeRepository.save(charge);
        log.info("Charge deactivated: {} by user: {}", charge.getChargeCode(), updatedBy.getUsername());
    }
}

