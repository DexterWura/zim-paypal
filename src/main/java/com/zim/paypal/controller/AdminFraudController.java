package com.zim.paypal.controller;

import com.zim.paypal.model.dto.FraudRuleDto;
import com.zim.paypal.model.dto.KycVerificationDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin controller for fraud, AML, and security management
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/admin/fraud")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminFraudController {

    private final FraudRuleService fraudRuleService;
    private final SuspiciousActivityService suspiciousActivityService;
    private final KycService kycService;
    private final FraudDetectionService fraudDetectionService;
    private final AmlService amlService;
    private final UserService userService;

    // ========== FRAUD RULES ==========

    @GetMapping("/rules")
    public String fraudRules(Model model) {
        List<FraudRule> rules = fraudRuleService.getAllRules();
        model.addAttribute("rules", rules);
        return "admin/fraud/rules";
    }

    @GetMapping("/rules/new")
    public String showCreateRuleForm(Model model) {
        model.addAttribute("ruleDto", new FraudRuleDto());
        model.addAttribute("ruleTypes", FraudRule.RuleType.values());
        model.addAttribute("actionTypes", FraudRule.ActionType.values());
        return "admin/fraud/rule-form";
    }

    @PostMapping("/rules/new")
    public String createRule(@Valid @ModelAttribute FraudRuleDto ruleDto,
                            BindingResult result,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("ruleTypes", FraudRule.RuleType.values());
            redirectAttributes.addFlashAttribute("actionTypes", FraudRule.ActionType.values());
            return "admin/fraud/rule-form";
        }

        try {
            User admin = userService.findByUsername(authentication.getName());
            fraudRuleService.createRule(ruleDto, admin);
            redirectAttributes.addFlashAttribute("success", "Fraud rule created successfully!");
            return "redirect:/admin/fraud/rules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fraud/rules/new";
        }
    }

    @GetMapping("/rules/{id}/edit")
    public String showEditRuleForm(@PathVariable Long id, Model model) {
        FraudRule rule = fraudRuleService.getRuleById(id);
        FraudRuleDto ruleDto = FraudRuleDto.builder()
                .ruleName(rule.getRuleName())
                .ruleCode(rule.getRuleCode())
                .ruleType(rule.getRuleType())
                .actionType(rule.getActionType())
                .thresholdAmount(rule.getThresholdAmount())
                .thresholdCount(rule.getThresholdCount())
                .timeWindowMinutes(rule.getTimeWindowMinutes())
                .riskScoreThreshold(rule.getRiskScoreThreshold())
                .isActive(rule.getIsActive())
                .description(rule.getDescription())
                .ruleConditions(rule.getRuleConditions())
                .build();
        model.addAttribute("ruleDto", ruleDto);
        model.addAttribute("ruleId", id);
        model.addAttribute("ruleTypes", FraudRule.RuleType.values());
        model.addAttribute("actionTypes", FraudRule.ActionType.values());
        return "admin/fraud/rule-form";
    }

    @PostMapping("/rules/{id}/edit")
    public String updateRule(@PathVariable Long id,
                            @Valid @ModelAttribute FraudRuleDto ruleDto,
                            BindingResult result,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("ruleId", id);
            redirectAttributes.addFlashAttribute("ruleTypes", FraudRule.RuleType.values());
            redirectAttributes.addFlashAttribute("actionTypes", FraudRule.ActionType.values());
            return "admin/fraud/rule-form";
        }

        try {
            User admin = userService.findByUsername(authentication.getName());
            fraudRuleService.updateRule(id, ruleDto, admin);
            redirectAttributes.addFlashAttribute("success", "Fraud rule updated successfully!");
            return "redirect:/admin/fraud/rules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fraud/rules/" + id + "/edit";
        }
    }

    // ========== SUSPICIOUS ACTIVITIES ==========

    @GetMapping("/activities")
    public String suspiciousActivities(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) String status,
                                      Model model) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<SuspiciousActivity> activities;

        if (status != null && !status.isEmpty()) {
            activities = suspiciousActivityService.getPendingActivities(pageable);
        } else {
            activities = suspiciousActivityService.getPendingActivities(pageable);
        }

        model.addAttribute("activities", activities);
        model.addAttribute("pendingCount", suspiciousActivityService.getPendingCount());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", activities.getTotalPages());
        return "admin/fraud/activities";
    }

    @GetMapping("/activities/{id}")
    public String activityDetail(@PathVariable Long id, Model model) {
        SuspiciousActivity activity = suspiciousActivityService.getActivityById(id);
        model.addAttribute("activity", activity);
        model.addAttribute("statuses", SuspiciousActivity.Status.values());
        return "admin/fraud/activity-detail";
    }

    @PostMapping("/activities/{id}/review")
    public String reviewActivity(@PathVariable Long id,
                                 @RequestParam SuspiciousActivity.Status status,
                                 @RequestParam(required = false) String reviewNotes,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.findByUsername(authentication.getName());
            suspiciousActivityService.reviewActivity(id, status, admin, reviewNotes);
            redirectAttributes.addFlashAttribute("success", "Activity reviewed successfully!");
            return "redirect:/admin/fraud/activities/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fraud/activities/" + id;
        }
    }

    // ========== KYC VERIFICATIONS ==========

    @GetMapping("/kyc")
    public String kycVerifications(Model model) {
        List<KycVerification> pending = kycService.getPendingVerifications();
        model.addAttribute("pendingVerifications", pending);
        return "admin/fraud/kyc";
    }

    @GetMapping("/kyc/{id}")
    public String kycDetail(@PathVariable Long id, Model model) {
        KycVerification verification = kycService.getVerificationById(id);
        model.addAttribute("verification", verification);
        return "admin/fraud/kyc-detail";
    }

    @PostMapping("/kyc/{id}/approve")
    public String approveKyc(@PathVariable Long id,
                             @RequestParam(required = false) String notes,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.findByUsername(authentication.getName());
            kycService.approveVerification(id, admin, notes);
            redirectAttributes.addFlashAttribute("success", "KYC verification approved!");
            return "redirect:/admin/fraud/kyc";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fraud/kyc/" + id;
        }
    }

    @PostMapping("/kyc/{id}/reject")
    public String rejectKyc(@PathVariable Long id,
                           @RequestParam(required = false) String notes,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.findByUsername(authentication.getName());
            kycService.rejectVerification(id, admin, notes);
            redirectAttributes.addFlashAttribute("success", "KYC verification rejected!");
            return "redirect:/admin/fraud/kyc";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fraud/kyc/" + id;
        }
    }

    // ========== RISK MONITORING ==========

    @GetMapping("/risk")
    public String riskMonitoring(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                Model model) {
        // This would typically show risk scores and high-risk users
        model.addAttribute("message", "Risk monitoring dashboard - coming soon");
        return "admin/fraud/risk";
    }
}

