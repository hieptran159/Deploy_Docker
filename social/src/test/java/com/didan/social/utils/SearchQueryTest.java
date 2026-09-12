package com.didan.social.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cửa quyết định giữa FULLTEXT và LIKE.
 *
 * Điểm mấu chốt là tiếng Việt: innodb_ft_min_token_size mặc định 3 ký tự, mà tiếng
 * Việt đầy âm tiết 2 chữ. Đoán sai chỗ này là lặng lẽ mất kết quả tìm kiếm.
 */
class SearchQueryTest {

    @Test
    void tachTokenTheoKhoangTrangVaDauCau() {
        assertEquals(List.of("truong", "nghe"), SearchQuery.tokens("truong nghe"));
        assertEquals(List.of("truong", "nghe"), SearchQuery.tokens("  truong,  nghe!  "));
        assertEquals(List.of(), SearchQuery.tokens("   "));
        assertEquals(List.of(), SearchQuery.tokens(null));
    }

    @Test
    void locKyTuToanTuCuaBooleanMode() {
        // Người dùng gõ +, -, * ... không được biến thành cú pháp truy vấn
        assertEquals(List.of("abc"), SearchQuery.tokens("+abc*"));
        assertEquals(List.of("abc"), SearchQuery.tokens("\"abc\""));
        assertEquals(List.of("ab", "cd"), SearchQuery.tokens("ab -cd"));
    }

    @Test
    void tokenDuDaiThiDungFulltext() {
        assertTrue(SearchQuery.canUseFulltext("BRICS", 3));
        assertTrue(SearchQuery.canUseFulltext("truong nghe", 3));
        assertTrue(SearchQuery.canUseFulltext("bún", 3), "3 ký tự là vừa đủ ngưỡng");
    }

    @Test
    void amTietNganCuaTiengVietPhaiRoiVeLike() {
        // Đây là lý do tồn tại của cả lớp này
        assertFalse(SearchQuery.canUseFulltext("bò", 3), "'bò' 2 ký tự -> không nằm trong chỉ mục");
        assertFalse(SearchQuery.canUseFulltext("ăn gì", 3));
        assertFalse(SearchQuery.canUseFulltext("bún bò", 3), "một token ngắn là đủ để phải rơi về LIKE");
        assertFalse(SearchQuery.canUseFulltext("a", 3));
    }

    @Test
    void truyVanRongThiKhongDungFulltext() {
        assertFalse(SearchQuery.canUseFulltext("", 3));
        assertFalse(SearchQuery.canUseFulltext("   ", 3));
        assertFalse(SearchQuery.canUseFulltext(null, 3));
        assertFalse(SearchQuery.canUseFulltext("!!!", 3), "chỉ toàn dấu câu -> không còn token nào");
    }

    @Test
    void bieuThucBooleanBatBuocMoiToken() {
        // +token* = GIAO, giống ngữ nghĩa "chứa tất cả các từ" của LIKE cũ.
        // NATURAL LANGUAGE MODE là HỢP, sẽ trả về cả bài chỉ khớp một từ.
        assertEquals("+truong* +nghe*", SearchQuery.booleanExpression("truong nghe"));
        assertEquals("+BRICS*", SearchQuery.booleanExpression("BRICS"));
        assertEquals("", SearchQuery.booleanExpression("   "));
    }
}
