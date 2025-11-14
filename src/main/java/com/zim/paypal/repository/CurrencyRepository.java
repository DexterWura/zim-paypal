package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Currency entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    /**
     * Find currency by currency code
     * 
     * @param currencyCode Currency code
     * @return Optional Currency
     */
    Optional<Currency> findByCurrencyCode(String currencyCode);

    /**
     * Find all active currencies
     * 
     * @return List of active currencies
     */
    List<Currency> findByIsActiveTrue();

    /**
     * Find base currency
     * 
     * @return Optional Currency
     */
    Optional<Currency> findByIsBaseCurrencyTrue();
}

