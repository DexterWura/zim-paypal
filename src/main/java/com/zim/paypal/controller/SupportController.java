package com.zim.paypal.controller;

import com.zim.paypal.model.dto.SupportTicketDto;
import com.zim.paypal.model.dto.TicketMessageDto;
import com.zim.paypal.model.entity.SupportTicket;
import com.zim.paypal.model.entity.TicketMessage;
import com.zim.paypal.service.SupportService;
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

import java.util.List;

/**
 * Controller for support ticket management
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {

    private final SupportService supportService;
    private final UserService userService;

    @GetMapping("/tickets")
    public String myTickets(Authentication authentication,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets = supportService.getTicketsByUser(user.getId(), pageable);
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tickets.getTotalPages());
        return "support/tickets";
    }

    @GetMapping("/tickets/new")
    public String showCreateTicketForm(Model model) {
        model.addAttribute("ticketDto", new SupportTicketDto());
        return "support/create-ticket";
    }

    @PostMapping("/tickets/new")
    public String createTicket(@Valid @ModelAttribute SupportTicketDto ticketDto,
                               BindingResult result,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "support/create-ticket";
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            SupportTicket ticket = supportService.createTicket(user.getId(), ticketDto);
            redirectAttributes.addFlashAttribute("success", 
                    "Support ticket created successfully! Ticket #: " + ticket.getTicketNumber());
            return "redirect:/support/tickets";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/support/tickets/new";
        }
    }

    @GetMapping("/tickets/{id}")
    public String viewTicket(@PathVariable Long id,
                            Authentication authentication,
                            Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        SupportTicket ticket = supportService.getTicketById(id);
        
        // Check if user owns the ticket or is admin
        if (!ticket.getUser().getId().equals(user.getId()) && 
            user.getRole() != com.zim.paypal.model.entity.User.UserRole.ADMIN) {
            return "redirect:/support/tickets";
        }
        
        boolean isAdmin = user.getRole() == com.zim.paypal.model.entity.User.UserRole.ADMIN;
        List<TicketMessage> messages = supportService.getTicketMessages(id, isAdmin);
        
        model.addAttribute("ticket", ticket);
        model.addAttribute("messages", messages);
        model.addAttribute("messageDto", new TicketMessageDto());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("user", user);
        
        return "support/ticket-detail";
    }

    @PostMapping("/tickets/{id}/message")
    public String addMessage(@PathVariable Long id,
                            @Valid @ModelAttribute TicketMessageDto messageDto,
                            BindingResult result,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "redirect:/support/tickets/" + id;
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            supportService.addMessage(id, user.getId(), messageDto);
            redirectAttributes.addFlashAttribute("success", "Message added successfully!");
            return "redirect:/support/tickets/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/support/tickets/" + id;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/tickets")
    public String adminTickets(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "50") int size,
                              @RequestParam(required = false) String status,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets;
        
        if (status != null && !status.isEmpty()) {
            try {
                SupportTicket.TicketStatus ticketStatus = SupportTicket.TicketStatus.valueOf(status.toUpperCase());
                tickets = supportService.getTicketsByStatus(ticketStatus, pageable);
            } catch (IllegalArgumentException e) {
                tickets = supportService.getOpenTickets(pageable);
            }
        } else {
            tickets = supportService.getOpenTickets(pageable);
        }
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tickets.getTotalPages());
        model.addAttribute("status", status);
        model.addAttribute("stats", supportService.getTicketStatistics());
        
        return "support/admin-tickets";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/tickets/{id}")
    public String adminViewTicket(@PathVariable Long id, Model model) {
        SupportTicket ticket = supportService.getTicketById(id);
        List<TicketMessage> messages = supportService.getTicketMessages(id, true);
        
        model.addAttribute("ticket", ticket);
        model.addAttribute("messages", messages);
        model.addAttribute("messageDto", new TicketMessageDto());
        model.addAttribute("isAdmin", true);
        
        return "support/ticket-detail";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/tickets/{id}/assign")
    public String assignTicket(@PathVariable Long id,
                              @RequestParam Long adminId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            supportService.assignTicket(id, adminId);
            redirectAttributes.addFlashAttribute("success", "Ticket assigned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/support/admin/tickets/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/tickets/{id}/status")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam SupportTicket.TicketStatus status,
                              RedirectAttributes redirectAttributes) {
        try {
            supportService.updateTicketStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Ticket status updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/support/admin/tickets/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/tickets/{id}/resolve")
    public String resolveTicket(@PathVariable Long id,
                               @RequestParam String resolution,
                               RedirectAttributes redirectAttributes) {
        try {
            supportService.resolveTicket(id, resolution);
            redirectAttributes.addFlashAttribute("success", "Ticket resolved!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/support/admin/tickets/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/tickets/{id}/close")
    public String closeTicket(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            supportService.closeTicket(id);
            redirectAttributes.addFlashAttribute("success", "Ticket closed!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/support/admin/tickets/" + id;
    }
}

