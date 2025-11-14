package com.zim.paypal.controller;

import com.zim.paypal.model.entity.Account;
import com.zim.paypal.model.entity.Currency;
import com.zim.paypal.service.AccountService;
import com.zim.paypal.service.CurrencyService;
import com.zim.paypal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for account management (multi-currency accounts)
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final CurrencyService currencyService;
    private final UserService userService;

    @GetMapping
    public String myAccounts(Authentication authentication, Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        List<Account> accounts = accountService.findByUser(user);
        List<Currency> availableCurrencies = currencyService.getAllActiveCurrencies();
        
        model.addAttribute("accounts", accounts);
        model.addAttribute("availableCurrencies", availableCurrencies);
        model.addAttribute("user", user);
        
        return "accounts/list";
    }

    @PostMapping("/create")
    public String createAccount(@RequestParam Long currencyId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            Account account = accountService.createAccount(user, currencyId);
            redirectAttributes.addFlashAttribute("success", 
                    "Account created successfully! Account #: " + account.getAccountNumber());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts";
    }
}

