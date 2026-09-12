package com.didan.social.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Xác định IP của client. Một chỗ duy nhất, vì đây là quyết định bảo mật:
 * RateLimitFilter dùng nó làm khoá đếm, màn hình "Thiết bị đang đăng nhập" dùng
 * nó để người dùng nhận ra phiên lạ. Hai nơi mà lệch nhau thì cái sai khó thấy.
 */
public final class ClientIpUtils {

    private ClientIpUtils() {}

    /**
     * @param trustForwarded chỉ bật khi đứng sau proxy tin cậy. Tắt (mặc định) thì
     *                       dùng IP TCP thật — client không giả được bằng header.
     */
    public static String resolve(HttpServletRequest request, boolean trustForwarded) {
        if (request == null) return null;
        if (trustForwarded) {
            // Header do proxy ghi đè (1 giá trị) -> đáng tin hơn X-Forwarded-For
            String cf = request.getHeader("CF-Connecting-IP");
            if (StringUtils.hasText(cf)) return cf.trim();
            String real = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(real)) return real.trim();
            // X-Forwarded-For: lấy entry CUỐI (hop tin cậy gần nhất thêm vào),
            // tránh phần đầu do client tự bịa để giả IP.
            String xff = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff)) {
                String[] parts = xff.split(",");
                return parts[parts.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
