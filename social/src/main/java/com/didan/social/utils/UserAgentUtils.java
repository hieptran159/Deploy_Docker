package com.didan.social.utils;

import org.springframework.util.StringUtils;

/**
 * Rút User-Agent thành nhãn ngắn cho màn hình "Thiết bị đang đăng nhập"
 * (ví dụ "Chrome trên Windows").
 *
 * Cố tình KHÔNG kéo thư viện parse UA về: ở đây chỉ cần đủ để người dùng nhận ra
 * máy của mình, sai một nhãn không gây hại gì. Chuỗi UA gốc vẫn nằm nguyên trong
 * DB nếu sau này cần soi kỹ.
 *
 * ponytail: so khớp chuỗi thô; đổi sang thư viện UA nếu nào đó cần thống kê thật.
 */
public final class UserAgentUtils {

    private UserAgentUtils() {}

    public static final String UNKNOWN = "Thiết bị không rõ";

    public static String label(String ua) {
        if (!StringUtils.hasText(ua)) return UNKNOWN;
        String browser = browser(ua);
        String os = os(ua);
        if (browser == null && os == null) return UNKNOWN;
        if (browser == null) return os;
        if (os == null) return browser;
        return browser + " trên " + os;
    }

    private static String browser(String ua) {
        // Thứ tự quan trọng: Edge/Opera/Coc Coc đều tự nhận là Chrome, Chrome tự
        // nhận là Safari. Cái nào đặc thù hơn phải xét trước.
        if (ua.contains("Edg/") || ua.contains("Edge/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("coc_coc_browser")) return "Cốc Cốc";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        return null;
    }

    private static String os(String ua) {
        // Android phải xét trước Linux: UA của Android có cả hai.
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return null;
    }
}
