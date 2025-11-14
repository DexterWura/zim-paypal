package com.zim.paypal.controller;

import com.zim.paypal.model.entity.User;
import com.zim.paypal.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Admin controller for admin dashboard and user management
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final com.zim.paypal.service.SupportService supportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> statistics = adminService.getDashboardStatistics();
        // Add support ticket statistics
        try {
            Map<String, Object> ticketStats = supportService.getTicketStatistics();
            statistics.putAll(ticketStats);
        } catch (Exception e) {
            // Support service might not be initialized
        }
        model.addAttribute("stats", statistics);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search,
                       Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;
        
        if (search != null && !search.isEmpty()) {
            users = adminService.searchUsers(search, pageable);
        } else {
            users = adminService.getAllUsers(pageable);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("search", search);
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = adminService.getUserById(id);
        model.addAttribute("user", user);
        return "admin/user-detail";
    }

    @PostMapping("/users/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                @RequestParam User.UserRole role,
                                RedirectAttributes redirectAttributes) {
        try {
            adminService.updateUserRole(id, role);
            redirectAttributes.addFlashAttribute("success", "User role updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/enable")
    public String setUserEnabled(@PathVariable Long id,
                                @RequestParam Boolean enabled,
                                RedirectAttributes redirectAttributes) {
        try {
            adminService.setUserEnabled(id, enabled);
            redirectAttributes.addFlashAttribute("success", 
                    "User account " + (enabled ? "enabled" : "disabled") + " successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/lock")
    public String setUserLocked(@PathVariable Long id,
                               @RequestParam Boolean locked,
                               RedirectAttributes redirectAttributes) {
        try {
            adminService.setUserLocked(id, locked);
            redirectAttributes.addFlashAttribute("success", 
                    "User account " + (locked ? "locked" : "unlocked") + " successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "50") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.zim.paypal.model.entity.Transaction> transactions = 
                adminService.getAllTransactions(pageable);
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        return "admin/transactions";
    }

    @GetMapping("/accounts")
    public String accounts(Model model) {
        model.addAttribute("accounts", adminService.getAllAccounts());
        return "admin/accounts";
    }
}

