package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Currency;
import com.zim.paypal.model.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ExchangeRate entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Find effective exchange rate between two currencies
     * 
     * @param fromCurrency From currency
     * @param toCurrency To currency
     * @param now Current date time
     * @return Optional ExchangeRate
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency " +
           "AND er.toCurrency = :toCurrency AND er.isActive = true " +
           "AND er.effectiveFrom <= :now " +
           "AND (er.effectiveTo IS NULL OR er.effectiveTo >= :now) " +
           "ORDER BY er.effectiveFrom DESC")
    Optional<ExchangeRate> findEffectiveRate(@Param("fromCurrency") Currency fromCurrency,
                                            @Param("toCurrency") Currency toCurrency,
                                            @Param("now") LocalDateTime now);

    /**
     * Find all effective exchange rates
     * 
     * @param now Current date time
     * @return List of effective rates
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.isActive = true " +
           "AND er.effectiveFrom <= :now " +
           "AND (er.effectiveTo IS NULL OR er.effectiveTo >= :now)")
    List<ExchangeRate> findEffectiveRates(@Param("now") LocalDateTime now);

    /**
     * Find rates by from currency
     * 
     * @param fromCurrency From currency
     * @return List of rates
     */
    List<ExchangeRate> findByFromCurrencyOrderByEffectiveFromDesc(Currency fromCurrency);
}

