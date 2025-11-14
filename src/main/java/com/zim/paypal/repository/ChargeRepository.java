package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Charge;
import com.zim.paypal.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Charge entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface ChargeRepository extends JpaRepository<Charge, Long> {

    /**
     * Find charge by charge code
     * 
     * @param chargeCode Charge code
     * @return Optional Charge
     */
    Optional<Charge> findByChargeCode(String chargeCode);

    /**
     * Find all active charges
     * 
     * @return List of active charges
     */
    List<Charge> findByIsActiveTrue();

    /**
     * Find charges by charge type
     * 
     * @param chargeType Charge type
     * @return List of charges
     */
    List<Charge> findByChargeTypeAndIsActiveTrue(Charge.ChargeType chargeType);

    /**
     * Find charges by transaction type
     * 
     * @param transactionType Transaction type
     * @return List of charges
     */
    List<Charge> findByTransactionTypeAndIsActiveTrue(Transaction.TransactionType transactionType);

    /**
     * Find charges by transaction type and charge type
     * 
     * @param transactionType Transaction type
     * @param chargeType Charge type
     * @return List of charges
     */
    List<Charge> findByTransactionTypeAndChargeTypeAndIsActiveTrue(
            Transaction.TransactionType transactionType, 
            Charge.ChargeType chargeType);
}

