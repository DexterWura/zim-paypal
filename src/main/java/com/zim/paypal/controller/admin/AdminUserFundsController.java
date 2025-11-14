package com.zim.paypal.controller.admin;

import com.zim.paypal.model.entity.User;
import com.zim.paypal.service.TransactionService;
import com.zim.paypal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin controller for adding funds to user accounts
 * 
 * @author dexterwura
 */
@Controller
@RequestMapping("/admin/users/funds")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserFundsController {

    private final UserService userService;
    private final TransactionService transactionService;

    @GetMapping
    public String showAddFundsForm(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            try {
                // Search by username or email
                User user = null;
                try {
                    user = userService.findByUsername(search);
                } catch (Exception e) {
                    try {
                        user = userService.findByEmail(search);
                    } catch (Exception ex) {
                        // User not found
                    }
                }
                if (user != null) {
                    model.addAttribute("users", List.of(user));
                } else {
                    model.addAttribute("users", List.of());
                }
            } catch (Exception e) {
                model.addAttribute("users", List.of());
            }
        }
        model.addAttribute("search", search);
        return "admin/users/add-funds";
    }

    @PostMapping("/add")
    public String addFunds(@RequestParam Long userId,
                          @RequestParam BigDecimal amount,
                          @RequestParam(required = false) String description,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(userId);
            transactionService.createDeposit(
                    userId, 
                    amount, 
                    description != null ? description : "Admin deposit");
            redirectAttributes.addFlashAttribute("success", 
                "Successfully added $" + amount + " to " + user.getUsername() + "'s account");
        } catch (Exception e) {
            log.error("Error adding funds: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error adding funds: " + e.getMessage());
        }
        return "redirect:/admin/users/funds";
    }
}

