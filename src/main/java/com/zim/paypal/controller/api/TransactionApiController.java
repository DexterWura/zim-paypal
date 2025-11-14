package com.zim.paypal.controller.api;

import com.zim.paypal.model.dto.ApiResponse;
import com.zim.paypal.model.dto.TransferRequest;
import com.zim.paypal.model.entity.Transaction;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.service.TransactionService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST API Controller for Transactions
 * 
 * @author dexterwura
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionApiController {

    private final TransactionService transactionService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Transaction>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Pageable pageable = PageRequest.of(page, size);
            Page<Transaction> transactions = transactionService.getTransactionsByUserId(user.getId(), pageable);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            log.error("Error getting transactions: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Transaction>> transfer(@Valid @RequestBody TransferRequest request,
                                                           Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Transaction transaction = transactionService.createTransfer(
                    user.getId(), request.getReceiverEmail(), request.getAmount(), request.getDescription());
            return ResponseEntity.ok(ApiResponse.success("Transfer successful", transaction));
        } catch (Exception e) {
            log.error("Error transferring: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<Transaction>> deposit(
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Transaction transaction = transactionService.createDeposit(
                    user.getId(), amount, description != null ? description : "Deposit");
            return ResponseEntity.ok(ApiResponse.success("Deposit successful", transaction));
        } catch (Exception e) {
            log.error("Error depositing: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Transaction>> getTransaction(@PathVariable Long transactionId,
                                                                   Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Transaction transaction = transactionService.findById(transactionId);
            
            if (!transaction.getSender().getId().equals(user.getId()) &&
                (transaction.getReceiver() == null || !transaction.getReceiver().getId().equals(user.getId()))) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(transaction));
        } catch (Exception e) {
            log.error("Error getting transaction: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

