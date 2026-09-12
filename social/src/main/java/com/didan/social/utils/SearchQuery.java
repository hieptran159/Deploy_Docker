package com.didan.social.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Quyết định một truy vấn tìm kiếm có dùng được FULLTEXT của MySQL hay không, và
 * dựng biểu thức BOOLEAN MODE tương ứng.
 *
 * Vì sao phải có lớp này: innodb_ft_min_token_size mặc định là 3 KÝ TỰ. Tiếng Việt
 * đầy âm tiết 2 chữ — "bò", "ăn", "gì", "hà", "cá" — và chúng KHÔNG nằm trong chỉ
 * mục. Chuyển thẳng sang FULLTEXT là lặng lẽ mất kết quả cho đúng những từ đó.
 *
 * Nên: mọi token đủ dài -> FULLTEXT (nhanh + xếp theo mức khớp); có token ngắn ->
 * rơi về LIKE như cũ (chậm hơn nhưng không mất kết quả nào).
 */
public final class SearchQuery {

    private SearchQuery() {}

    /** Ký tự có nghĩa đặc biệt trong BOOLEAN MODE — phải loại, kẻo người dùng gõ nhầm thành cú pháp. */
    private static final String OPERATORS = "+-><()~*\"@";

    /** Tách theo khoảng trắng và dấu câu, bỏ ký tự toán tử của BOOLEAN MODE. */
    public static List<String> tokens(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String piece : raw.trim().split("[\\s\\p{Punct}]+")) {
            StringBuilder sb = new StringBuilder();
            for (char c : piece.toCharArray()) {
                if (OPERATORS.indexOf(c) < 0) sb.append(c);
            }
            String t = sb.toString().trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /**
     * @param minTokenSize innodb_ft_min_token_size của server (mặc định 3)
     * @return true nếu FULLTEXT sẽ tìm được đủ, false thì phải dùng LIKE
     */
    public static boolean canUseFulltext(String raw, int minTokenSize) {
        List<String> ts = tokens(raw);
        if (ts.isEmpty()) return false;
        for (String t : ts) {
            if (t.length() < minTokenSize) return false;
        }
        return true;
    }

    /**
     * Biểu thức BOOLEAN MODE: mọi token đều BẮT BUỘC (+token*), tức là giao — cùng
     * ngữ nghĩa "chứa tất cả các từ" như LIKE trước đây, không phải hợp như
     * NATURAL LANGUAGE MODE (vốn sẽ trả về cả bài chỉ khớp một từ).
     */
    public static String booleanExpression(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String t : tokens(raw)) {
            if (sb.length() > 0) sb.append(' ');
            sb.append('+').append(t).append('*');
        }
        return sb.toString();
    }
}
