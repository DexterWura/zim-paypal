package com.zim.paypal.controller;

import com.zim.paypal.model.dto.ServicePurchaseDto;
import com.zim.paypal.model.entity.ServicePurchase;
import com.zim.paypal.model.entity.ServiceProvider;
import com.zim.paypal.service.ServicePurchaseService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for service purchase management (airtime, data, ZESA tokens)
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/services")
@RequiredArgsConstructor
@Slf4j
public class ServicePurchaseController {

    private final ServicePurchaseService purchaseService;
    private final UserService userService;

    @GetMapping("/buy")
    public String showPurchaseForm(@RequestParam(required = false) String type, Model model) {
        ServicePurchaseDto purchaseDto = new ServicePurchaseDto();
        
        // Set default service type if provided
        if (type != null) {
            try {
                purchaseDto.setServiceType(ServicePurchase.ServiceType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Invalid type, use default
            }
        }
        
        // Get providers based on service type
        List<ServiceProvider> providers;
        if (purchaseDto.getServiceType() != null) {
            providers = purchaseService.getProvidersByServiceType(purchaseDto.getServiceType());
        } else {
            providers = purchaseService.getActiveProviders();
        }
        
        model.addAttribute("purchaseDto", purchaseDto);
        model.addAttribute("providers", providers);
        model.addAttribute("serviceTypes", ServicePurchase.ServiceType.values());
        
        return "services/purchase";
    }

    @PostMapping("/buy")
    public String purchaseService(@Valid @ModelAttribute ServicePurchaseDto purchaseDto,
                                 BindingResult result,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            List<ServiceProvider> providers = purchaseService.getProvidersByServiceType(purchaseDto.getServiceType());
            redirectAttributes.addFlashAttribute("providers", providers);
            redirectAttributes.addFlashAttribute("serviceTypes", ServicePurchase.ServiceType.values());
            return "services/purchase";
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            ServicePurchase purchase = purchaseService.purchaseService(user.getId(), purchaseDto);
            redirectAttributes.addFlashAttribute("success", 
                    "Purchase successful! Reference #: " + purchase.getReferenceNumber());
            return "redirect:/services/my-purchases";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/services/buy?type=" + (purchaseDto.getServiceType() != null ? purchaseDto.getServiceType() : "");
        }
    }

    @GetMapping("/my-purchases")
    public String myPurchases(Authentication authentication,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<ServicePurchase> purchases = purchaseService.getPurchasesByUser(user.getId(), pageable);
        
        model.addAttribute("purchases", purchases);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", purchases.getTotalPages());
        
        return "services/my-purchases";
    }

    @GetMapping("/{referenceNumber}")
    public String viewPurchase(@PathVariable String referenceNumber,
                              Authentication authentication,
                              Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        ServicePurchase purchase = purchaseService.getPurchaseByReference(referenceNumber);
        
        // Check if user owns this purchase
        if (!purchase.getUser().getId().equals(user.getId())) {
            return "redirect:/services/my-purchases";
        }
        
        model.addAttribute("purchase", purchase);
        return "services/detail";
    }
}

