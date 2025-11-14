package com.zim.paypal.controller;

import com.zim.paypal.model.dto.*;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller
 * 
 * @author Zim Development Team
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CardService cardService;
    private final StatementService statementService;

    @GetMapping("/account/balance")
    public ResponseEntity<Map<String, Object>> getBalance(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Account account = accountService.findActiveAccountByUser(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("balance", account.getBalance());
        response.put("currency", account.getCurrencyCode());
        response.put("accountNumber", account.getAccountNumber());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@Valid @RequestBody TransferRequest request,
                                                       BindingResult result,
                                                       Authentication authentication) {
        if (result.hasErrors()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Validation failed");
            error.put("details", result.getAllErrors());
            return ResponseEntity.badRequest().body(error);
        }

        try {
            User sender = userService.findByUsername(authentication.getName());
            Transaction transaction = transactionService.createTransfer(
                    sender.getId(), request.getReceiverEmail(), request.getAmount(), request.getDescription());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionNumber", transaction.getTransactionNumber());
            response.put("amount", transaction.getAmount());
            response.put("status", transaction.getStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@Valid @RequestBody DepositRequest request,
                                                       BindingResult result,
                                                       Authentication authentication) {
        if (result.hasErrors()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Validation failed");
            error.put("details", result.getAllErrors());
            return ResponseEntity.badRequest().body(error);
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            Transaction transaction = transactionService.createDeposit(
                    user.getId(), request.getAmount(), request.getDescription());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionNumber", transaction.getTransactionNumber());
            response.put("amount", transaction.getAmount());
            response.put("status", transaction.getStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Transaction endpoints moved to TransactionApiController at /api/transactions

    @GetMapping("/cards")
    public ResponseEntity<List<Card>> getCards(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Card> cards = cardService.findActiveCardsByUser(user.getId());
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/cards")
    public ResponseEntity<Map<String, Object>> linkCard(@Valid @RequestBody CardRequest request,
                                                        BindingResult result,
                                                        Authentication authentication) {
        if (result.hasErrors()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Validation failed");
            error.put("details", result.getAllErrors());
            return ResponseEntity.badRequest().body(error);
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            Card card = Card.builder()
                    .cardNumber(request.getCardNumber())
                    .cardholderName(request.getCardholderName())
                    .expiryDate(request.getExpiryDate())
                    .cvv(request.getCvv())
                    .cardType(request.getCardType())
                    .cardBrand(request.getCardBrand())
                    .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                    .billingAddress(request.getBillingAddress())
                    .billingCity(request.getBillingCity())
                    .billingState(request.getBillingState())
                    .billingZip(request.getBillingZip())
                    .billingCountry(request.getBillingCountry())
                    .build();

            Card savedCard = cardService.linkCard(user.getId(), card);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cardId", savedCard.getId());
            response.put("lastFourDigits", savedCard.getLastFourDigits());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> pay(@Valid @RequestBody PaymentRequest request,
                                                   BindingResult result,
                                                   Authentication authentication) {
        if (result.hasErrors()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Validation failed");
            error.put("details", result.getAllErrors());
            return ResponseEntity.badRequest().body(error);
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            Transaction transaction;
            
            if (request.getCardId() != null) {
                transaction = transactionService.createPaymentFromCard(
                        user.getId(), request.getCardId(), request.getAmount(), request.getDescription());
            } else {
                transaction = transactionService.createPaymentFromWallet(
                        user.getId(), request.getAmount(), request.getDescription(), null);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionNumber", transaction.getTransactionNumber());
            response.put("amount", transaction.getAmount());
            response.put("status", transaction.getStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/statements")
    public ResponseEntity<List<Statement>> getStatements(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Statement> statements = statementService.getStatementsByUser(user.getId());
        return ResponseEntity.ok(statements);
    }

    @PostMapping("/statements/generate")
    public ResponseEntity<Map<String, Object>> generateStatement(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Statement statement = statementService.generateMonthlyStatement(user.getId(), year, month);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statementNumber", statement.getStatementNumber());
            response.put("startDate", statement.getStartDate());
            response.put("endDate", statement.getEndDate());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

