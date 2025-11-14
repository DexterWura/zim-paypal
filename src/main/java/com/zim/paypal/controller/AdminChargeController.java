package com.zim.paypal.controller;

import com.zim.paypal.model.dto.ChargeDto;
import com.zim.paypal.model.dto.TaxDto;
import com.zim.paypal.model.entity.Charge;
import com.zim.paypal.model.entity.Tax;
import com.zim.paypal.service.ChargeService;
import com.zim.paypal.service.TaxService;
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
 * Admin controller for managing charges and taxes
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminChargeController {

    private final ChargeService chargeService;
    private final TaxService taxService;
    private final UserService userService;

    // ========== CHARGES ==========

    @GetMapping("/charges")
    public String charges(Model model) {
        List<Charge> charges = chargeService.getAllCharges();
        model.addAttribute("charges", charges);
        return "admin/charges";
    }

    @GetMapping("/charges/new")
    public String showCreateChargeForm(Model model) {
        model.addAttribute("chargeDto", new ChargeDto());
        model.addAttribute("chargeTypes", Charge.ChargeType.values());
        model.addAttribute("chargeMethods", Charge.ChargeMethod.values());
        model.addAttribute("transactionTypes", com.zim.paypal.model.entity.Transaction.TransactionType.values());
        return "admin/charge-form";
    }

    @PostMapping("/charges/new")
    public String createCharge(@Valid @ModelAttribute ChargeDto chargeDto,
                              BindingResult result,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("chargeDto", chargeDto);
            redirectAttributes.addFlashAttribute("chargeTypes", Charge.ChargeType.values());
            redirectAttributes.addFlashAttribute("chargeMethods", Charge.ChargeMethod.values());
            redirectAttributes.addFlashAttribute("transactionTypes", com.zim.paypal.model.entity.Transaction.TransactionType.values());
            return "admin/charge-form";
        }

        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            chargeService.createCharge(chargeDto, admin);
            redirectAttributes.addFlashAttribute("success", "Charge created successfully!");
            return "redirect:/admin/charges";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/charges/new";
        }
    }

    @GetMapping("/charges/{id}/edit")
    public String showEditChargeForm(@PathVariable Long id, Model model) {
        Charge charge = chargeService.getChargeById(id);
        ChargeDto chargeDto = ChargeDto.builder()
                .chargeName(charge.getChargeName())
                .chargeCode(charge.getChargeCode())
                .chargeType(charge.getChargeType())
                .transactionType(charge.getTransactionType())
                .chargeMethod(charge.getChargeMethod())
                .fixedAmount(charge.getFixedAmount())
                .percentageRate(charge.getPercentageRate())
                .minAmount(charge.getMinAmount())
                .maxAmount(charge.getMaxAmount())
                .isActive(charge.getIsActive())
                .description(charge.getDescription())
                .regulationReference(charge.getRegulationReference())
                .build();
        model.addAttribute("chargeDto", chargeDto);
        model.addAttribute("chargeId", id);
        model.addAttribute("chargeTypes", Charge.ChargeType.values());
        model.addAttribute("chargeMethods", Charge.ChargeMethod.values());
        model.addAttribute("transactionTypes", com.zim.paypal.model.entity.Transaction.TransactionType.values());
        return "admin/charge-form";
    }

    @PostMapping("/charges/{id}/edit")
    public String updateCharge(@PathVariable Long id,
                              @Valid @ModelAttribute ChargeDto chargeDto,
                              BindingResult result,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("chargeDto", chargeDto);
            redirectAttributes.addFlashAttribute("chargeId", id);
            redirectAttributes.addFlashAttribute("chargeTypes", Charge.ChargeType.values());
            redirectAttributes.addFlashAttribute("chargeMethods", Charge.ChargeMethod.values());
            redirectAttributes.addFlashAttribute("transactionTypes", com.zim.paypal.model.entity.Transaction.TransactionType.values());
            return "admin/charge-form";
        }

        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            chargeService.updateCharge(id, chargeDto, admin);
            redirectAttributes.addFlashAttribute("success", "Charge updated successfully!");
            return "redirect:/admin/charges";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/charges/" + id + "/edit";
        }
    }

    @PostMapping("/charges/{id}/delete")
    public String deleteCharge(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            chargeService.deleteCharge(id, admin);
            redirectAttributes.addFlashAttribute("success", "Charge deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/charges";
    }

    // ========== TAXES ==========

    @GetMapping("/taxes")
    public String taxes(Model model) {
        List<Tax> taxes = taxService.getAllTaxes();
        model.addAttribute("taxes", taxes);
        return "admin/taxes";
    }

    @GetMapping("/taxes/new")
    public String showCreateTaxForm(Model model) {
        model.addAttribute("taxDto", new TaxDto());
        model.addAttribute("taxTypes", Tax.TaxType.values());
        model.addAttribute("transactionTypes", com.zim.paypal.model.entity.Transaction.TransactionType.values());
        return "admin/tax-form";
    }

    @PostMapping("/taxes/new")
    public String createTax(@Valid @ModelAttribute TaxDto taxDto,
                           BindingResult result,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("taxDto", taxDto);
            redirectAttributes.addFlashAttribute("taxTypes", Tax.TaxType.values());
            redirectAttributes.addFlashAttribute("transactionTypes", com.zim.paypal.model.entity.Transaction.TransactionType.values());
            return "admin/tax-form";
        }

        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            taxService.createTax(taxDto, admin);
            redirectAttributes.addFlashAttribute("success", "Tax created successfully!");
            return "redirect:/admin/taxes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/taxes/new";
        }
    }

    @GetMapping("/taxes/{id}/edit")
    public String showEditTaxForm(@PathVariable Long id, Model model) {
        Tax tax = taxService.getTaxById(id);
        TaxDto taxDto = TaxDto.builder()
                .taxName(tax.getTaxName())
                .taxCode(tax.getTaxCode())
                .taxType(tax.getTaxType())
                .taxRate(tax.getTaxRate())
                .transactionType(tax.getTransactionType())
                .isActive(tax.getIsActive())
                .description(tax.getDescription())
                .regulationReference(tax.getRegulationReference())
                .effectiveFrom(tax.getEffectiveFrom())
                .effectiveTo(tax.getEffectiveTo())
                .build();
        model.addAttribute("taxDto", taxDto);
        model.addAttribute("taxId", id);
        model.addAttribute("taxTypes", Tax.TaxType.values());
        model.addAttribute("transactionTypes", com.zim.paypal.model.entity.Transaction.TransactionType.values());
        return "admin/tax-form";
    }

    @PostMapping("/taxes/{id}/edit")
    public String updateTax(@PathVariable Long id,
                          @Valid @ModelAttribute TaxDto taxDto,
                          BindingResult result,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("taxDto", taxDto);
            redirectAttributes.addFlashAttribute("taxId", id);
            redirectAttributes.addFlashAttribute("taxTypes", Tax.TaxType.values());
            redirectAttributes.addFlashAttribute("transactionTypes", com.zim.paypal.model.entity.Transaction.TransactionType.values());
            return "admin/tax-form";
        }

        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            taxService.updateTax(id, taxDto, admin);
            redirectAttributes.addFlashAttribute("success", "Tax updated successfully!");
            return "redirect:/admin/taxes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/taxes/" + id + "/edit";
        }
    }

    @PostMapping("/taxes/{id}/delete")
    public String deleteTax(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            taxService.deleteTax(id, admin);
            redirectAttributes.addFlashAttribute("success", "Tax deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/taxes";
    }
}

