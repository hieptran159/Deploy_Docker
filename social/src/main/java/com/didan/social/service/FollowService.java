package com.didan.social.service;

import com.didan.social.dto.FollowDTO;

public interface FollowService {
    // Danh sách bạn bè (đã chấp nhận) của userId
    FollowDTO getFriends(String userId) throws Exception;
    // Lời mời kết bạn tôi ĐANG NHẬN (chờ tôi chấp nhận)
    FollowDTO getIncomingRequests() throws Exception;
    // Lời mời kết bạn tôi ĐÃ GỬI (chờ đối phương chấp nhận)
    FollowDTO getOutgoingRequests() throws Exception;
    // Quan hệ giữa tôi và userId: self | none | pending_out | pending_in | friends
    String friendStatus(String userId) throws Exception;

    boolean sendRequest(String userId) throws Exception;      // gửi lời mời
    boolean acceptRequest(String requesterId) throws Exception; // chấp nhận lời mời từ requesterId
    boolean declineRequest(String requesterId) throws Exception; // từ chối lời mời từ requesterId
    boolean cancelRequest(String userId) throws Exception;     // huỷ lời mời mình đã gửi
    boolean unfriend(String userId) throws Exception;          // huỷ kết bạn

    // Dùng cho tính năng khác (vd: thêm thành viên nhóm)
    boolean areFriends(String a, String b);
    // Danh sách id bạn bè đã kết bạn của 1 người (không cần ngữ cảnh đăng nhập)
    java.util.List<String> friendIdsOf(String userId);

    // ----- Chặn người dùng -----
    boolean blockUser(String userId) throws Exception;   // chặn (đồng thời huỷ mọi quan hệ bạn/lời mời 2 chiều)
    boolean unblockUser(String userId) throws Exception;  // bỏ chặn
    FollowDTO getBlocked() throws Exception;              // danh sách id tôi đã chặn
    // Tập id có quan hệ chặn với userId (tôi chặn họ HOẶC họ chặn tôi) - không cần ngữ cảnh đăng nhập
    java.util.Set<String> blockRelatedIds(String userId);
    boolean isBlockedEither(String a, String b);
}
