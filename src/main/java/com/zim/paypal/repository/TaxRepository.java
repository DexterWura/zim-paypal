package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Tax;
import com.zim.paypal.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Tax entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface TaxRepository extends JpaRepository<Tax, Long> {

    /**
     * Find tax by tax code
     * 
     * @param taxCode Tax code
     * @return Optional Tax
     */
    Optional<Tax> findByTaxCode(String taxCode);

    /**
     * Find all active taxes
     * 
     * @return List of active taxes
     */
    List<Tax> findByIsActiveTrue();

    /**
     * Find taxes by tax type
     * 
     * @param taxType Tax type
     * @return List of taxes
     */
    List<Tax> findByTaxTypeAndIsActiveTrue(Tax.TaxType taxType);

    /**
     * Find taxes by transaction type
     * 
     * @param transactionType Transaction type
     * @return List of taxes
     */
    List<Tax> findByTransactionTypeAndIsActiveTrue(Transaction.TransactionType transactionType);

    /**
     * Find effective taxes (active and within effective date range)
     * 
     * @param now Current date time
     * @return List of effective taxes
     */
    @Query("SELECT t FROM Tax t WHERE t.isActive = true " +
           "AND (t.effectiveFrom IS NULL OR t.effectiveFrom <= :now) " +
           "AND (t.effectiveTo IS NULL OR t.effectiveTo >= :now)")
    List<Tax> findEffectiveTaxes(@Param("now") LocalDateTime now);

    /**
     * Find effective taxes by transaction type
     * 
     * @param transactionType Transaction type
     * @param now Current date time
     * @return List of effective taxes
     */
    @Query("SELECT t FROM Tax t WHERE t.isActive = true " +
           "AND (t.transactionType IS NULL OR t.transactionType = :transactionType) " +
           "AND (t.effectiveFrom IS NULL OR t.effectiveFrom <= :now) " +
           "AND (t.effectiveTo IS NULL OR t.effectiveTo >= :now)")
    List<Tax> findEffectiveTaxesByTransactionType(
            @Param("transactionType") Transaction.TransactionType transactionType,
            @Param("now") LocalDateTime now);
}

