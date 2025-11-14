package com.zim.paypal.service;

import com.zim.paypal.model.dto.KycVerificationDto;
import com.zim.paypal.model.entity.KycVerification;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.KycVerificationRepository;
import com.zim.paypal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for KYC verification management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KycService {

    private final KycVerificationRepository kycVerificationRepository;
    private final UserRepository userRepository;

    public KycVerification createVerification(KycVerificationDto dto, User createdBy) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        KycVerification verification = KycVerification.builder()
                .user(user)
                .verificationLevel(dto.getVerificationLevel())
                .verificationStatus(KycVerification.VerificationStatus.PENDING)
                .documentType(dto.getDocumentType())
                .documentNumber(dto.getDocumentNumber())
                .documentPath(dto.getDocumentPath())
                .dateOfBirth(dto.getDateOfBirth())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .postalCode(dto.getPostalCode())
                .phoneNumber(dto.getPhoneNumber())
                .verificationNotes(dto.getVerificationNotes())
                .expiresAt(dto.getExpiresAt())
                .createdBy(createdBy)
                .build();

        return kycVerificationRepository.save(verification);
    }

    public KycVerification approveVerification(Long verificationId, User verifiedBy, String notes) {
        KycVerification verification = kycVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));

        verification.setVerificationStatus(KycVerification.VerificationStatus.APPROVED);
        verification.setVerifiedBy(verifiedBy);
        verification.setVerifiedAt(java.time.LocalDateTime.now());
        verification.setVerificationNotes(notes);

        return kycVerificationRepository.save(verification);
    }

    public KycVerification rejectVerification(Long verificationId, User reviewedBy, String notes) {
        KycVerification verification = kycVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));

        verification.setVerificationStatus(KycVerification.VerificationStatus.REJECTED);
        verification.setVerifiedBy(reviewedBy);
        verification.setVerifiedAt(java.time.LocalDateTime.now());
        verification.setVerificationNotes(notes);

        return kycVerificationRepository.save(verification);
    }

    @Transactional(readOnly = true)
    public KycVerification getVerificationById(Long verificationId) {
        return kycVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
    }

    @Transactional(readOnly = true)
    public List<KycVerification> getPendingVerifications() {
        return kycVerificationRepository.findByVerificationStatusOrderByCreatedAtDesc(
                KycVerification.VerificationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public KycVerification getLatestByUser(User user) {
        return kycVerificationRepository.findLatestByUser(user)
                .orElse(null);
    }
}

