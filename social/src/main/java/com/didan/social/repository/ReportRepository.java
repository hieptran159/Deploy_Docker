package com.didan.social.repository;

import com.didan.social.entity.Reports;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Reports, String> {
    List<Reports> findTop300ByStatusOrderByCreatedAtDesc(String status);
    List<Reports> findTop300ByOrderByCreatedAtDesc();
    Reports findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(String reporterId, String targetType, String targetId, String status);
    long countByTargetTypeAndTargetIdAndStatus(String targetType, String targetId, String status);
}
