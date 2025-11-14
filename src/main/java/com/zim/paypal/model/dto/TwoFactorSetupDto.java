package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.TwoFactorAuth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for 2FA setup
 * 
 * @author Zim Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupDto {

    @NotNull(message = "Auth method is required")
    private TwoFactorAuth.AuthMethod method;

    @NotBlank(message = "Phone number is required for SMS method")
    private String phoneNumber;

    @NotBlank(message = "Verification code is required")
    private String verificationCode;
}

