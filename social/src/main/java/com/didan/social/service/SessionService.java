package com.didan.social.service;

/**
 * Quản lý phiên đăng nhập theo THIẾT BỊ (bảng user_sessions).
 *
 * Tách riêng khỏi AuthService để AdminService / UserService gọi được mà không
 * tạo phụ thuộc vòng, và để chỗ nào cần "đá hết mọi thiết bị" cũng dùng chung
 * một đường, không viết lại logic thu hồi ở nhiều nơi.
 */
public interface SessionService {

    /** Mở phiên mới cho một thiết bị. Trả về [accessToken, refreshToken] bản gốc. */
    String[] openSession(String userId, boolean remember);

    /** Đăng xuất một thiết bị: chặn token đó và xoá đúng hàng phiên của nó. */
    void closeSession(String accessToken);

    /** Thu hồi MỌI phiên của tài khoản: bị chặn, tự vô hiệu hoá, xoá tài khoản. */
    void revokeAllSessions(String userId);
}
