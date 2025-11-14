package com.zim.paypal.controller.api;

import com.zim.paypal.model.dto.ApiResponse;
import com.zim.paypal.model.entity.Account;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.service.AccountService;
import com.zim.paypal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Account Management
 * 
 * @author dexterwura
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountApiController {

    private final AccountService accountService;
    private final UserService userService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Account account = accountService.findActiveAccountByUser(user);
            
            Map<String, Object> balanceInfo = Map.of(
                "accountId", account.getId(),
                "accountNumber", account.getAccountNumber(),
                "balance", account.getBalance(),
                "currency", account.getCurrencyCode(),
                "currencySymbol", account.getCurrency() != null ? account.getCurrency().getSymbol() : "$"
            );
            
            return ResponseEntity.ok(ApiResponse.success(balanceInfo));
        } catch (Exception e) {
            log.error("Error getting balance: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Account>>> getAccounts(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<Account> accounts = accountService.findByUser(user);
            return ResponseEntity.ok(ApiResponse.success(accounts));
        } catch (Exception e) {
            log.error("Error getting accounts: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Account>> createAccount(
            @RequestParam(required = false) Long currencyId,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Account account = currencyId != null ?
                    accountService.createAccount(user, currencyId) :
                    accountService.createDefaultAccount(user);
            return ResponseEntity.ok(ApiResponse.success("Account created successfully", account));
        } catch (Exception e) {
            log.error("Error creating account: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Account>> getAccount(@PathVariable Long accountId,
                                                           Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Account account = accountService.findById(accountId);
            
            if (!account.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(account));
        } catch (Exception e) {
            log.error("Error getting account: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

