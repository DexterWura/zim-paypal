package com.zim.paypal.controller;

import com.zim.paypal.model.dto.ReportRequestDto;
import com.zim.paypal.model.entity.Report;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.service.ReportService;
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
 * Controller for report management (Web UI)
 * 
 * @author dexterwura
 */
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @GetMapping
    public String myReports(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Authentication authentication,
                           Model model) {
        User user = userService.findByUsername(authentication.getName());
        Page<Report> reports = reportService.getReportsByUser(user, PageRequest.of(page, size));
        
        model.addAttribute("reports", reports);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reports.getTotalPages());
        model.addAttribute("reportTypes", Report.ReportType.values());
        model.addAttribute("reportFormats", Report.ReportFormat.values());
        return "reports/list";
    }

    @PostMapping("/generate")
    public String generateReport(@Valid @ModelAttribute ReportRequestDto reportRequest,
                                BindingResult result,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid report request");
            return "redirect:/reports";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            Report report = reportService.generateReport(reportRequest, user);
            redirectAttributes.addFlashAttribute("success", 
                    "Report generation started! Report ID: " + report.getId());
            return "redirect:/reports";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reports";
        }
    }

    @GetMapping("/{reportId}")
    public String reportDetail(@PathVariable Long reportId,
                              Authentication authentication,
                              Model model) {
        User user = userService.findByUsername(authentication.getName());
        Report report = reportService.getReportById(reportId);
        
        if (!report.getUser().getId().equals(user.getId())) {
            return "redirect:/reports";
        }
        
        model.addAttribute("report", report);
        return "reports/detail";
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long reportId,
                                                 Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Report report = reportService.getReportById(reportId);
            
            if (!report.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            byte[] fileContent = reportService.downloadReport(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(report.getFormat()));
            headers.setContentDispositionFormData("attachment", report.getFileName());
            headers.setContentLength(fileContent.length);
            
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error downloading report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get media type for report format
     */
    private MediaType getMediaType(Report.ReportFormat format) {
        return switch (format) {
            case PDF -> MediaType.APPLICATION_PDF;
            case CSV -> MediaType.parseMediaType("text/csv");
            case EXCEL -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case JSON -> MediaType.APPLICATION_JSON;
        };
    }
}

