package com.zim.paypal.service;

import com.zim.paypal.model.entity.CountryRestriction;
import com.zim.paypal.repository.CountryRestrictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for country restriction management
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CountryRestrictionService {

    private final CountryRestrictionRepository countryRestrictionRepository;

    /**
     * Check if country is enabled
     * 
     * @param countryCode Country code (ISO 3166-1 alpha-2)
     * @return true if enabled, false otherwise
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "countryRestrictions", key = "#countryCode")
    public boolean isCountryEnabled(String countryCode) {
        return countryRestrictionRepository.findByCountryCode(countryCode.toUpperCase())
                .map(CountryRestriction::getIsEnabled)
                .orElse(false); // Default to disabled if not found
    }

    /**
     * Check if registration is allowed for country
     * 
     * @param countryCode Country code
     * @return true if registration allowed, false otherwise
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "countryRestrictions", key = "'REG_' + #countryCode")
    public boolean isRegistrationAllowed(String countryCode) {
        return countryRestrictionRepository.findByCountryCode(countryCode.toUpperCase())
                .map(c -> c.getIsEnabled() && c.getIsRegistrationAllowed())
                .orElse(false);
    }

    /**
     * Check if transactions are allowed for country
     * 
     * @param countryCode Country code
     * @return true if transactions allowed, false otherwise
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "countryRestrictions", key = "'TXN_' + #countryCode")
    public boolean isTransactionAllowed(String countryCode) {
        return countryRestrictionRepository.findByCountryCode(countryCode.toUpperCase())
                .map(c -> c.getIsEnabled() && c.getIsTransactionAllowed())
                .orElse(false);
    }

    /**
     * Check if merchant features are allowed for country
     * 
     * @param countryCode Country code
     * @return true if merchant allowed, false otherwise
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "countryRestrictions", key = "'MER_' + #countryCode")
    public boolean isMerchantAllowed(String countryCode) {
        return countryRestrictionRepository.findByCountryCode(countryCode.toUpperCase())
                .map(c -> c.getIsEnabled() && c.getIsMerchantAllowed())
                .orElse(false);
    }

    /**
     * Get country restriction by code
     * 
     * @param countryCode Country code
     * @return CountryRestriction entity
     */
    @Transactional(readOnly = true)
    public CountryRestriction getCountryRestriction(String countryCode) {
        return countryRestrictionRepository.findByCountryCode(countryCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Country restriction not found: " + countryCode));
    }

    /**
     * Get all country restrictions
     * 
     * @return List of all country restrictions
     */
    @Transactional(readOnly = true)
    public List<CountryRestriction> getAllCountryRestrictions() {
        return countryRestrictionRepository.findAll();
    }

    /**
     * Get enabled countries
     * 
     * @return List of enabled countries
     */
    @Transactional(readOnly = true)
    public List<CountryRestriction> getEnabledCountries() {
        return countryRestrictionRepository.findByIsEnabledTrue();
    }

    /**
     * Find countries where registration is allowed
     * 
     * @return List of countries
     */
    @Transactional(readOnly = true)
    public List<CountryRestriction> findByIsEnabledTrueAndIsRegistrationAllowedTrue() {
        return countryRestrictionRepository.findByIsEnabledTrueAndIsRegistrationAllowedTrue();
    }

    /**
     * Create or update country restriction
     * 
     * @param countryRestriction CountryRestriction entity
     * @param updatedBy Admin username
     * @return Saved CountryRestriction
     */
    @CacheEvict(value = "countryRestrictions", allEntries = true)
    public CountryRestriction saveCountryRestriction(CountryRestriction countryRestriction, String updatedBy) {
        countryRestriction.setCountryCode(countryRestriction.getCountryCode().toUpperCase());
        countryRestriction.setUpdatedBy(updatedBy);
        CountryRestriction saved = countryRestrictionRepository.save(countryRestriction);
        log.info("Country restriction {} saved by {}", saved.getCountryCode(), updatedBy);
        return saved;
    }

    /**
     * Toggle country enabled status
     * 
     * @param countryCode Country code
     * @param enabled Enabled status
     * @param updatedBy Admin username
     * @return Updated CountryRestriction
     */
    @CacheEvict(value = "countryRestrictions", allEntries = true)
    public CountryRestriction toggleCountry(String countryCode, Boolean enabled, String updatedBy) {
        CountryRestriction restriction = getCountryRestriction(countryCode);
        restriction.setIsEnabled(enabled);
        restriction.setUpdatedBy(updatedBy);
        CountryRestriction updated = countryRestrictionRepository.save(restriction);
        log.info("Country {} toggled to {} by {}", countryCode, enabled, updatedBy);
        return updated;
    }
}

