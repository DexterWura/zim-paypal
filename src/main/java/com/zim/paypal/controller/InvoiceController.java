package com.zim.paypal.controller;

import com.zim.paypal.model.dto.InvoiceDto;
import com.zim.paypal.model.entity.Invoice;
import com.zim.paypal.service.InvoiceService;
import com.zim.paypal.service.CurrencyService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for invoice management
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CurrencyService currencyService;
    private final UserService userService;

    @GetMapping
    public String myInvoices(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            Authentication authentication,
                            Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        Page<Invoice> sentInvoices = invoiceService.getInvoicesByMerchant(user, PageRequest.of(page, size));
        Page<Invoice> receivedInvoices = invoiceService.getInvoicesByCustomer(user, PageRequest.of(page, size));
        
        model.addAttribute("sentInvoices", sentInvoices);
        model.addAttribute("receivedInvoices", receivedInvoices);
        model.addAttribute("currentPage", page);
        return "invoices/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("invoiceDto", new InvoiceDto());
        model.addAttribute("currencies", currencyService.getAllActiveCurrencies());
        return "invoices/create";
    }

    @PostMapping("/create")
    public String createInvoice(@Valid @ModelAttribute InvoiceDto invoiceDto,
                               BindingResult result,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("currencies", currencyService.getAllActiveCurrencies());
            return "invoices/create";
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            Invoice invoice = invoiceService.createInvoice(invoiceDto, user);
            redirectAttributes.addFlashAttribute("success", 
                    "Invoice created! Invoice #: " + invoice.getInvoiceNumber());
            return "redirect:/invoices";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices/create";
        }
    }

    @GetMapping("/{id}")
    public String invoiceDetail(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        return "invoices/detail";
    }

    @PostMapping("/{id}/send")
    public String sendInvoice(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            invoiceService.sendInvoice(id);
            redirectAttributes.addFlashAttribute("success", "Invoice sent successfully!");
            return "redirect:/invoices/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices/" + id;
        }
    }

    @PostMapping("/{id}/pay")
    public String payInvoice(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            invoiceService.payInvoice(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Invoice paid successfully!");
            return "redirect:/invoices/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices/" + id;
        }
    }
}

