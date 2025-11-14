package com.zim.paypal.controller.api;

import com.zim.paypal.model.dto.ApiResponse;
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
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for Report Management
 * 
 * @author dexterwura
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Report>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Page<Report> reports = reportService.getReportsByUser(user, PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success(reports));
        } catch (Exception e) {
            log.error("Error getting reports: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Report>> generateReport(@Valid @RequestBody ReportRequestDto reportRequest,
                                                              Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Report report = reportService.generateReport(reportRequest, user);
            return ResponseEntity.ok(ApiResponse.success("Report generation started", report));
        } catch (Exception e) {
            log.error("Error generating report: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<Report>> getReport(@PathVariable Long reportId,
                                                        Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Report report = reportService.getReportById(reportId);
            
            if (!report.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            log.error("Error getting report: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
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

