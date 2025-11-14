package com.zim.paypal.repository;

import com.zim.paypal.model.entity.GatewayTransaction;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GatewayTransaction entity
 * 
 * @author dexterwura
 */
@Repository
public interface GatewayTransactionRepository extends JpaRepository<GatewayTransaction, Long> {
    
    Optional<GatewayTransaction> findByGatewayTransactionId(String gatewayTransactionId);
    
    List<GatewayTransaction> findByUserOrderByCreatedAtDesc(User user);
    
    List<GatewayTransaction> findByUserAndStatus(User user, GatewayTransaction.TransactionStatus status);
}

