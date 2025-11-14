package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Report;
import com.zim.paypal.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Report entity
 * 
 * @author dexterwura
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Find reports by user
     * 
     * @param user User entity
     * @param pageable Pageable object
     * @return Page of reports
     */
    Page<Report> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find reports by user and type
     * 
     * @param user User entity
     * @param reportType Report type
     * @param pageable Pageable object
     * @return Page of reports
     */
    Page<Report> findByUserAndReportTypeOrderByCreatedAtDesc(User user, 
                                                              Report.ReportType reportType, 
                                                              Pageable pageable);
}

