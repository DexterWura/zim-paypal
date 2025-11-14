package com.zim.paypal.controller;

import com.zim.paypal.model.dto.SubscriptionDto;
import com.zim.paypal.model.entity.RecurringPayment;
import com.zim.paypal.model.entity.SubscriptionPlan;
import com.zim.paypal.service.SubscriptionService;
import com.zim.paypal.service.SubscriptionPlanService;
import com.zim.paypal.service.UserService;
import com.zim.paypal.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for subscription management
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionPlanService planService;
    private final UserService userService;
    private final AccountService accountService;

    @GetMapping
    public String mySubscriptions(Authentication authentication, Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        List<RecurringPayment> subscriptions = subscriptionService.getSubscriptionsBySubscriber(user);
        model.addAttribute("subscriptions", subscriptions);
        return "subscriptions/list";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam(required = false) Long planId,
                                Authentication authentication,
                                Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        List<com.zim.paypal.model.entity.Account> accounts = accountService.findByUser(user);
        List<SubscriptionPlan> plans = planService.getAllActivePlans();
        
        SubscriptionDto subscriptionDto = new SubscriptionDto();
        if (planId != null) {
            SubscriptionPlan plan = planService.getPlanById(planId);
            subscriptionDto.setPlanId(planId);
            subscriptionDto.setAmount(plan.getAmount());
            subscriptionDto.setBillingCycle(plan.getBillingCycle());
        }
        
        model.addAttribute("subscriptionDto", subscriptionDto);
        model.addAttribute("accounts", accounts);
        model.addAttribute("plans", plans);
        model.addAttribute("billingCycles", SubscriptionPlan.BillingCycle.values());
        return "subscriptions/create";
    }

    @PostMapping("/create")
    public String createSubscription(@Valid @ModelAttribute SubscriptionDto subscriptionDto,
                                    @RequestParam(required = false) String merchantEmail,
                                    BindingResult result,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("accounts", accountService.findByUser(
                    userService.findByUsername(authentication.getName())));
            redirectAttributes.addFlashAttribute("plans", planService.getAllActivePlans());
            redirectAttributes.addFlashAttribute("billingCycles", SubscriptionPlan.BillingCycle.values());
            return "subscriptions/create";
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            
            // If merchant email provided, find merchant
            if (merchantEmail != null && !merchantEmail.isEmpty()) {
                com.zim.paypal.model.entity.User merchant = userService.findByEmail(merchantEmail);
                subscriptionDto.setMerchantId(merchant.getId());
            }
            
            RecurringPayment subscription = subscriptionService.createSubscription(subscriptionDto, user);
            redirectAttributes.addFlashAttribute("success", 
                    "Subscription created! Subscription ID: " + subscription.getSubscriptionId());
            return "redirect:/subscriptions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/subscriptions/create";
        }
    }

    @GetMapping("/{subscriptionId}")
    public String subscriptionDetail(@PathVariable String subscriptionId,
                                   Authentication authentication,
                                   Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        RecurringPayment subscription = subscriptionService.getSubscriptionById(subscriptionId);
        
        if (!subscription.getSubscriber().getId().equals(user.getId())) {
            return "redirect:/subscriptions";
        }
        
        model.addAttribute("subscription", subscription);
        return "subscriptions/detail";
    }

    @PostMapping("/{subscriptionId}/cancel")
    public String cancelSubscription(@PathVariable String subscriptionId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            subscriptionService.cancelSubscription(subscriptionId, user);
            redirectAttributes.addFlashAttribute("success", "Subscription cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/subscriptions";
    }

    @PostMapping("/{subscriptionId}/pause")
    public String pauseSubscription(@PathVariable String subscriptionId,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            subscriptionService.pauseSubscription(subscriptionId, user);
            redirectAttributes.addFlashAttribute("success", "Subscription paused successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/subscriptions";
    }

    @PostMapping("/{subscriptionId}/resume")
    public String resumeSubscription(@PathVariable String subscriptionId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            subscriptionService.resumeSubscription(subscriptionId, user);
            redirectAttributes.addFlashAttribute("success", "Subscription resumed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/subscriptions";
    }
}

