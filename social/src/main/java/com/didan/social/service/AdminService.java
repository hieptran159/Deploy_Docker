package com.didan.social.service;

import com.didan.social.dto.BlacklistUserDTO;

import java.util.List;

public interface AdminService {
    // Phân quyền cho user làm admin
    boolean grantAdmin(String userId) throws Exception;
    List<BlacklistUserDTO> getAllBlacklistUser() throws Exception;
    boolean blockUser(String userId) throws Exception;
    boolean unblockUser(String userId) throws Exception;
    // Số liệu tổng quan cho bảng điều khiển admin
    java.util.Map<String, Object> getStats() throws Exception;
    // Nhật ký hành động admin (mới nhất trước), phân trang
    java.util.List<com.didan.social.entity.AdminLog> getLogs(int page, int size) throws Exception;
}
