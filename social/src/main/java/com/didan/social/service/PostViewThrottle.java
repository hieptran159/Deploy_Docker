package com.didan.social.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chống đếm trùng lượt xem.
 *
 * Không có nó thì F5 mười lần ra mười lượt xem — con số đó không còn là "lượt
 * xem" mà là "số lần tải trang", tức là nói dối tác giả.
 *
 * Cách rẻ nhất mà vẫn trung thực: nhớ trong bộ nhớ cặp (bài, người xem) trong
 * một khoảng thời gian. Cùng người, cùng bài, trong cửa sổ đó -> chỉ tính 1.
 * Không tạo bảng, không thêm một hàng DB cho mỗi lượt xem.
 *
 * ponytail: trạng thái nằm trong RAM một tiến trình, giống hệt RateLimitFilter —
 * deploy nhiều instance thì mỗi instance đếm riêng, và khởi động lại là quên.
 * Nâng cấp khi nào chạy nhiều instance: chuyển sang Redis hoặc bảng post_views.
 */
@Component
public class PostViewThrottle {

    /** khoá "postId|viewer" -> mốc tính lượt gần nhất */
    private final Map<String, Long> seen = new ConcurrentHashMap<>();

    @Value("${app.post.view-window-ms:1800000}")
    private long windowMs;

    /** Chặn trên số khoá, để một đợt crawl không thổi bay bộ nhớ. */
    @Value("${app.post.view-cache-max:50000}")
    private int maxEntries;

    /**
     * @param viewer userId nếu đã đăng nhập, không thì IP. Null/rỗng -> không đếm,
     *               vì không phân biệt được người xem thì đếm bao nhiêu cũng vô nghĩa.
     * @return true nếu lượt xem này nên được ghi nhận
     */
    public boolean shouldCount(String postId, String viewer) {
        if (postId == null || viewer == null || viewer.isEmpty()) return false;
        long now = System.currentTimeMillis();
        if (seen.size() > maxEntries) sweep(now);
        Long last = seen.get(postId + "|" + viewer);
        if (last != null && now - last < windowMs) return false;
        seen.put(postId + "|" + viewer, now);
        return true;
    }

    private void sweep(long now) {
        for (Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator(); it.hasNext(); ) {
            if (now - it.next().getValue() >= windowMs) it.remove();
        }
        // Quét xong vẫn quá ngưỡng (rất nhiều lượt xem thật trong cùng cửa sổ):
        // xoá sạch còn hơn để bộ nhớ phình vô hạn. Cùng lắm là đếm trùng một ít.
        if (seen.size() > maxEntries) seen.clear();
    }
}
