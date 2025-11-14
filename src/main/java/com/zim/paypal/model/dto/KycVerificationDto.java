package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.KycVerification;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for KYC verification
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Verification level is required")
    private KycVerification.VerificationLevel verificationLevel;

    private String documentType;

    private String documentNumber;

    private String documentPath;

    private LocalDate dateOfBirth;

    private String address;

    private String city;

    private String state;

    private String country;

    private String postalCode;

    private String phoneNumber;

    private String verificationNotes;

    private LocalDateTime expiresAt;
}

