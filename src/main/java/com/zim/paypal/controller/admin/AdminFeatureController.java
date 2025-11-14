package com.zim.paypal.controller.admin;

import com.zim.paypal.model.entity.FeatureFlag;
import com.zim.paypal.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin controller for feature flag management
 * 
 * @author dexterwura
 */
@Controller
@RequestMapping("/admin/features")
@RequiredArgsConstructor
@Slf4j
public class AdminFeatureController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public String featureFlags(Model model) {
        List<FeatureFlag> flags = featureFlagService.getAllFeatureFlags();
        
        // Group by category
        Map<String, List<FeatureFlag>> flagsByCategory = flags.stream()
                .collect(Collectors.groupingBy(
                    flag -> flag.getCategory() != null ? flag.getCategory() : "OTHER",
                    Collectors.toList()
                ));
        
        model.addAttribute("flagsByCategory", flagsByCategory);
        model.addAttribute("categories", flagsByCategory.keySet());
        return "admin/features";
    }

    @PostMapping("/{featureName}/toggle")
    public String toggleFeature(@PathVariable String featureName,
                               @RequestParam Boolean enabled,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            featureFlagService.toggleFeature(featureName, enabled, authentication.getName());
            redirectAttributes.addFlashAttribute("success", 
                    "Feature " + featureName + " " + (enabled ? "enabled" : "disabled") + " successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/features";
    }
}

