package com.zim.paypal.controller.admin;

import com.zim.paypal.model.entity.CountryRestriction;
import com.zim.paypal.service.CountryRestrictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin controller for country restriction management
 * 
 * @author dexterwura
 */
@Controller
@RequestMapping("/admin/countries")
@RequiredArgsConstructor
@Slf4j
public class AdminCountryController {

    private final CountryRestrictionService countryRestrictionService;

    @GetMapping
    public String countries(Model model) {
        List<CountryRestriction> countries = countryRestrictionService.getAllCountryRestrictions();
        model.addAttribute("countries", countries);
        return "admin/countries";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("countryRestriction", new CountryRestriction());
        return "admin/country-form";
    }

    @PostMapping("/create")
    public String createCountry(@ModelAttribute CountryRestriction countryRestriction,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            countryRestrictionService.saveCountryRestriction(countryRestriction, authentication.getName());
            redirectAttributes.addFlashAttribute("success", 
                    "Country " + countryRestriction.getCountryName() + " added successfully");
            return "redirect:/admin/countries";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/countries/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        CountryRestriction country = countryRestrictionService.getAllCountryRestrictions().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Country not found"));
        model.addAttribute("countryRestriction", country);
        return "admin/country-form";
    }

    @PostMapping("/{id}/edit")
    public String updateCountry(@PathVariable Long id,
                               @ModelAttribute CountryRestriction countryRestriction,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            countryRestriction.setId(id);
            countryRestrictionService.saveCountryRestriction(countryRestriction, authentication.getName());
            redirectAttributes.addFlashAttribute("success", 
                    "Country " + countryRestriction.getCountryName() + " updated successfully");
            return "redirect:/admin/countries";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/countries/" + id + "/edit";
        }
    }

    @PostMapping("/{countryCode}/toggle")
    public String toggleCountry(@PathVariable String countryCode,
                              @RequestParam Boolean enabled,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            countryRestrictionService.toggleCountry(countryCode, enabled, authentication.getName());
            redirectAttributes.addFlashAttribute("success", 
                    "Country " + countryCode + " " + (enabled ? "enabled" : "disabled") + " successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/countries";
    }
}

