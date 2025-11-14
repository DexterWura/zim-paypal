package com.zim.paypal.service;

import com.zim.paypal.model.dto.ReportRequestDto;
import com.zim.paypal.model.entity.*;
import com.zim.paypal.repository.ReportRepository;
import com.zim.paypal.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for report generation and management
 * 
 * @author dexterwura
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final AccountService accountService;
    private static final String REPORT_DIR = "reports";

    /**
     * Generate report
     * 
     * @param reportRequest Report request DTO
     * @param user User entity
     * @return Created report
     */
    public Report generateReport(ReportRequestDto reportRequest, User user) {
        Report report = Report.builder()
                .user(user)
                .reportType(reportRequest.getReportType())
                .format(reportRequest.getFormat())
                .status(Report.ReportStatus.PENDING)
                .startDate(reportRequest.getStartDate())
                .endDate(reportRequest.getEndDate())
                .parameters(reportRequest.getParameters())
                .build();

        Report saved = reportRepository.save(report);
        
        // Generate report asynchronously
        generateReportAsync(saved);
        
        return saved;
    }

    /**
     * Generate report asynchronously
     * 
     * @param report Report entity
     */
    @Async
    public void generateReportAsync(Report report) {
        try {
            report.setStatus(Report.ReportStatus.PROCESSING);
            reportRepository.save(report);

            Map<String, Object> data = collectReportData(report);
            byte[] fileContent = generateReportFile(report, data);
            
            // Save file
            String fileName = generateFileName(report);
            Path reportDir = Paths.get(REPORT_DIR);
            if (!Files.exists(reportDir)) {
                Files.createDirectories(reportDir);
            }
            
            Path filePath = reportDir.resolve(fileName);
            Files.write(filePath, fileContent);
            
            report.markAsCompleted(filePath.toString(), fileName, (long) fileContent.length);
            reportRepository.save(report);
            
            log.info("Report generated successfully: {}", report.getId());
        } catch (Exception e) {
            log.error("Error generating report {}: {}", report.getId(), e.getMessage(), e);
            report.markAsFailed(e.getMessage());
            reportRepository.save(report);
        }
    }

    /**
     * Collect report data based on report type
     * 
     * @param report Report entity
     * @return Report data map
     */
    private Map<String, Object> collectReportData(Report report) {
        Map<String, Object> data = new HashMap<>();
        User user = report.getUser();
        
        switch (report.getReportType()) {
            case TRANSACTION_SUMMARY:
                data.putAll(generateTransactionSummary(user, report.getStartDate(), report.getEndDate()));
                break;
            case TRANSACTION_DETAIL:
                data.putAll(generateTransactionDetail(user, report.getStartDate(), report.getEndDate()));
                break;
            case ACCOUNT_STATEMENT:
                data.putAll(generateAccountStatement(user, report.getStartDate(), report.getEndDate()));
                break;
            case REVENUE_REPORT:
                data.putAll(generateRevenueReport(user, report.getStartDate(), report.getEndDate()));
                break;
            default:
                data.put("message", "Report type not yet implemented");
        }
        
        data.put("reportType", report.getReportType().name());
        data.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        data.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "name", user.getFullName()
        ));
        
        return data;
    }

    /**
     * Generate transaction summary report
     */
    private Map<String, Object> generateTransactionSummary(User user, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = getTransactionsInRange(user, startDate, endDate);
        
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getReceiver() != null && t.getReceiver().getId().equals(user.getId()))
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getSender() != null && t.getSender().getId().equals(user.getId()))
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTransactions", transactions.size());
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpenses", totalExpenses);
        summary.put("netAmount", totalIncome.subtract(totalExpenses));
        summary.put("transactions", transactions.stream()
                .map(t -> Map.of(
                    "id", t.getId(),
                    "transactionNumber", t.getTransactionNumber(),
                    "type", t.getTransactionType().name(),
                    "amount", t.getAmount(),
                    "status", t.getStatus().name(),
                    "date", t.getCreatedAt().toString()
                ))
                .collect(Collectors.toList()));
        
        return summary;
    }

    /**
     * Generate transaction detail report
     */
    private Map<String, Object> generateTransactionDetail(User user, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = getTransactionsInRange(user, startDate, endDate);
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("transactions", transactions.stream()
                .map(t -> {
                    Map<String, Object> tx = new HashMap<>();
                    tx.put("id", t.getId());
                    tx.put("transactionNumber", t.getTransactionNumber());
                    tx.put("type", t.getTransactionType().name());
                    tx.put("amount", t.getAmount());
                    tx.put("currencyCode", t.getCurrencyCode());
                    tx.put("status", t.getStatus().name());
                    tx.put("description", t.getDescription());
                    tx.put("sender", t.getSender() != null ? t.getSender().getFullName() : "N/A");
                    tx.put("receiver", t.getReceiver() != null ? t.getReceiver().getFullName() : "N/A");
                    tx.put("createdAt", t.getCreatedAt().toString());
                    return tx;
                })
                .collect(Collectors.toList()));
        detail.put("totalCount", transactions.size());
        
        return detail;
    }

    /**
     * Generate account statement report
     */
    private Map<String, Object> generateAccountStatement(User user, LocalDate startDate, LocalDate endDate) {
        Account account = accountService.findActiveAccountByUser(user);
        List<Transaction> transactions = getTransactionsInRange(user, startDate, endDate);
        
        Map<String, Object> statement = new HashMap<>();
        statement.put("accountNumber", account.getAccountNumber());
        statement.put("accountBalance", account.getBalance());
        statement.put("currencyCode", account.getCurrencyCode());
        statement.put("openingBalance", calculateOpeningBalance(account, startDate));
        statement.put("closingBalance", account.getBalance());
        statement.put("transactions", transactions);
        statement.put("period", Map.of("start", startDate != null ? startDate.toString() : "N/A",
                                      "end", endDate != null ? endDate.toString() : "N/A"));
        
        return statement;
    }

    /**
     * Generate revenue report
     */
    private Map<String, Object> generateRevenueReport(User user, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = getTransactionsInRange(user, startDate, endDate);
        
        BigDecimal revenue = transactions.stream()
                .filter(t -> t.getReceiver() != null && t.getReceiver().getId().equals(user.getId()))
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> revenueReport = new HashMap<>();
        revenueReport.put("totalRevenue", revenue);
        revenueReport.put("transactionCount", transactions.size());
        revenueReport.put("averageTransaction", transactions.isEmpty() ? BigDecimal.ZERO :
                revenue.divide(new BigDecimal(transactions.size()), 2, java.math.RoundingMode.HALF_UP));
        
        return revenueReport;
    }

    /**
     * Get transactions in date range
     */
    private List<Transaction> getTransactionsInRange(User user, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusYears(1);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        return transactionRepository.findByUserAndDateRange(user, start, end, 
                org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    /**
     * Calculate opening balance
     */
    private BigDecimal calculateOpeningBalance(Account account, LocalDate startDate) {
        if (startDate == null) {
            return account.getBalance();
        }
        // Simplified - in production, calculate from transactions before start date
        return account.getBalance();
    }

    /**
     * Generate report file based on format
     */
    private byte[] generateReportFile(Report report, Map<String, Object> data) throws IOException {
        switch (report.getFormat()) {
            case PDF:
                return generatePdfReport(data);
            case CSV:
                return generateCsvReport(data);
            case EXCEL:
                return generateExcelReport(data);
            case JSON:
                return generateJsonReport(data);
            default:
                return generateCsvReport(data);
        }
    }

    /**
     * Generate PDF report
     */
    private byte[] generatePdfReport(Map<String, Object> data) throws IOException {
        // Simplified PDF generation - in production, use iText or Apache PDFBox
        StringBuilder content = new StringBuilder();
        content.append("Report: ").append(data.get("reportType")).append("\n");
        content.append("Generated At: ").append(data.get("generatedAt")).append("\n");
        content.append("User: ").append(((Map<?, ?>) data.get("user")).get("name")).append("\n\n");
        
        if (data.containsKey("totalTransactions")) {
            content.append("Total Transactions: ").append(data.get("totalTransactions")).append("\n");
            content.append("Total Income: ").append(data.get("totalIncome")).append("\n");
            content.append("Total Expenses: ").append(data.get("totalExpenses")).append("\n");
        }
        
        return content.toString().getBytes();
    }

    /**
     * Generate CSV report
     */
    private byte[] generateCsvReport(Map<String, Object> data) throws IOException {
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("Report Type,Generated At,User\n");
        csv.append(data.get("reportType")).append(",")
           .append(data.get("generatedAt")).append(",")
           .append(((Map<?, ?>) data.get("user")).get("name")).append("\n");
        
        // Data rows
        if (data.containsKey("transactions")) {
            csv.append("\nTransaction Details\n");
            csv.append("ID,Transaction Number,Type,Amount,Status,Date\n");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) data.get("transactions");
            for (Map<String, Object> tx : transactions) {
                csv.append(tx.get("id")).append(",")
                   .append(tx.get("transactionNumber")).append(",")
                   .append(tx.get("type")).append(",")
                   .append(tx.get("amount")).append(",")
                   .append(tx.get("status")).append(",")
                   .append(tx.get("date")).append("\n");
            }
        }
        
        return csv.toString().getBytes();
    }

    /**
     * Generate Excel report
     */
    private byte[] generateExcelReport(Map<String, Object> data) throws IOException {
        // Simplified - in production, use Apache POI
        return generateCsvReport(data); // Fallback to CSV for now
    }

    /**
     * Generate JSON report
     */
    private byte[] generateJsonReport(Map<String, Object> data) throws IOException {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.writeValueAsBytes(data);
    }

    /**
     * Generate file name
     */
    private String generateFileName(Report report) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = report.getFormat().name().toLowerCase();
        return String.format("report_%s_%s.%s", report.getReportType().name(), timestamp, extension);
    }

    /**
     * Get reports by user
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of reports
     */
    @Transactional(readOnly = true)
    public Page<Report> getReportsByUser(User user, Pageable pageable) {
        return reportRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get report by ID
     * 
     * @param reportId Report ID
     * @return Report entity
     */
    @Transactional(readOnly = true)
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
    }

    /**
     * Download report file
     * 
     * @param report Report entity
     * @return File bytes
     */
    public byte[] downloadReport(Report report) throws IOException {
        if (report.getStatus() != Report.ReportStatus.COMPLETED || report.getFilePath() == null) {
            throw new IllegalStateException("Report not ready for download");
        }
        
        Path filePath = Paths.get(report.getFilePath());
        return Files.readAllBytes(filePath);
    }
}

