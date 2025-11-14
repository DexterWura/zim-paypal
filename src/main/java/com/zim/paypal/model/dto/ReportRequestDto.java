package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.Report;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for report generation request
 * 
 * @author dexterwura
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {

    @NotNull(message = "Report type is required")
    private Report.ReportType reportType;

    @Builder.Default
    private Report.ReportFormat format = Report.ReportFormat.PDF;

    private LocalDate startDate;

    private LocalDate endDate;

    private String parameters; // JSON string for additional parameters
}

