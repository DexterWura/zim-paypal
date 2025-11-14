package com.zim.paypal.controller;

import com.zim.paypal.model.entity.PaymentLink;
import com.zim.paypal.service.PaymentLinkService;
import com.zim.paypal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Public controller for payment link pages (no authentication required)
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/pay")
@RequiredArgsConstructor
@Slf4j
public class PublicPaymentController {

    private final PaymentLinkService paymentLinkService;
    private final UserService userService;

    @GetMapping("/{linkCode}")
    public String showPaymentPage(@PathVariable String linkCode, Model model) {
        try {
            PaymentLink link = paymentLinkService.getPaymentLinkByCode(linkCode);
            
            if (!link.isValid()) {
                model.addAttribute("error", "This payment link is no longer valid");
                return "payment-links/invalid";
            }

            model.addAttribute("link", link);
            return "payment-links/pay";
        } catch (Exception e) {
            model.addAttribute("error", "Payment link not found");
            return "payment-links/invalid";
        }
    }

    @PostMapping("/{linkCode}")
    public String processPayment(@PathVariable String linkCode,
                                 @RequestParam String payerEmail,
                                 @RequestParam BigDecimal amount,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            // If user is authenticated, use their account
            String email = payerEmail;
            if (authentication != null && authentication.isAuthenticated()) {
                com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
                email = user.getEmail();
            }

            paymentLinkService.processPaymentLink(linkCode, email, amount);
            redirectAttributes.addFlashAttribute("success", "Payment processed successfully!");
            return "redirect:/pay/" + linkCode + "/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pay/" + linkCode;
        }
    }

    @GetMapping("/{linkCode}/success")
    public String paymentSuccess(@PathVariable String linkCode, Model model) {
        PaymentLink link = paymentLinkService.getPaymentLinkByCode(linkCode);
        model.addAttribute("link", link);
        return "payment-links/success";
    }
}

