package com.zim.paypal.controller;

import com.zim.paypal.model.dto.TwoFactorSetupDto;
import com.zim.paypal.model.entity.TwoFactorAuth;
import com.zim.paypal.service.TwoFactorService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Two-Factor Authentication
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/2fa")
@RequiredArgsConstructor
@Slf4j
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserService userService;

    @GetMapping("/setup")
    public String showSetupPage(Authentication authentication, Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        TwoFactorAuth twoFactorAuth = twoFactorService.getOrCreateTwoFactorAuth(user);
        
        model.addAttribute("twoFactorAuth", twoFactorAuth);
        model.addAttribute("setupDto", new TwoFactorSetupDto());
        model.addAttribute("methods", TwoFactorAuth.AuthMethod.values());
        model.addAttribute("isEnabled", twoFactorAuth.getIsEnabled());
        
        return "2fa/setup";
    }

    @PostMapping("/setup/initiate")
    public String initiateSetup(@RequestParam TwoFactorAuth.AuthMethod method,
                               @RequestParam(required = false) String phoneNumber,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            
            // Update phone number if SMS method
            if (method == TwoFactorAuth.AuthMethod.SMS && phoneNumber != null) {
                TwoFactorAuth twoFactorAuth = twoFactorService.getOrCreateTwoFactorAuth(user);
                twoFactorAuth.setPhoneNumber(phoneNumber);
            }
            
            // Send verification code
            String code = twoFactorService.sendVerificationCode(user, method);
            
            // In production, don't show code to user
            redirectAttributes.addFlashAttribute("code", code);
            redirectAttributes.addFlashAttribute("method", method);
            redirectAttributes.addFlashAttribute("message", 
                    "Verification code sent! Please enter it to complete setup.");
            
            return "redirect:/2fa/verify?method=" + method;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/2fa/setup";
        }
    }

    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam(required = false) TwoFactorAuth.AuthMethod method,
                                Authentication authentication,
                                Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        TwoFactorAuth twoFactorAuth = twoFactorService.getOrCreateTwoFactorAuth(user);
        
        model.addAttribute("method", method != null ? method : twoFactorAuth.getMethod());
        model.addAttribute("setupDto", new TwoFactorSetupDto());
        
        return "2fa/verify";
    }

    @PostMapping("/setup/complete")
    public String completeSetup(@Valid @ModelAttribute TwoFactorSetupDto setupDto,
                               BindingResult result,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "2fa/verify";
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            
            // Verify code
            boolean isValid = twoFactorService.verifyCode(user, setupDto.getVerificationCode());
            
            if (!isValid) {
                redirectAttributes.addFlashAttribute("error", "Invalid verification code");
                return "redirect:/2fa/verify?method=" + setupDto.getMethod();
            }

            // Mark as verified and enable
            twoFactorService.markAsVerified(user);
            
            // Generate TOTP secret if using TOTP/APP
            String secret = null;
            if (setupDto.getMethod() == TwoFactorAuth.AuthMethod.TOTP || 
                setupDto.getMethod() == TwoFactorAuth.AuthMethod.APP) {
                secret = twoFactorService.generateTotpSecret();
            }
            
            twoFactorService.enableTwoFactorAuth(user, setupDto.getMethod(), 
                    secret, setupDto.getPhoneNumber());
            
            redirectAttributes.addFlashAttribute("success", 
                    "Two-Factor Authentication enabled successfully!");
            return "redirect:/2fa/setup";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/2fa/verify?method=" + setupDto.getMethod();
        }
    }

    @PostMapping("/disable")
    public String disable2FA(Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            twoFactorService.disableTwoFactorAuth(user);
            redirectAttributes.addFlashAttribute("success", 
                    "Two-Factor Authentication disabled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/2fa/setup";
    }

    @GetMapping("/backup-codes")
    public String showBackupCodes(Authentication authentication, Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        TwoFactorAuth twoFactorAuth = twoFactorService.getOrCreateTwoFactorAuth(user);
        
        if (twoFactorAuth.getBackupCodes() != null) {
            String[] codes = twoFactorAuth.getBackupCodes().split(",");
            model.addAttribute("backupCodes", codes);
        }
        
        return "2fa/backup-codes";
    }

    @PostMapping("/regenerate-backup-codes")
    public String regenerateBackupCodes(Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            TwoFactorAuth twoFactorAuth = twoFactorService.getOrCreateTwoFactorAuth(user);
            
            if (!twoFactorAuth.getIsEnabled()) {
                redirectAttributes.addFlashAttribute("error", "2FA is not enabled");
                return "redirect:/2fa/setup";
            }

            // Generate new backup codes
            java.util.List<String> codes = twoFactorService.generateBackupCodes(10);
            twoFactorAuth.setBackupCodes(String.join(",", codes));
            // Save would be done in service, but for now update directly
            redirectAttributes.addFlashAttribute("success", "Backup codes regenerated!");
            redirectAttributes.addFlashAttribute("backupCodes", codes);
            
            return "redirect:/2fa/backup-codes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/2fa/setup";
        }
    }
}

