package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.Invoice;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for invoice creation
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {

    @NotBlank(message = "Customer email is required")
    private String customerEmail;

    private String customerName;

    private String customerAddress;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be at least 0.01")
    private BigDecimal totalAmount;

    private Long currencyId;

    private BigDecimal taxAmount;

    private BigDecimal discountAmount;

    private String notes;

    private String terms;

    @Builder.Default
    private List<InvoiceItemDto> items = new ArrayList<>();
}

