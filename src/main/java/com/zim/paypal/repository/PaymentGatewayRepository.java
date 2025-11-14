package com.zim.paypal.repository;

import com.zim.paypal.model.entity.PaymentGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentGateway entity
 * 
 * @author dexterwura
 */
@Repository
public interface PaymentGatewayRepository extends JpaRepository<PaymentGateway, Long> {
    
    Optional<PaymentGateway> findByGatewayName(String gatewayName);
    
    List<PaymentGateway> findByIsEnabledTrue();
    
    List<PaymentGateway> findByGatewayType(PaymentGateway.GatewayType gatewayType);
}

