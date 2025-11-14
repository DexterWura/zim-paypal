package com.zim.paypal.repository;

import com.zim.paypal.model.entity.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ServiceProvider entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {

    /**
     * Find provider by provider code
     * 
     * @param providerCode Provider code
     * @return Optional ServiceProvider
     */
    Optional<ServiceProvider> findByProviderCode(String providerCode);

    /**
     * Find all active providers
     * 
     * @return List of active providers
     */
    List<ServiceProvider> findByIsActiveTrue();

    /**
     * Find providers by type
     * 
     * @param providerType Provider type
     * @return List of providers
     */
    List<ServiceProvider> findByProviderTypeAndIsActiveTrue(ServiceProvider.ProviderType providerType);

    /**
     * Find providers that support airtime
     * 
     * @return List of providers
     */
    List<ServiceProvider> findBySupportsAirtimeTrueAndIsActiveTrue();

    /**
     * Find providers that support data
     * 
     * @return List of providers
     */
    List<ServiceProvider> findBySupportsDataTrueAndIsActiveTrue();

    /**
     * Find providers that support tokens
     * 
     * @return List of providers
     */
    List<ServiceProvider> findBySupportsTokensTrueAndIsActiveTrue();
}

