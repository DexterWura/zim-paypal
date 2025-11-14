package com.zim.paypal.service;

import com.zim.paypal.model.entity.SuspiciousActivity;
import com.zim.paypal.model.entity.User;
import com.zim.paypal.repository.SuspiciousActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for suspicious activity management
 * 
 * @author Zim Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SuspiciousActivityService {

    private final SuspiciousActivityRepository suspiciousActivityRepository;

    @Transactional(readOnly = true)
    public Page<SuspiciousActivity> getPendingActivities(Pageable pageable) {
        return suspiciousActivityRepository.findByStatusOrderByCreatedAtDesc(
                SuspiciousActivity.Status.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SuspiciousActivity> getActivitiesByUser(User user, Pageable pageable) {
        return suspiciousActivityRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public SuspiciousActivity getActivityById(Long activityId) {
        return suspiciousActivityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));
    }

    public SuspiciousActivity reviewActivity(Long activityId, SuspiciousActivity.Status status, 
                                            User reviewedBy, String reviewNotes) {
        SuspiciousActivity activity = getActivityById(activityId);
        activity.setStatus(status);
        activity.setReviewedBy(reviewedBy);
        activity.setReviewNotes(reviewNotes);
        activity.setReviewedAt(java.time.LocalDateTime.now());

        return suspiciousActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return suspiciousActivityRepository.countByStatus(SuspiciousActivity.Status.PENDING);
    }
}

