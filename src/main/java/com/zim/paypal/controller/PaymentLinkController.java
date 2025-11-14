package com.zim.paypal.controller;

import com.zim.paypal.model.dto.PaymentLinkDto;
import com.zim.paypal.model.entity.PaymentLink;
import com.zim.paypal.service.PaymentLinkService;
import com.zim.paypal.service.CurrencyService;
import com.zim.paypal.service.QRCodeService;
import com.zim.paypal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for payment link management
 * 
 * @author Zim Development Team
 */
@Controller
@RequestMapping("/payment-links")
@RequiredArgsConstructor
@Slf4j
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;
    private final CurrencyService currencyService;
    private final QRCodeService qrCodeService;
    private final UserService userService;

    @GetMapping
    public String myPaymentLinks(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                Authentication authentication,
                                Model model) {
        com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
        Page<PaymentLink> links = paymentLinkService.getPaymentLinksByCreator(
                user, PageRequest.of(page, size));
        
        model.addAttribute("links", links);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", links.getTotalPages());
        return "payment-links/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("linkDto", new PaymentLinkDto());
        model.addAttribute("currencies", currencyService.getAllActiveCurrencies());
        model.addAttribute("linkTypes", PaymentLink.LinkType.values());
        return "payment-links/create";
    }

    @PostMapping("/create")
    public String createPaymentLink(@Valid @ModelAttribute PaymentLinkDto linkDto,
                                    BindingResult result,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("currencies", currencyService.getAllActiveCurrencies());
            redirectAttributes.addFlashAttribute("linkTypes", PaymentLink.LinkType.values());
            return "payment-links/create";
        }

        try {
            com.zim.paypal.model.entity.User user = userService.findByUsername(authentication.getName());
            PaymentLink link = paymentLinkService.createPaymentLink(linkDto, user);
            redirectAttributes.addFlashAttribute("success", 
                    "Payment link created! Share this link: /pay/" + link.getLinkCode());
            redirectAttributes.addFlashAttribute("linkCode", link.getLinkCode());
            return "redirect:/payment-links";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/payment-links/create";
        }
    }

    @GetMapping("/{id}")
    public String linkDetail(@PathVariable Long id, Model model) {
        PaymentLink link = paymentLinkService.getPaymentLinkById(id);
        model.addAttribute("link", link);
        model.addAttribute("paymentUrl", qrCodeService.getPaymentUrl(link.getLinkCode()));
        return "payment-links/detail";
    }

    @GetMapping("/{id}/qr")
    @ResponseBody
    public ResponseEntity<byte[]> getQRCode(@PathVariable Long id) {
        try {
            PaymentLink link = paymentLinkService.getPaymentLinkById(id);
            byte[] qrCodeImage = qrCodeService.generatePaymentLinkQRCode(link.getLinkCode());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeImage.length);
            headers.setContentDispositionFormData("attachment", "payment-link-qr-" + link.getLinkCode() + ".png");

            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generating QR code: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/qr/{linkCode}")
    @ResponseBody
    public ResponseEntity<byte[]> getQRCodeByLinkCode(@PathVariable String linkCode) {
        try {
            PaymentLink link = paymentLinkService.getPaymentLinkByCode(linkCode);
            byte[] qrCodeImage = qrCodeService.generatePaymentLinkQRCode(link.getLinkCode());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeImage.length);
            headers.setContentDispositionFormData("attachment", "payment-link-qr-" + linkCode + ".png");

            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generating QR code: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

