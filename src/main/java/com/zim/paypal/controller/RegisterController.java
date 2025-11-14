package com.zim.paypal.controller;

import com.zim.paypal.model.dto.RegisterDto;
import com.zim.paypal.service.CountryRestrictionService;
import com.zim.paypal.service.FeatureFlagService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for user registration
 * 
 * @author dexterwura
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RegisterController {

    private final UserService userService;
    private final FeatureFlagService featureFlagService;
    private final CountryRestrictionService countryRestrictionService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        // Check if country restrictions feature is enabled
        if (featureFlagService.isFeatureEnabled(FeatureFlag.FeatureNames.COUNTRY_RESTRICTIONS)) {
            model.addAttribute("enabledCountries", countryRestrictionService.getEnabledCountries());
        }
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid RegisterDto registerDto, BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        // Check if country restrictions feature is enabled
        if (featureFlagService.isFeatureEnabled(FeatureFlag.FeatureNames.COUNTRY_RESTRICTIONS)) {
            if (registerDto.getCountryCode() != null && !registerDto.getCountryCode().isEmpty()) {
                if (!countryRestrictionService.isRegistrationAllowed(registerDto.getCountryCode())) {
                    result.rejectValue("countryCode", "error.countryCode", 
                            "Registration is not allowed from your country");
                }
            } else {
                result.rejectValue("countryCode", "error.countryCode", 
                        "Country is required");
            }
        }

        if (result.hasErrors()) {
            if (featureFlagService.isFeatureEnabled(FeatureFlag.FeatureNames.COUNTRY_RESTRICTIONS)) {
                model.addAttribute("enabledCountries", countryRestrictionService.getEnabledCountries());
            }
            return "register";
        }

        try {
            userService.register(registerDto);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            if (featureFlagService.isFeatureEnabled(FeatureFlag.FeatureNames.COUNTRY_RESTRICTIONS)) {
                model.addAttribute("enabledCountries", countryRestrictionService.getEnabledCountries());
            }
            return "register";
        }
    }
}

