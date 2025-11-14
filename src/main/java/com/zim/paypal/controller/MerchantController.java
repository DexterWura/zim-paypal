package com.zim.paypal.controller;

import com.zim.paypal.model.dto.PaymentButtonDto;
import com.zim.paypal.model.entity.MerchantApiKey;
import com.zim.paypal.model.entity.PaymentButton;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.service.MerchantApiKeyService;
import com.zim.paypal.service.PaymentButtonService;
import com.zim.paypal.service.CurrencyService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for merchant tools management
 * 
 * @author dexterwura
 */
@Controller
@RequestMapping("/merchant")
@RequiredArgsConstructor
@Slf4j
public class MerchantController {

    private final PaymentButtonService buttonService;
    private final MerchantApiKeyService apiKeyService;
    private final CurrencyService currencyService;
    private final UserService userService;
    private final FeatureFlagService featureFlagService;
    private final CountryRestrictionService countryRestrictionService;

    @Value("${app.base-url:http://localhost:80}")
    private String baseUrl;

    @GetMapping("/buttons")
    public String myButtons(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        List<PaymentButton> buttons = buttonService.getPaymentButtonsByMerchant(user);
        model.addAttribute("buttons", buttons);
        return "merchant/buttons";
    }

    @GetMapping("/buttons/create")
    public String showCreateButtonForm(Model model) {
        model.addAttribute("buttonDto", new PaymentButtonDto());
        model.addAttribute("currencies", currencyService.getAllActiveCurrencies());
        model.addAttribute("buttonStyles", PaymentButton.ButtonStyle.values());
        model.addAttribute("buttonSizes", PaymentButton.ButtonSize.values());
        return "merchant/create-button";
    }

    @PostMapping("/buttons/create")
    public String createButton(@Valid @ModelAttribute PaymentButtonDto buttonDto,
                              BindingResult result,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("currencies", currencyService.getAllActiveCurrencies());
            redirectAttributes.addFlashAttribute("buttonStyles", PaymentButton.ButtonStyle.values());
            redirectAttributes.addFlashAttribute("buttonSizes", PaymentButton.ButtonSize.values());
            return "merchant/create-button";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            PaymentButton button = buttonService.createPaymentButton(buttonDto, user);
            redirectAttributes.addFlashAttribute("success", 
                    "Payment button created! Button Code: " + button.getButtonCode());
            return "redirect:/merchant/buttons";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/merchant/buttons/create";
        }
    }

    @GetMapping("/buttons/{id}")
    public String buttonDetail(@PathVariable Long id,
                              Authentication authentication,
                              Model model) {
        User user = userService.findByUsername(authentication.getName());
        PaymentButton button = buttonService.getPaymentButtonById(id);
        
        if (!button.getMerchant().getId().equals(user.getId())) {
            return "redirect:/merchant/buttons";
        }
        
        String embedCode = buttonService.generateEmbedCode(button, baseUrl);
        model.addAttribute("button", button);
        model.addAttribute("embedCode", embedCode);
        return "merchant/button-detail";
    }

    @GetMapping("/api-keys")
    public String myApiKeys(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        List<MerchantApiKey> apiKeys = apiKeyService.getApiKeysByMerchant(user);
        model.addAttribute("apiKeys", apiKeys);
        return "merchant/api-keys";
    }

    @PostMapping("/api-keys/create")
    public String createApiKey(@RequestParam String keyName,
                               @RequestParam(required = false) String description,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            MerchantApiKey apiKey = apiKeyService.createApiKey(keyName, description, user);
            redirectAttributes.addFlashAttribute("success", 
                    "API Key created! Save your secret now - it won't be shown again.");
            redirectAttributes.addFlashAttribute("apiKey", apiKey.getApiKey());
            redirectAttributes.addFlashAttribute("apiSecret", apiKey.getApiSecret());
            return "redirect:/merchant/api-keys";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/merchant/api-keys";
        }
    }

    @PostMapping("/api-keys/{id}/revoke")
    public String revokeApiKey(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            apiKeyService.revokeApiKey(id, user);
            redirectAttributes.addFlashAttribute("success", "API key revoked successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/merchant/api-keys";
    }
}

