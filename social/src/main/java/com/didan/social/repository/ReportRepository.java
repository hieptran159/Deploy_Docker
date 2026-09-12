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
    long countByStatus(String status);

    /**
     * [targetType, targetId, số báo cáo OPEN] cho cả trang bằng MỘT truy vấn, thay vì
     * countByTargetTypeAndTargetIdAndStatus cho từng dòng.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT r.targetType, r.targetId, COUNT(r) FROM reports r "
          + "WHERE r.status = 'OPEN' AND r.targetId IN :ids GROUP BY r.targetType, r.targetId")
    java.util.List<Object[]> countOpenByTargets(
            @org.springframework.data.repository.query.Param("ids") java.util.Collection<String> ids);
}
