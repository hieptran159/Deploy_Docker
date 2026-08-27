package com.didan.social.service;

import com.didan.social.dto.NotificationDTO;

import java.util.List;

public interface NotificationService {
    // Tạo thông báo (bỏ qua nếu người nhận trùng người tạo)
    void push(String recipientId, String actorId, String type, String targetId, String message);
    // Như push nhưng bỏ qua nếu đã có 1 thông báo CHƯA ĐỌC cùng (recipient, type, targetId)
    void pushUnique(String recipientId, String actorId, String type, String targetId, String message);
    // Danh sách thông báo của người dùng hiện tại (mới nhất trước)
    List<NotificationDTO> listMine() throws Exception;
    // Số thông báo chưa đọc của người dùng hiện tại
    long unreadCountMine() throws Exception;
    // Đánh dấu tất cả đã đọc
    void markAllRead() throws Exception;
    // Đánh dấu 1 thông báo đã đọc
    void markRead(String notificationId) throws Exception;
}
