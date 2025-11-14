package com.zim.paypal.controller;

import com.zim.paypal.model.dto.CheckoutRequestDto;
import com.zim.paypal.model.entity.PaymentButton;
import com.zim.paypal.service.CheckoutService;
import com.zim.paypal.service.PaymentButtonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Public controller for checkout pages (no authentication required)
 * 
 * @author dexterwura
 */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class PublicCheckoutController {

    private final PaymentButtonService buttonService;
    private final CheckoutService checkoutService;

    @GetMapping("/{buttonCode}")
    public String showCheckoutPage(@PathVariable String buttonCode, Model model) {
        try {
            PaymentButton button = buttonService.getPaymentButtonByCode(buttonCode);
            
            if (!button.getIsActive()) {
                model.addAttribute("error", "This payment button is no longer active");
                return "merchant/checkout-invalid";
            }

            model.addAttribute("button", button);
            model.addAttribute("checkoutRequest", new CheckoutRequestDto());
            return "merchant/checkout";
        } catch (Exception e) {
            model.addAttribute("error", "Payment button not found");
            return "merchant/checkout-invalid";
        }
    }

    @PostMapping("/{buttonCode}")
    public String processCheckout(@PathVariable String buttonCode,
                                 @ModelAttribute CheckoutRequestDto checkoutRequest,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            checkoutRequest.setButtonCode(buttonCode);
            
            // If user is authenticated, use their account
            if (authentication != null && authentication.isAuthenticated()) {
                // In production, get email from authenticated user
            }

            checkoutService.processCheckout(checkoutRequest);
            redirectAttributes.addFlashAttribute("success", "Payment processed successfully!");
            
            PaymentButton button = buttonService.getPaymentButtonByCode(buttonCode);
            if (button.getSuccessUrl() != null) {
                return "redirect:" + button.getSuccessUrl();
            }
            return "redirect:/checkout/" + buttonCode + "/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout/" + buttonCode;
        }
    }

    @GetMapping("/{buttonCode}/success")
    public String checkoutSuccess(@PathVariable String buttonCode, Model model) {
        PaymentButton button = buttonService.getPaymentButtonByCode(buttonCode);
        model.addAttribute("button", button);
        return "merchant/checkout-success";
    }
}

