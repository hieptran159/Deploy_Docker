package com.didan.social.service;


import com.didan.social.dto.ConversationDTO;
import com.didan.social.dto.MessageDTO;
import com.didan.social.payload.request.SendMessageRequest;

import java.util.List;
import java.util.Map;

public interface ChatService {
    // Tạo 1 phòng chat
    String createConversation(String conversationName) throws Exception;
    // Mở (hoặc tạo) cuộc trò chuyện 1-1 với một người dùng, cả hai đều là participant
    ConversationDTO openDirectConversation(String otherUserId) throws Exception;
    // Thêm 1 người dùng vào nhóm (người gọi phải là thành viên của nhóm)
    boolean addMember(String conversationId, String userId) throws Exception;
    // Danh sách thành viên của 1 hội thoại (người gọi phải là thành viên)
    List<Map<String, String>> getMembers(String conversationId) throws Exception;
    // Tham gia vào phòng chat
    ConversationDTO joinConversation(String conversationId) throws Exception;
    // Rời khỏi phòng chat
    boolean leaveConversation(String conversationId) throws Exception;
    // Gửi tin nhắn trong phòng chat
    MessageDTO sendMessage(String conversationId, SendMessageRequest sendMessageRequest) throws Exception;
    // Lấy ra tất cả nhóm chat đã tham gia
    List<ConversationDTO> getAllConversation() throws Exception;
    // Tìm kiếm nhóm chat để tham gia
    List<ConversationDTO> searchConversation(String conversationName) throws Exception;
    // Lấy ra tất cả đoạn chat trong nhóm chat
    List<MessageDTO> getAllMessagesInConversation(String conversationId) throws Exception;
    // Sửa nội dung 1 tin nhắn (chỉ chủ tin nhắn, tin chưa thu hồi)
    MessageDTO editMessage(String messageId, String content) throws Exception;

    // Thả / bỏ cảm xúc cho tin nhắn; trả về map emoji -> số lượt sau thao tác
    java.util.Map<String, Long> reactMessage(String messageId, String emoji) throws Exception;
    java.util.Map<String, Long> unreactMessage(String messageId) throws Exception;

    // Đặt ảnh đại diện nhóm; trả về đường dẫn ảnh đã lưu
    String setConversationAvatar(String conversationId, org.springframework.web.multipart.MultipartFile avatar) throws Exception;
    // Thu hồi 1 tin nhắn (chỉ chủ tin nhắn)
    MessageDTO recallMessage(String messageId) throws Exception;
    // Đổi tên nhóm (người gọi phải là thành viên, không áp dụng cho DM)
    boolean renameConversation(String conversationId, String newName) throws Exception;

    // Bật/tắt thông báo hội thoại cho người dùng hiện tại
    boolean setConversationMuted(String conversationId, boolean muted) throws Exception;
    // Xoá 1 thành viên khỏi nhóm (người gọi phải là thành viên, không áp dụng cho DM)
    boolean removeMember(String conversationId, String userId) throws Exception;
}
