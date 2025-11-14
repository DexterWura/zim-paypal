package com.zim.paypal.service;

import com.zim.paypal.model.dto.ExchangeRateDto;
import com.zim.paypal.model.entity.Currency;
import com.zim.paypal.model.entity.ExchangeRate;
import com.zim.paypal.repository.CurrencyRepository;
import com.zim.paypal.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for exchange rate management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExchangeRateService {

    private final ExchangeRateRepository rateRepository;
    private final CurrencyRepository currencyRepository;

    /**
     * Create a new exchange rate
     * 
     * @param rateDto Exchange rate DTO
     * @param createdBy User who created the rate
     * @return Created exchange rate
     */
    public ExchangeRate createExchangeRate(ExchangeRateDto rateDto, com.zim.paypal.model.entity.User createdBy) {
        Currency fromCurrency = currencyRepository.findById(rateDto.getFromCurrencyId())
                .orElseThrow(() -> new IllegalArgumentException("From currency not found"));
        Currency toCurrency = currencyRepository.findById(rateDto.getToCurrencyId())
                .orElseThrow(() -> new IllegalArgumentException("To currency not found"));

        if (fromCurrency.getId().equals(toCurrency.getId())) {
            throw new IllegalArgumentException("From and to currencies cannot be the same");
        }

        ExchangeRate rate = ExchangeRate.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(rateDto.getRate())
                .effectiveFrom(rateDto.getEffectiveFrom())
                .effectiveTo(rateDto.getEffectiveTo())
                .isActive(rateDto.getIsActive())
                .createdBy(createdBy)
                .build();

        ExchangeRate savedRate = rateRepository.save(rate);
        log.info("Exchange rate created: {} to {} by user: {}", 
                fromCurrency.getCurrencyCode(), toCurrency.getCurrencyCode(), createdBy.getUsername());
        return savedRate;
    }

    /**
     * Update an existing exchange rate
     * 
     * @param rateId Rate ID
     * @param rateDto Exchange rate DTO
     * @param updatedBy User who updated the rate
     * @return Updated exchange rate
     */
    public ExchangeRate updateExchangeRate(Long rateId, ExchangeRateDto rateDto, com.zim.paypal.model.entity.User updatedBy) {
        ExchangeRate rate = rateRepository.findById(rateId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange rate not found: " + rateId));

        Currency fromCurrency = currencyRepository.findById(rateDto.getFromCurrencyId())
                .orElseThrow(() -> new IllegalArgumentException("From currency not found"));
        Currency toCurrency = currencyRepository.findById(rateDto.getToCurrencyId())
                .orElseThrow(() -> new IllegalArgumentException("To currency not found"));

        if (fromCurrency.getId().equals(toCurrency.getId())) {
            throw new IllegalArgumentException("From and to currencies cannot be the same");
        }

        rate.setFromCurrency(fromCurrency);
        rate.setToCurrency(toCurrency);
        rate.setRate(rateDto.getRate());
        rate.setEffectiveFrom(rateDto.getEffectiveFrom());
        rate.setEffectiveTo(rateDto.getEffectiveTo());
        rate.setIsActive(rateDto.getIsActive());
        rate.setUpdatedBy(updatedBy);

        ExchangeRate savedRate = rateRepository.save(rate);
        log.info("Exchange rate updated: {} to {} by user: {}", 
                fromCurrency.getCurrencyCode(), toCurrency.getCurrencyCode(), updatedBy.getUsername());
        return savedRate;
    }

    /**
     * Get exchange rate between two currencies
     * 
     * @param fromCurrencyCode From currency code
     * @param toCurrencyCode To currency code
     * @return Exchange rate
     */
    @Transactional(readOnly = true)
    public BigDecimal getExchangeRate(String fromCurrencyCode, String toCurrencyCode) {
        if (fromCurrencyCode.equals(toCurrencyCode)) {
            return BigDecimal.ONE;
        }

        Currency fromCurrency = currencyRepository.findByCurrencyCode(fromCurrencyCode)
                .orElseThrow(() -> new IllegalArgumentException("From currency not found: " + fromCurrencyCode));
        Currency toCurrency = currencyRepository.findByCurrencyCode(toCurrencyCode)
                .orElseThrow(() -> new IllegalArgumentException("To currency not found: " + toCurrencyCode));

        ExchangeRate rate = rateRepository.findEffectiveRate(fromCurrency, toCurrency, LocalDateTime.now())
                .orElseThrow(() -> new IllegalStateException(
                        "No effective exchange rate found from " + fromCurrencyCode + " to " + toCurrencyCode));

        return rate.getRate();
    }

    /**
     * Convert amount from one currency to another
     * 
     * @param amount Amount to convert
     * @param fromCurrencyCode From currency code
     * @param toCurrencyCode To currency code
     * @return Converted amount
     */
    @Transactional(readOnly = true)
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrencyCode, String toCurrencyCode) {
        if (fromCurrencyCode.equals(toCurrencyCode)) {
            return amount;
        }

        BigDecimal rate = getExchangeRate(fromCurrencyCode, toCurrencyCode);
        return amount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Get all effective exchange rates
     * 
     * @return List of rates
     */
    @Transactional(readOnly = true)
    public List<ExchangeRate> getAllEffectiveRates() {
        return rateRepository.findEffectiveRates(LocalDateTime.now());
    }

    /**
     * Get all exchange rates
     * 
     * @return List of rates
     */
    @Transactional(readOnly = true)
    public List<ExchangeRate> getAllRates() {
        return rateRepository.findAll();
    }

    /**
     * Get rate by ID
     * 
     * @param rateId Rate ID
     * @return ExchangeRate entity
     */
    @Transactional(readOnly = true)
    public ExchangeRate getRateById(Long rateId) {
        return rateRepository.findById(rateId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange rate not found: " + rateId));
    }
}

