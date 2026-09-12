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

    /**
     * Các thiết bị đang đăng nhập của một tài khoản, mới dùng nhất lên trước.
     *
     * `lastUsedAt` là lần XOAY VÒNG TOKEN gần nhất, không phải lần gọi API gần
     * nhất: cập nhật mỗi request sẽ thêm một lượt ghi DB vào đường xác thực nóng.
     * Access token sống 1 ngày nên mốc này có thể trễ tới chừng đó.
     *
     * @param currentAccessToken token gốc của thiết bị đang gọi, để đánh dấu "máy này"
     */
    java.util.List<com.didan.social.dto.SessionDTO> listSessions(String userId, String currentAccessToken);

    /**
     * Đăng xuất MỘT thiết bị theo sessionId. Chỉ chủ phiên gọi được — nếu không
     * thì bất kỳ ai cũng đá được người khác ra chỉ bằng một id đoán trúng.
     */
    void closeSessionById(String userId, String sessionId) throws Exception;
}
