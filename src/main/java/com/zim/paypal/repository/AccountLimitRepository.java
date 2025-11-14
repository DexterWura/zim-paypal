package com.zim.paypal.repository;

import com.zim.paypal.model.entity.AccountLimit;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AccountLimit entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface AccountLimitRepository extends JpaRepository<AccountLimit, Long> {

    /**
     * Find limit by limit code
     * 
     * @param limitCode Limit code
     * @return Optional AccountLimit
     */
    Optional<AccountLimit> findByLimitCode(String limitCode);

    /**
     * Find all active limits
     * 
     * @return List of active limits
     */
    List<AccountLimit> findByIsActiveTrue();

    /**
     * Find limits by limit type
     * 
     * @param limitType Limit type
     * @return List of limits
     */
    List<AccountLimit> findByLimitTypeAndIsActiveTrue(AccountLimit.LimitType limitType);

    /**
     * Find limits by user role
     * 
     * @param userRole User role
     * @return List of limits
     */
    List<AccountLimit> findByUserRoleAndIsActiveTrue(User.UserRole userRole);

    /**
     * Find limits by user role and limit type
     * 
     * @param userRole User role
     * @param limitType Limit type
     * @return List of limits
     */
    List<AccountLimit> findByUserRoleAndLimitTypeAndIsActiveTrue(User.UserRole userRole, AccountLimit.LimitType limitType);
}

