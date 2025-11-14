package com.zim.paypal.repository;

import com.zim.paypal.model.entity.CountryRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CountryRestriction entity
 * 
 * @author dexterwura
 */
@Repository
public interface CountryRestrictionRepository extends JpaRepository<CountryRestriction, Long> {

    /**
     * Find country restriction by country code
     * 
     * @param countryCode Country code (ISO 3166-1 alpha-2)
     * @return Optional CountryRestriction
     */
    Optional<CountryRestriction> findByCountryCode(String countryCode);

    /**
     * Find all enabled countries
     * 
     * @return List of enabled countries
     */
    List<CountryRestriction> findByIsEnabledTrue();

    /**
     * Find countries where registration is allowed
     * 
     * @return List of countries
     */
    List<CountryRestriction> findByIsEnabledTrueAndIsRegistrationAllowedTrue();

    /**
     * Find countries where transactions are allowed
     * 
     * @return List of countries
     */
    List<CountryRestriction> findByIsEnabledTrueAndIsTransactionAllowedTrue();

    /**
     * Find countries where merchant features are allowed
     * 
     * @return List of countries
     */
    List<CountryRestriction> findByIsEnabledTrueAndIsMerchantAllowedTrue();
}

