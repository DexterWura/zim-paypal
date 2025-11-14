package com.zim.paypal.controller;

import com.zim.paypal.model.dto.ReversalRequestDto;
import com.zim.paypal.model.entity.TransactionReversal;
import com.zim.paypal.service.ReversalService;
import com.zim.paypal.service.TransactionService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for transaction reversal management
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/reversals")
@RequiredArgsConstructor
@Slf4j
public class ReversalController {

    private final ReversalService reversalService;
    private final UserService userService;
    private final TransactionService transactionService;

    @GetMapping("/request")
    public String showRequestForm(@RequestParam(required = false) Long transactionId, Model model) {
        ReversalRequestDto reversalDto = new ReversalRequestDto();
        if (transactionId != null) {
            reversalDto.setTransactionId(transactionId);
        }
        model.addAttribute("reversalDto", reversalDto);
        return "reversals/request";
    }

    @PostMapping("/request")
    public String requestReversal(@Valid @ModelAttribute ReversalRequestDto reversalDto,
                                   BindingResult result,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "reversals/request";
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            TransactionReversal reversal = reversalService.requestReversal(user.getId(), reversalDto);
            redirectAttributes.addFlashAttribute("success", 
                    "Reversal request submitted! Reversal #: " + reversal.getReversalNumber());
            return "redirect:/reversals/my-reversals";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reversals/request?transactionId=" + reversalDto.getTransactionId();
        }
    }

    @GetMapping("/my-reversals")
    public String myReversals(Authentication authentication,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionReversal> reversals = reversalService.getReversalsByUser(user.getId(), pageable);
        
        model.addAttribute("reversals", reversals);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reversals.getTotalPages());
        
        return "reversals/my-reversals";
    }

    @GetMapping("/{id}")
    public String viewReversal(@PathVariable Long id,
                               Authentication authentication,
                               Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        TransactionReversal reversal = reversalService.getReversalById(id);
        
        // Check if user requested this reversal or is admin
        if (!reversal.getRequestedBy().getId().equals(user.getId()) && 
            user.getRole() != com.zim.paypal.model.entity.User.UserRole.ADMIN) {
            return "redirect:/reversals/my-reversals";
        }
        
        model.addAttribute("reversal", reversal);
        model.addAttribute("isAdmin", user.getRole() == com.zim.paypal.model.entity.User.UserRole.ADMIN);
        
        return "reversals/detail";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reversals")
    public String adminReversals(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size,
                                @RequestParam(required = false) String status,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionReversal> reversals;
        
        if (status != null && !status.isEmpty()) {
            try {
                TransactionReversal.ReversalStatus reversalStatus = TransactionReversal.ReversalStatus.valueOf(status.toUpperCase());
                reversals = reversalService.getAllReversals(pageable);
            } catch (IllegalArgumentException e) {
                reversals = reversalService.getPendingReversals(pageable);
            }
        } else {
            reversals = reversalService.getPendingReversals(pageable);
        }
        
        model.addAttribute("reversals", reversals);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reversals.getTotalPages());
        model.addAttribute("status", status);
        model.addAttribute("stats", reversalService.getReversalStatistics());
        
        return "reversals/admin-reversals";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reversals/{id}")
    public String adminViewReversal(@PathVariable Long id, Model model) {
        TransactionReversal reversal = reversalService.getReversalById(id);
        model.addAttribute("reversal", reversal);
        model.addAttribute("isAdmin", true);
        return "reversals/detail";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/reversals/{id}/approve")
    public String approveReversal(@PathVariable Long id,
                                  @RequestParam(required = false) String notes,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            reversalService.approveReversal(id, admin.getId(), notes != null ? notes : "");
            redirectAttributes.addFlashAttribute("success", "Reversal approved!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reversals/admin/reversals/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/reversals/{id}/reject")
    public String rejectReversal(@PathVariable Long id,
                                 @RequestParam String notes,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            reversalService.rejectReversal(id, admin.getId(), notes);
            redirectAttributes.addFlashAttribute("success", "Reversal rejected!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reversals/admin/reversals/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/reversals/{id}/process")
    public String processReversal(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User admin = userService.findByUsername(authentication.getName());
            reversalService.processReversal(id, admin.getId());
            redirectAttributes.addFlashAttribute("success", "Reversal processed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reversals/admin/reversals/" + id;
    }
}

