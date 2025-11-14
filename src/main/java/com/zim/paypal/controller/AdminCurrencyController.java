package com.zim.paypal.controller;

import com.zim.paypal.model.dto.AccountLimitDto;
import com.zim.paypal.model.dto.CurrencyDto;
import com.zim.paypal.model.dto.ExchangeRateDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.service.AccountLimitService;
import com.zim.paypal.service.CurrencyService;
import com.zim.paypal.service.ExchangeRateService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin controller for managing currencies, exchange rates, and account limits
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminCurrencyController {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;
    private final AccountLimitService accountLimitService;
    private final UserService userService;

    // ========== CURRENCIES ==========

    @GetMapping("/currencies")
    public String currencies(Model model) {
        List<Currency> currencies = currencyService.getAllCurrencies();
        model.addAttribute("currencies", currencies);
        return "admin/currencies";
    }

    @GetMapping("/currencies/new")
    public String showCreateCurrencyForm(Model model) {
        model.addAttribute("currencyDto", new CurrencyDto());
        return "admin/currency-form";
    }

    @PostMapping("/currencies/new")
    public String createCurrency(@Valid @ModelAttribute CurrencyDto currencyDto,
                                BindingResult result,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/currency-form";
        }

        try {
            User admin = userService.findByUsername(authentication.getName());
            currencyService.createCurrency(currencyDto, admin);
            redirectAttributes.addFlashAttribute("success", "Currency created successfully!");
            return "redirect:/admin/currencies";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/currencies/new";
        }
    }

    @GetMapping("/currencies/{id}/edit")
    public String showEditCurrencyForm(@PathVariable Long id, Model model) {
        Currency currency = currencyService.getCurrencyById(id);
        CurrencyDto currencyDto = CurrencyDto.builder()
                .currencyCode(currency.getCurrencyCode())
                .currencyName(currency.getCurrencyName())
                .symbol(currency.getSymbol())
                .isActive(currency.getIsActive())
                .isBaseCurrency(currency.getIsBaseCurrency())
                .decimalPlaces(currency.getDecimalPlaces())
                .build();
        model.addAttribute("currencyDto", currencyDto);
        model.addAttribute("currencyId", id);
        return "admin/currency-form";
    }

    @PostMapping("/currencies/{id}/edit")
    public String updateCurrency(@PathVariable Long id,
                                 @Valid @ModelAttribute CurrencyDto currencyDto,
                                 BindingResult result,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("currencyId", id);
            return "admin/currency-form";
        }

        try {
            User admin = userService.findByUsername(authentication.getName());
            currencyService.updateCurrency(id, currencyDto, admin);
            redirectAttributes.addFlashAttribute("success", "Currency updated successfully!");
            return "redirect:/admin/currencies";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/currencies/" + id + "/edit";
        }
    }

    // ========== EXCHANGE RATES ==========

    @GetMapping("/exchange-rates")
    public String exchangeRates(Model model) {
        List<ExchangeRate> rates = exchangeRateService.getAllRates();
        List<Currency> currencies = currencyService.getAllActiveCurrencies();
        model.addAttribute("rates", rates);
        model.addAttribute("currencies", currencies);
        return "admin/exchange-rates";
    }

    @GetMapping("/exchange-rates/new")
    public String showCreateRateForm(Model model) {
        model.addAttribute("rateDto", new ExchangeRateDto());
        model.addAttribute("currencies", currencyService.getAllActiveCurrencies());
        return "admin/exchange-rate-form";
    }

    @PostMapping("/exchange-rates/new")
    public String createExchangeRate(@Valid @ModelAttribute ExchangeRateDto rateDto,
                                     BindingResult result,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("currencies", currencyService.getAllActiveCurrencies());
            return "admin/exchange-rate-form";
        }

        try {
            User admin = userService.findByUsername(authentication.getName());
            exchangeRateService.createExchangeRate(rateDto, admin);
            redirectAttributes.addFlashAttribute("success", "Exchange rate created successfully!");
            return "redirect:/admin/exchange-rates";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/exchange-rates/new";
        }
    }

    @GetMapping("/exchange-rates/{id}/edit")
    public String showEditRateForm(@PathVariable Long id, Model model) {
        ExchangeRate rate = exchangeRateService.getRateById(id);
        ExchangeRateDto rateDto = ExchangeRateDto.builder()
                .fromCurrencyId(rate.getFromCurrency().getId())
                .toCurrencyId(rate.getToCurrency().getId())
                .rate(rate.getRate())
                .effectiveFrom(rate.getEffectiveFrom())
                .effectiveTo(rate.getEffectiveTo())
                .isActive(rate.getIsActive())
                .build();
        model.addAttribute("rateDto", rateDto);
        model.addAttribute("rateId", id);
        model.addAttribute("currencies", currencyService.getAllActiveCurrencies());
        return "admin/exchange-rate-form";
    }

    @PostMapping("/exchange-rates/{id}/edit")
    public String updateExchangeRate(@PathVariable Long id,
                                     @Valid @ModelAttribute ExchangeRateDto rateDto,
                                     BindingResult result,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("rateId", id);
            redirectAttributes.addFlashAttribute("currencies", currencyService.getAllActiveCurrencies());
            return "admin/exchange-rate-form";
        }

        try {
            User admin = userService.findByUsername(authentication.getName());
            exchangeRateService.updateExchangeRate(id, rateDto, admin);
            redirectAttributes.addFlashAttribute("success", "Exchange rate updated successfully!");
            return "redirect:/admin/exchange-rates";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/exchange-rates/" + id + "/edit";
        }
    }

    // ========== ACCOUNT LIMITS ==========

    @GetMapping("/account-limits")
    public String accountLimits(Model model) {
        List<AccountLimit> limits = accountLimitService.getAllLimits();
        model.addAttribute("limits", limits);
        return "admin/account-limits";
    }

    @GetMapping("/account-limits/new")
    public String showCreateLimitForm(Model model) {
        model.addAttribute("limitDto", new AccountLimitDto());
        model.addAttribute("limitTypes", AccountLimit.LimitType.values());
        model.addAttribute("periodTypes", AccountLimit.PeriodType.values());
        model.addAttribute("userRoles", User.UserRole.values());
        return "admin/account-limit-form";
    }

    @PostMapping("/account-limits/new")
    public String createAccountLimit(@Valid @ModelAttribute AccountLimitDto limitDto,
                                    BindingResult result,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("limitTypes", AccountLimit.LimitType.values());
            redirectAttributes.addFlashAttribute("periodTypes", AccountLimit.PeriodType.values());
            redirectAttributes.addFlashAttribute("userRoles", User.UserRole.values());
            return "admin/account-limit-form";
        }

        try {
            User admin = userService.findByUsername(authentication.getName());
            accountLimitService.createLimit(limitDto, admin);
            redirectAttributes.addFlashAttribute("success", "Account limit created successfully!");
            return "redirect:/admin/account-limits";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/account-limits/new";
        }
    }

    @GetMapping("/account-limits/{id}/edit")
    public String showEditLimitForm(@PathVariable Long id, Model model) {
        AccountLimit limit = accountLimitService.getLimitById(id);
        AccountLimitDto limitDto = AccountLimitDto.builder()
                .limitName(limit.getLimitName())
                .limitCode(limit.getLimitCode())
                .limitType(limit.getLimitType())
                .periodType(limit.getPeriodType())
                .maxAccountsPerUser(limit.getMaxAccountsPerUser())
                .maxTransactionAmount(limit.getMaxTransactionAmount())
                .maxDailyAmount(limit.getMaxDailyAmount())
                .maxWeeklyAmount(limit.getMaxWeeklyAmount())
                .maxMonthlyAmount(limit.getMaxMonthlyAmount())
                .maxDailyCount(limit.getMaxDailyCount())
                .maxWeeklyCount(limit.getMaxWeeklyCount())
                .maxMonthlyCount(limit.getMaxMonthlyCount())
                .userRole(limit.getUserRole())
                .isActive(limit.getIsActive())
                .description(limit.getDescription())
                .build();
        model.addAttribute("limitDto", limitDto);
        model.addAttribute("limitId", id);
        model.addAttribute("limitTypes", AccountLimit.LimitType.values());
        model.addAttribute("periodTypes", AccountLimit.PeriodType.values());
        model.addAttribute("userRoles", User.UserRole.values());
        return "admin/account-limit-form";
    }

    @PostMapping("/account-limits/{id}/edit")
    public String updateAccountLimit(@PathVariable Long id,
                                    @Valid @ModelAttribute AccountLimitDto limitDto,
                                    BindingResult result,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("limitId", id);
            redirectAttributes.addFlashAttribute("limitTypes", AccountLimit.LimitType.values());
            redirectAttributes.addFlashAttribute("periodTypes", AccountLimit.PeriodType.values());
            redirectAttributes.addFlashAttribute("userRoles", User.UserRole.values());
            return "admin/account-limit-form";
        }

        try {
            User admin = userService.findByUsername(authentication.getName());
            accountLimitService.updateLimit(id, limitDto, admin);
            redirectAttributes.addFlashAttribute("success", "Account limit updated successfully!");
            return "redirect:/admin/account-limits";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/account-limits/" + id + "/edit";
        }
    }

    @PostMapping("/account-limits/{id}/delete")
    public String deleteAccountLimit(@PathVariable Long id,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.findByUsername(authentication.getName());
            AccountLimit limit = accountLimitService.getLimitById(id);
            limit.setIsActive(false);
            limit.setUpdatedBy(admin);
            accountLimitService.updateLimit(id, AccountLimitDto.builder()
                    .limitName(limit.getLimitName())
                    .limitCode(limit.getLimitCode())
                    .limitType(limit.getLimitType())
                    .periodType(limit.getPeriodType())
                    .maxAccountsPerUser(limit.getMaxAccountsPerUser())
                    .maxTransactionAmount(limit.getMaxTransactionAmount())
                    .maxDailyAmount(limit.getMaxDailyAmount())
                    .maxWeeklyAmount(limit.getMaxWeeklyAmount())
                    .maxMonthlyAmount(limit.getMaxMonthlyAmount())
                    .maxDailyCount(limit.getMaxDailyCount())
                    .maxWeeklyCount(limit.getMaxWeeklyCount())
                    .maxMonthlyCount(limit.getMaxMonthlyCount())
                    .userRole(limit.getUserRole())
                    .isActive(false)
                    .description(limit.getDescription())
                    .build(), admin);
            redirectAttributes.addFlashAttribute("success", "Account limit deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/account-limits";
    }
}

