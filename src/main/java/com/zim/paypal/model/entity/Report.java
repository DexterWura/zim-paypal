package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Report entity for storing report configurations and history
 * 
 * @author dexterwura
 */
@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_user", columnList = "user_id"),
    @Index(name = "idx_report_type", columnList = "report_type"),
    @Index(name = "idx_report_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    @Builder.Default
    private ReportFormat format = ReportFormat.PDF;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters; // JSON string for report parameters

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Enumeration for report types
     */
    public enum ReportType {
        TRANSACTION_SUMMARY,
        TRANSACTION_DETAIL,
        ACCOUNT_STATEMENT,
        REVENUE_REPORT,
        EXPENSE_REPORT,
        USER_ACTIVITY,
        FRAUD_ANALYSIS,
        AML_COMPLIANCE,
        TAX_REPORT,
        CUSTOM
    }

    /**
     * Enumeration for report formats
     */
    public enum ReportFormat {
        PDF, CSV, EXCEL, JSON
    }

    /**
     * Enumeration for report status
     */
    public enum ReportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    /**
     * Mark report as completed
     */
    public void markAsCompleted(String filePath, String fileName, Long fileSize) {
        this.status = ReportStatus.COMPLETED;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark report as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = ReportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}

