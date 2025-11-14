package com.zim.paypal.service;

import com.zim.paypal.model.dto.CurrencyDto;
import com.zim.paypal.model.entity.Currency;
import com.zim.paypal.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for currency management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    /**
     * Create a new currency
     * 
     * @param currencyDto Currency DTO
     * @param createdBy User who created the currency
     * @return Created currency
     */
    public Currency createCurrency(CurrencyDto currencyDto, com.zim.paypal.model.entity.User createdBy) {
        // Check if currency code already exists
        if (currencyRepository.findByCurrencyCode(currencyDto.getCurrencyCode()).isPresent()) {
            throw new IllegalArgumentException("Currency code already exists: " + currencyDto.getCurrencyCode());
        }

        // If setting as base currency, unset other base currencies
        if (currencyDto.getIsBaseCurrency()) {
            currencyRepository.findByIsBaseCurrencyTrue().ifPresent(existingBase -> {
                existingBase.setIsBaseCurrency(false);
                currencyRepository.save(existingBase);
            });
        }

        Currency currency = Currency.builder()
                .currencyCode(currencyDto.getCurrencyCode())
                .currencyName(currencyDto.getCurrencyName())
                .symbol(currencyDto.getSymbol())
                .isActive(currencyDto.getIsActive())
                .isBaseCurrency(currencyDto.getIsBaseCurrency())
                .decimalPlaces(currencyDto.getDecimalPlaces())
                .createdBy(createdBy)
                .build();

        Currency savedCurrency = currencyRepository.save(currency);
        log.info("Currency created: {} by user: {}", currencyDto.getCurrencyCode(), createdBy.getUsername());
        return savedCurrency;
    }

    /**
     * Update an existing currency
     * 
     * @param currencyId Currency ID
     * @param currencyDto Currency DTO
     * @param updatedBy User who updated the currency
     * @return Updated currency
     */
    public Currency updateCurrency(Long currencyId, CurrencyDto currencyDto, com.zim.paypal.model.entity.User updatedBy) {
        Currency currency = currencyRepository.findById(currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + currencyId));

        // Check if currency code is being changed and if new code already exists
        if (!currency.getCurrencyCode().equals(currencyDto.getCurrencyCode())) {
            if (currencyRepository.findByCurrencyCode(currencyDto.getCurrencyCode()).isPresent()) {
                throw new IllegalArgumentException("Currency code already exists: " + currencyDto.getCurrencyCode());
            }
        }

        // If setting as base currency, unset other base currencies
        if (currencyDto.getIsBaseCurrency() && !currency.getIsBaseCurrency()) {
            currencyRepository.findByIsBaseCurrencyTrue().ifPresent(existingBase -> {
                existingBase.setIsBaseCurrency(false);
                currencyRepository.save(existingBase);
            });
        }

        currency.setCurrencyCode(currencyDto.getCurrencyCode());
        currency.setCurrencyName(currencyDto.getCurrencyName());
        currency.setSymbol(currencyDto.getSymbol());
        currency.setIsActive(currencyDto.getIsActive());
        currency.setIsBaseCurrency(currencyDto.getIsBaseCurrency());
        currency.setDecimalPlaces(currencyDto.getDecimalPlaces());
        currency.setUpdatedBy(updatedBy);

        Currency savedCurrency = currencyRepository.save(currency);
        log.info("Currency updated: {} by user: {}", currencyDto.getCurrencyCode(), updatedBy.getUsername());
        return savedCurrency;
    }

    /**
     * Get currency by ID
     * 
     * @param currencyId Currency ID
     * @return Currency entity
     */
    @Transactional(readOnly = true)
    public Currency getCurrencyById(Long currencyId) {
        return currencyRepository.findById(currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + currencyId));
    }

    /**
     * Get currency by code
     * 
     * @param currencyCode Currency code
     * @return Currency entity
     */
    @Transactional(readOnly = true)
    public Currency getCurrencyByCode(String currencyCode) {
        return currencyRepository.findByCurrencyCode(currencyCode)
                .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + currencyCode));
    }

    /**
     * Get all active currencies
     * 
     * @return List of currencies
     */
    @Transactional(readOnly = true)
    public List<Currency> getAllActiveCurrencies() {
        return currencyRepository.findByIsActiveTrue();
    }

    /**
     * Get all currencies
     * 
     * @return List of currencies
     */
    @Transactional(readOnly = true)
    public List<Currency> getAllCurrencies() {
        return currencyRepository.findAll();
    }

    /**
     * Get base currency
     * 
     * @return Currency entity
     */
    @Transactional(readOnly = true)
    public Currency getBaseCurrency() {
        return currencyRepository.findByIsBaseCurrencyTrue()
                .orElseThrow(() -> new IllegalStateException("No base currency configured"));
    }
}

