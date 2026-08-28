package com.didan.social.service;

import com.didan.social.entity.AdminLog;
import com.didan.social.entity.Users;
import com.didan.social.repository.AdminLogRepository;
import com.didan.social.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Ghi nhật ký hành động admin. Fire-and-forget: nuốt lỗi để không phá vỡ hành động chính.
 */
@Service
public class AdminLogService {
    private final Logger logger = LoggerFactory.getLogger(AdminLogService.class);
    private final AdminLogRepository adminLogRepository;
    private final UserRepository userRepository;
    private final AuthorizePathService authorizePathService;

    @Autowired
    public AdminLogService(AdminLogRepository adminLogRepository, UserRepository userRepository,
                           AuthorizePathService authorizePathService) {
        this.adminLogRepository = adminLogRepository;
        this.userRepository = userRepository;
        this.authorizePathService = authorizePathService;
    }

    public void record(String action, String targetType, String targetId, String detail) {
        try {
            String adminId = authorizePathService.getUserIdAuthoried();
            Users admin = userRepository.findFirstByUserId(adminId);
            AdminLog log = new AdminLog();
            log.setId(UUID.randomUUID().toString());
            log.setAdminId(adminId);
            log.setAdminName(admin != null ? admin.getFullName() : adminId);
            log.setAction(action);
            log.setTargetType(targetType);
            log.setTargetId(targetId);
            log.setDetail(detail == null ? null : (detail.length() > 500 ? detail.substring(0, 500) : detail));
            log.setCreatedAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
            adminLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Ghi admin log lỗi: {}", e.getMessage());
        }
    }
}
