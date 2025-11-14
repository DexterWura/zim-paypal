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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Web controller for Thymeleaf views
 * 
 * @author Zim Development Team
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CardService cardService;
    private final StatementService statementService;
    private final MoneyRequestService moneyRequestService;
    private final RewardsService rewardsService;
    private final BillSplitService billSplitService;
    private final PaymentGatewayService paymentGatewayService;
    private final GatewayTransactionService gatewayTransactionService;

    @GetMapping("/")
    public String home(Authentication authentication) {
        // Redirect authenticated users to dashboard
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                       @RequestParam(required = false) String logout,
                       Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }

    // Registration endpoints moved to RegisterController to handle country restrictions and feature flags

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        Account account = accountService.findActiveAccountByUser(user);
        List<Card> cards = cardService.findActiveCardsByUser(user.getId());

        // Get recent transactions
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactions = transactionService.getTransactionsByUser(user.getId(), pageable);

        // Get pending money requests
        List<MoneyRequest> pendingRequests = 
                moneyRequestService.getPendingRequestsForRecipient(user.getId());

        // Get rewards
        Rewards rewards = rewardsService.getRewardsByUserId(user.getId());

        // Get pending bill split participants
        List<com.zim.paypal.model.entity.BillSplitParticipant> pendingSplits = 
                billSplitService.getPendingParticipants(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("cards", cards);
        model.addAttribute("transactions", transactions.getContent());
        model.addAttribute("balance", account.getBalance());
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("pendingRequestCount", pendingRequests.size());
        model.addAttribute("rewards", rewards);
        model.addAttribute("pendingSplits", pendingSplits);
        model.addAttribute("pendingSplitCount", pendingSplits.size());

        return "dashboard";
    }

    @GetMapping("/send")
    public String showSendForm(Model model) {
        model.addAttribute("transferRequest", new TransferRequest());
        return "send";
    }

    @PostMapping("/send")
    public String sendMoney(@Valid @ModelAttribute TransferRequest request,
                           BindingResult result,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "send";
        }

        try {
            User sender = userService.findByUsername(authentication.getName());
            Transaction transaction = transactionService.createTransfer(
                    sender.getId(), request.getReceiverEmail(), request.getAmount(), request.getDescription());
            redirectAttributes.addFlashAttribute("success", "Money sent successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/send";
        }
    }

    @GetMapping("/deposit")
    public String showDepositForm(Model model) {
        List<PaymentGateway> gateways = paymentGatewayService.getEnabledGateways();
        model.addAttribute("gateways", gateways);
        model.addAttribute("depositRequest", new DepositRequest());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@Valid @ModelAttribute DepositRequest request,
                         BindingResult result,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        List<PaymentGateway> gateways = paymentGatewayService.getEnabledGateways();
        model.addAttribute("gateways", gateways);
        
        if (result.hasErrors()) {
            return "deposit";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            
            // If gateway is selected, use gateway transaction
            if (request.getGatewayId() != null) {
                GatewayTransaction gatewayTransaction = gatewayTransactionService.initiateDeposit(
                        user.getId(),
                        request.getGatewayId(),
                        request.getAmount(),
                        request.getPhoneNumber(),
                        user.getEmail()
                );
                redirectAttributes.addFlashAttribute("success", 
                    "Deposit initiated via " + gatewayTransaction.getGateway().getDisplayName() + "! Processing...");
            } else {
                // Direct deposit (admin only or fallback)
                Transaction transaction = transactionService.createDeposit(
                        user.getId(), request.getAmount(), request.getDescription());
                redirectAttributes.addFlashAttribute("success", "Deposit successful!");
            }
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/deposit";
        }
    }

    @GetMapping("/cards")
    public String cards(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        List<Card> cards = cardService.findActiveCardsByUser(user.getId());
        model.addAttribute("cards", cards);
        model.addAttribute("cardRequest", new CardRequest());
        return "cards";
    }

    @PostMapping("/cards")
    public String linkCard(@Valid @ModelAttribute CardRequest request,
                          BindingResult result,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "cards";
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

            cardService.linkCard(user.getId(), card);
            redirectAttributes.addFlashAttribute("success", "Card linked successfully!");
            return "redirect:/cards";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cards";
        }
    }

    @GetMapping("/transactions")
    public String transactions(Authentication authentication,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              Model model) {
        User user = userService.findByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getTransactionsByUser(user.getId(), pageable);

        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        return "transactions";
    }

    @GetMapping("/statements")
    public String statements(Authentication authentication, Model model) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<Statement> statements = statementService.getStatementsByUser(user.getId());
            model.addAttribute("statements", statements != null ? statements : new ArrayList<>());
        } catch (Exception e) {
            log.error("Error loading statements: {}", e.getMessage());
            model.addAttribute("statements", new ArrayList<>());
            model.addAttribute("error", "Error loading statements: " + e.getMessage());
        }
        return "statements";
    }

    @PostMapping("/statements/generate")
    public String generateStatement(@RequestParam int year,
                                   @RequestParam int month,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Statement statement = statementService.generateMonthlyStatement(user.getId(), year, month);
            redirectAttributes.addFlashAttribute("success", "Statement generated successfully!");
            return "redirect:/statements";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/statements";
        }
    }

    @GetMapping("/pay")
    public String showPayForm(Model model) {
        model.addAttribute("paymentRequest", new PaymentRequest());
        return "pay";
    }

    @PostMapping("/pay")
    public String pay(@Valid @ModelAttribute PaymentRequest request,
                     BindingResult result,
                     Authentication authentication,
                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "pay";
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
            
            redirectAttributes.addFlashAttribute("success", "Payment successful!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pay";
        }
    }

    @GetMapping("/request")
    public String showRequestForm(Model model) {
        model.addAttribute("moneyRequestDto", new MoneyRequestDto());
        return "request";
    }

    @PostMapping("/request")
    public String createRequest(@Valid @ModelAttribute MoneyRequestDto requestDto,
                                BindingResult result,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "request";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            MoneyRequest request = moneyRequestService.createRequest(
                    user.getId(), 
                    requestDto.getRecipientEmail(), 
                    requestDto.getAmount(), 
                    requestDto.getMessage(), 
                    requestDto.getNote());
            redirectAttributes.addFlashAttribute("success", "Money request sent successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/request";
        }
    }

    @GetMapping("/requests")
    public String requests(Authentication authentication,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model) {
        User user = userService.findByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<MoneyRequest> requests = 
                moneyRequestService.getRequestsByUser(user.getId(), pageable);

        // Get pending requests separately
        List<MoneyRequest> pendingReceived = 
                moneyRequestService.getPendingRequestsForRecipient(user.getId());
        List<MoneyRequest> pendingSent = 
                moneyRequestService.getPendingRequestsForRequester(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("requests", requests);
        model.addAttribute("pendingReceived", pendingReceived);
        model.addAttribute("pendingSent", pendingSent);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", requests.getTotalPages());
        return "requests";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            moneyRequestService.approveRequest(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Money request approved and payment sent!");
            return "redirect:/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/requests";
        }
    }

    @PostMapping("/requests/{id}/decline")
    public String declineRequest(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            moneyRequestService.declineRequest(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Money request declined.");
            return "redirect:/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/requests";
        }
    }

    @PostMapping("/requests/{id}/cancel")
    public String cancelRequest(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            moneyRequestService.cancelRequest(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Money request cancelled.");
            return "redirect:/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/requests";
        }
    }

    @GetMapping("/rewards")
    public String rewards(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        Rewards rewards = rewardsService.getRewardsByUserId(user.getId());
        model.addAttribute("rewards", rewards);
        return "rewards";
    }

    @PostMapping("/rewards/redeem")
    public String redeemPoints(@RequestParam Integer points,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            BigDecimal cashValue = rewardsService.redeemPoints(user.getId(), points);
            
            // Deposit the redeemed cash into account
            Account account = accountService.findActiveAccountByUser(user);
            accountService.deposit(account.getId(), cashValue);
            
            redirectAttributes.addFlashAttribute("success", 
                    "Redeemed " + points + " points for $" + cashValue + "!");
            return "redirect:/rewards";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rewards";
        }
    }
}
