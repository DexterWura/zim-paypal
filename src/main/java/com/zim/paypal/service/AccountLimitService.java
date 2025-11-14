package com.zim.paypal.service;

import com.zim.paypal.model.dto.AccountLimitDto;
import com.zim.paypal.model.entity.AccountLimit;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.AccountLimitRepository;
import com.zim.paypal.repository.AccountRepository;
import com.zim.paypal.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for account limit management and validation
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountLimitService {

    private final AccountLimitRepository limitRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Create a new account limit
     * 
     * @param limitDto Limit DTO
     * @param createdBy User who created the limit
     * @return Created limit
     */
    public AccountLimit createLimit(AccountLimitDto limitDto, User createdBy) {
        // Check if limit code already exists
        if (limitRepository.findByLimitCode(limitDto.getLimitCode()).isPresent()) {
            throw new IllegalArgumentException("Limit code already exists: " + limitDto.getLimitCode());
        }

        AccountLimit limit = AccountLimit.builder()
                .limitName(limitDto.getLimitName())
                .limitCode(limitDto.getLimitCode())
                .limitType(limitDto.getLimitType())
                .periodType(limitDto.getPeriodType())
                .maxAccountsPerUser(limitDto.getMaxAccountsPerUser())
                .maxTransactionAmount(limitDto.getMaxTransactionAmount())
                .maxDailyAmount(limitDto.getMaxDailyAmount())
                .maxWeeklyAmount(limitDto.getMaxWeeklyAmount())
                .maxMonthlyAmount(limitDto.getMaxMonthlyAmount())
                .maxDailyCount(limitDto.getMaxDailyCount())
                .maxWeeklyCount(limitDto.getMaxWeeklyCount())
                .maxMonthlyCount(limitDto.getMaxMonthlyCount())
                .userRole(limitDto.getUserRole())
                .isActive(limitDto.getIsActive())
                .description(limitDto.getDescription())
                .createdBy(createdBy)
                .build();

        AccountLimit savedLimit = limitRepository.save(limit);
        log.info("Account limit created: {} by user: {}", limitDto.getLimitCode(), createdBy.getUsername());
        return savedLimit;
    }

    /**
     * Update an existing limit
     * 
     * @param limitId Limit ID
     * @param limitDto Limit DTO
     * @param updatedBy User who updated the limit
     * @return Updated limit
     */
    public AccountLimit updateLimit(Long limitId, AccountLimitDto limitDto, User updatedBy) {
        AccountLimit limit = limitRepository.findById(limitId)
                .orElseThrow(() -> new IllegalArgumentException("Limit not found: " + limitId));

        // Check if limit code is being changed and if new code already exists
        if (!limit.getLimitCode().equals(limitDto.getLimitCode())) {
            if (limitRepository.findByLimitCode(limitDto.getLimitCode()).isPresent()) {
                throw new IllegalArgumentException("Limit code already exists: " + limitDto.getLimitCode());
            }
        }

        limit.setLimitName(limitDto.getLimitName());
        limit.setLimitCode(limitDto.getLimitCode());
        limit.setLimitType(limitDto.getLimitType());
        limit.setPeriodType(limitDto.getPeriodType());
        limit.setMaxAccountsPerUser(limitDto.getMaxAccountsPerUser());
        limit.setMaxTransactionAmount(limitDto.getMaxTransactionAmount());
        limit.setMaxDailyAmount(limitDto.getMaxDailyAmount());
        limit.setMaxWeeklyAmount(limitDto.getMaxWeeklyAmount());
        limit.setMaxMonthlyAmount(limitDto.getMaxMonthlyAmount());
        limit.setMaxDailyCount(limitDto.getMaxDailyCount());
        limit.setMaxWeeklyCount(limitDto.getMaxWeeklyCount());
        limit.setMaxMonthlyCount(limitDto.getMaxMonthlyCount());
        limit.setUserRole(limitDto.getUserRole());
        limit.setIsActive(limitDto.getIsActive());
        limit.setDescription(limitDto.getDescription());
        limit.setUpdatedBy(updatedBy);

        AccountLimit savedLimit = limitRepository.save(limit);
        log.info("Account limit updated: {} by user: {}", limitDto.getLimitCode(), updatedBy.getUsername());
        return savedLimit;
    }

    /**
     * Get limit by ID
     * 
     * @param limitId Limit ID
     * @return AccountLimit entity
     */
    @Transactional(readOnly = true)
    public AccountLimit getLimitById(Long limitId) {
        return limitRepository.findById(limitId)
                .orElseThrow(() -> new IllegalArgumentException("Limit not found: " + limitId));
    }

    /**
     * Get all active limits
     * 
     * @return List of limits
     */
    @Transactional(readOnly = true)
    public List<AccountLimit> getAllActiveLimits() {
        return limitRepository.findByIsActiveTrue();
    }

    /**
     * Get limits for user role
     * 
     * @param userRole User role
     * @return List of limits
     */
    @Transactional(readOnly = true)
    public List<AccountLimit> getLimitsForRole(User.UserRole userRole) {
        return limitRepository.findByUserRoleAndIsActiveTrue(userRole);
    }

    /**
     * Check if user can create another account
     * 
     * @param userId User ID
     * @param userRole User role
     * @return true if allowed
     */
    @Transactional(readOnly = true)
    public boolean canCreateAccount(Long userId, User.UserRole userRole) {
        List<AccountLimit> limits = limitRepository.findByUserRoleAndLimitTypeAndIsActiveTrue(
                userRole, AccountLimit.LimitType.ACCOUNT_COUNT);

        if (limits.isEmpty()) {
            return true; // No limit configured
        }

        // Get the most restrictive limit
        AccountLimit limit = limits.stream()
                .filter(l -> l.getMaxAccountsPerUser() != null)
                .min((l1, l2) -> Integer.compare(l1.getMaxAccountsPerUser(), l2.getMaxAccountsPerUser()))
                .orElse(null);

        if (limit == null || limit.getMaxAccountsPerUser() == null) {
            return true;
        }

        long currentAccountCount = accountRepository.countByUserId(userId);
        return currentAccountCount < limit.getMaxAccountsPerUser();
    }

    /**
     * Check if transaction amount is within limits
     * 
     * @param userId User ID
     * @param userRole User role
     * @param amount Transaction amount
     * @return true if allowed
     */
    @Transactional(readOnly = true)
    public boolean isTransactionAmountAllowed(Long userId, User.UserRole userRole, BigDecimal amount) {
        List<AccountLimit> limits = limitRepository.findByUserRoleAndLimitTypeAndIsActiveTrue(
                userRole, AccountLimit.LimitType.TRANSACTION_AMOUNT);

        if (limits.isEmpty()) {
            return true; // No limit configured
        }

        // Check per-transaction limit
        AccountLimit transactionLimit = limits.stream()
                .filter(l -> l.getMaxTransactionAmount() != null)
                .min((l1, l2) -> l1.getMaxTransactionAmount().compareTo(l2.getMaxTransactionAmount()))
                .orElse(null);

        if (transactionLimit != null && transactionLimit.getMaxTransactionAmount() != null) {
            if (amount.compareTo(transactionLimit.getMaxTransactionAmount()) > 0) {
                return false;
            }
        }

        // Check daily limit
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        BigDecimal dailyTotal = transactionRepository.findBySenderIdAndCreatedAtBetween(
                userId, startOfDay, endOfDay)
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AccountLimit dailyLimit = limits.stream()
                .filter(l -> l.getMaxDailyAmount() != null)
                .min((l1, l2) -> l1.getMaxDailyAmount().compareTo(l2.getMaxDailyAmount()))
                .orElse(null);

        if (dailyLimit != null && dailyLimit.getMaxDailyAmount() != null) {
            if (dailyTotal.add(amount).compareTo(dailyLimit.getMaxDailyAmount()) > 0) {
                return false;
            }
        }

        // Check weekly limit
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfWeek = weekStart.atStartOfDay();
        LocalDateTime endOfWeek = weekStart.plusWeeks(1).atStartOfDay();

        BigDecimal weeklyTotal = transactionRepository.findBySenderIdAndCreatedAtBetween(
                userId, startOfWeek, endOfWeek)
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AccountLimit weeklyLimit = limits.stream()
                .filter(l -> l.getMaxWeeklyAmount() != null)
                .min((l1, l2) -> l1.getMaxWeeklyAmount().compareTo(l2.getMaxWeeklyAmount()))
                .orElse(null);

        if (weeklyLimit != null && weeklyLimit.getMaxWeeklyAmount() != null) {
            if (weeklyTotal.add(amount).compareTo(weeklyLimit.getMaxWeeklyAmount()) > 0) {
                return false;
            }
        }

        // Check monthly limit
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime startOfMonth = monthStart.atStartOfDay();
        LocalDateTime endOfMonth = monthStart.plusMonths(1).atStartOfDay();

        BigDecimal monthlyTotal = transactionRepository.findBySenderIdAndCreatedAtBetween(
                userId, startOfMonth, endOfMonth)
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AccountLimit monthlyLimit = limits.stream()
                .filter(l -> l.getMaxMonthlyAmount() != null)
                .min((l1, l2) -> l1.getMaxMonthlyAmount().compareTo(l2.getMaxMonthlyAmount()))
                .orElse(null);

        if (monthlyLimit != null && monthlyLimit.getMaxMonthlyAmount() != null) {
            if (monthlyTotal.add(amount).compareTo(monthlyLimit.getMaxMonthlyAmount()) > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get all limits
     * 
     * @return List of limits
     */
    @Transactional(readOnly = true)
    public List<AccountLimit> getAllLimits() {
        return limitRepository.findAll();
    }
}

