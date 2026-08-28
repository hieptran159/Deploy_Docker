package com.didan.social.utils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trích hashtag từ tiêu đề / nội dung bài viết.
 * Quy tắc: '#' + chữ/số/gạch dưới (hỗ trợ Unicode: #chủ_đề, #hà_nội), độ dài 1..50,
 * chuẩn hóa về chữ thường, không lặp. Tối đa 20 tag / bài.
 */
public final class HashtagUtils {
    private HashtagUtils() {}

    private static final Pattern TAG = Pattern.compile("#([\\p{L}\\p{N}_]{1,50})");
    private static final int MAX_TAGS = 20;

    public static Set<String> extract(String... texts) {
        Set<String> out = new LinkedHashSet<>();
        if (texts == null) return out;
        for (String text : texts) {
            if (text == null || text.isEmpty()) continue;
            Matcher m = TAG.matcher(text);
            while (m.find()) {
                String tag = m.group(1).toLowerCase();
                // bỏ tag toàn chữ số (vd "#123") -> ít ý nghĩa phân loại
                if (tag.chars().allMatch(Character::isDigit)) continue;
                out.add(tag);
                if (out.size() >= MAX_TAGS) return out;
            }
        }
        return out;
    }

    /** Chuẩn hóa 1 tag do client gửi lên (có thể kèm '#'). Trả null nếu không hợp lệ. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.startsWith("#")) t = t.substring(1);
        t = t.toLowerCase();
        if (t.isEmpty() || t.length() > 50) return null;
        if (!t.matches("[\\p{L}\\p{N}_]+")) return null;
        if (t.chars().allMatch(Character::isDigit)) return null;
        return t;
    }
}
