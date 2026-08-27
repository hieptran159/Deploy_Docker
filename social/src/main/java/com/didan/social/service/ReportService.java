package com.didan.social.service;

import com.didan.social.dto.ReportDTO;

import java.util.List;

public interface ReportService {
    // Người dùng gửi báo cáo (targetType: USER | POST | COMMENT)
    boolean create(String targetType, String targetId, String reason) throws Exception;

    // Admin: danh sách báo cáo (status null/ALL = tất cả, mặc định OPEN)
    List<ReportDTO> listForAdmin(String status) throws Exception;

    // Admin: xử lý 1 báo cáo (status: RESOLVED | DISMISSED)
    boolean handle(String reportId, String status) throws Exception;
}
