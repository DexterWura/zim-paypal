package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.BillSplit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for bill split creation
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillSplitDto {

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal totalAmount;

    @NotNull(message = "Split method is required")
    private BillSplit.SplitMethod splitMethod;

    @NotNull(message = "Participants are required")
    @Size(min = 1, message = "At least one participant is required")
    private List<ParticipantDto> participants;

    /**
     * DTO for participant
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDto {
        @NotBlank(message = "Participant email is required")
        private String email;

        private BigDecimal amount; // For custom split
        private BigDecimal percentage; // For percentage split
    }
}

