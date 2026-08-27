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
}
