package com.didan.social.repository;

import com.didan.social.entity.AdminLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, String> {
    List<AdminLog> findByOrderByCreatedAtDesc(Pageable pageable);
}
