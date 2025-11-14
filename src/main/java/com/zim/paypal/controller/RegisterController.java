package com.zim.paypal.controller;

import com.zim.paypal.model.dto.RegisterRequest;
import com.zim.paypal.model.entity.FeatureFlag;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.service.CountryRestrictionService;
import com.zim.paypal.service.FeatureFlagService;
import com.zim.paypal.service.RewardsService;
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
    private final RewardsService rewardsService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        // Check if country restrictions feature is enabled
        if (featureFlagService.isFeatureEnabled(FeatureFlag.FeatureNames.COUNTRY_RESTRICTIONS)) {
            model.addAttribute("enabledCountries", countryRestrictionService.findByIsEnabledTrueAndIsRegistrationAllowedTrue());
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid RegisterRequest registerRequest, BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        // Check if country restrictions feature is enabled
        if (featureFlagService.isFeatureEnabled(FeatureFlag.FeatureNames.COUNTRY_RESTRICTIONS)) {
            if (registerRequest.getCountryCode() != null && !registerRequest.getCountryCode().isEmpty()) {
                if (!countryRestrictionService.isRegistrationAllowed(registerRequest.getCountryCode())) {
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
                model.addAttribute("enabledCountries", countryRestrictionService.findByIsEnabledTrueAndIsRegistrationAllowedTrue());
            }
            return "register";
        }

        try {
            // Determine user role from form selection
            User.UserRole userRole = User.UserRole.USER;
            if (registerRequest.getRole() != null && registerRequest.getRole().equalsIgnoreCase("ADMIN")) {
                userRole = User.UserRole.ADMIN;
                log.info("Admin registration selected for user: {}", registerRequest.getUsername());
            }
            
            User user = User.builder()
                    .username(registerRequest.getUsername())
                    .email(registerRequest.getEmail())
                    .password(registerRequest.getPassword())
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .phoneNumber(registerRequest.getPhoneNumber())
                    .countryCode(registerRequest.getCountryCode())
                    .role(userRole)
                    .build();

            User savedUser = userService.registerUser(user);
            
            // Initialize rewards for new user
            try {
                rewardsService.initializeRewards(savedUser);
            } catch (Exception e) {
                log.warn("Failed to initialize rewards: {}", e.getMessage());
            }
            
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            if (featureFlagService.isFeatureEnabled(FeatureFlag.FeatureNames.COUNTRY_RESTRICTIONS)) {
                model.addAttribute("enabledCountries", countryRestrictionService.findByIsEnabledTrueAndIsRegistrationAllowedTrue());
            }
            return "register";
        }
    }
}

