package com.zim.paypal.controller;

import com.zim.paypal.model.dto.BillSplitDto;
import com.zim.paypal.model.entity.BillSplit;
import com.zim.paypal.model.entity.BillSplitParticipant;
import com.zim.paypal.service.BillSplitService;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for bill split management
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/split")
@RequiredArgsConstructor
@Slf4j
public class BillSplitController {

    private final BillSplitService billSplitService;
    private final UserService userService;

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        BillSplitDto splitDto = new BillSplitDto();
        splitDto.setParticipants(new java.util.ArrayList<>());
        // Add one empty participant by default
        splitDto.getParticipants().add(BillSplitDto.ParticipantDto.builder().build());
        model.addAttribute("splitDto", splitDto);
        return "split/create";
    }

    @PostMapping("/create")
    public String createBillSplit(@Valid @ModelAttribute BillSplitDto splitDto,
                                 BindingResult result,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "split/create";
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            BillSplit billSplit = billSplitService.createBillSplit(user.getId(), splitDto);
            redirectAttributes.addFlashAttribute("success", 
                    "Bill split created! Split #: " + billSplit.getSplitNumber());
            return "redirect:/split/my-splits";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/split/create";
        }
    }

    @GetMapping("/my-splits")
    public String mySplits(Authentication authentication,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        
        Page<BillSplit> createdSplits = billSplitService.getSplitsByCreator(user.getId(), pageable);
        Page<BillSplit> participantSplits = billSplitService.getSplitsByParticipant(user.getId(), pageable);
        
        // Get pending participants
        List<BillSplitParticipant> pendingParticipants = billSplitService.getPendingParticipants(user.getId());
        
        // Get participant amounts for each split (for display)
        Map<Long, BigDecimal> participantAmounts = new HashMap<>();
        for (BillSplit split : participantSplits.getContent()) {
            List<BillSplitParticipant> participants = billSplitService.getParticipants(split.getId());
            BillSplitParticipant userParticipant = participants.stream()
                    .filter(p -> p.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElse(null);
            if (userParticipant != null) {
                participantAmounts.put(split.getId(), userParticipant.getAmount());
            }
        }
        
        model.addAttribute("createdSplits", createdSplits);
        model.addAttribute("participantSplits", participantSplits);
        model.addAttribute("participantAmounts", participantAmounts);
        model.addAttribute("pendingParticipants", pendingParticipants);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", createdSplits.getTotalPages());
        
        return "split/my-splits";
    }

    @GetMapping("/{id}")
    public String viewSplit(@PathVariable Long id,
                           Authentication authentication,
                           Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        BillSplit billSplit = billSplitService.getSplitById(id);
        
        // Check if user is creator or participant
        boolean isCreator = billSplit.getCreator().getId().equals(user.getId());
        boolean isParticipant = billSplit.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId()));
        
        if (!isCreator && !isParticipant) {
            return "redirect:/split/my-splits";
        }
        
        List<BillSplitParticipant> participants = billSplitService.getParticipants(id);
        BillSplitParticipant userParticipant = participants.stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        
        model.addAttribute("billSplit", billSplit);
        model.addAttribute("participants", participants);
        model.addAttribute("userParticipant", userParticipant);
        model.addAttribute("isCreator", isCreator);
        model.addAttribute("user", user);
        
        return "split/detail";
    }

    @PostMapping("/{id}/pay")
    public String payShare(@PathVariable Long id,
                          @RequestParam Long participantId,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            billSplitService.payParticipantShare(participantId, user.getId());
            redirectAttributes.addFlashAttribute("success", "Your share has been paid!");
            return "redirect:/split/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/split/" + id;
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelSplit(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            billSplitService.cancelBillSplit(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Bill split cancelled.");
            return "redirect:/split/my-splits";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/split/" + id;
        }
    }
}

