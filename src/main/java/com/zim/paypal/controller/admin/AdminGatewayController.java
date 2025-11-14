package com.zim.paypal.controller.admin;

import com.zim.paypal.model.entity.PaymentGateway;
import com.zim.paypal.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin controller for managing payment gateways
 * 
 * @author dexterwura
 */
@Controller
@RequestMapping("/admin/gateways")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminGatewayController {

    private final PaymentGatewayService gatewayService;

    @GetMapping
    public String listGateways(Model model) {
        List<PaymentGateway> gateways = gatewayService.getAllGateways();
        model.addAttribute("gateways", gateways);
        return "admin/gateways/list";
    }

    @GetMapping("/{id}/edit")
    public String editGateway(@PathVariable Long id, Model model) {
        PaymentGateway gateway = gatewayService.getGatewayById(id);
        model.addAttribute("gateway", gateway);
        return "admin/gateways/edit";
    }

    @PostMapping("/{id}/update")
    public String updateGateway(@PathVariable Long id,
                                @ModelAttribute PaymentGateway gateway,
                                RedirectAttributes redirectAttributes) {
        try {
            gateway.setId(id);
            gatewayService.updateGateway(gateway);
            redirectAttributes.addFlashAttribute("success", "Gateway updated successfully!");
        } catch (Exception e) {
            log.error("Error updating gateway: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error updating gateway: " + e.getMessage());
        }
        return "redirect:/admin/gateways";
    }

    @PostMapping("/{id}/toggle")
    public String toggleGateway(@PathVariable Long id,
                                @RequestParam boolean enabled,
                                RedirectAttributes redirectAttributes) {
        try {
            gatewayService.toggleGateway(id, enabled);
            redirectAttributes.addFlashAttribute("success", 
                "Gateway " + (enabled ? "enabled" : "disabled") + " successfully!");
        } catch (Exception e) {
            log.error("Error toggling gateway: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error toggling gateway: " + e.getMessage());
        }
        return "redirect:/admin/gateways";
    }
}

