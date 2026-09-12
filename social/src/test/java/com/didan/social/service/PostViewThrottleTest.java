package com.didan.social.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chống đếm trùng lượt xem.
 *
 * Không có nó thì F5 mười lần ra mười lượt — con số đó là "số lần tải trang",
 * không phải "lượt xem", tức là nói dối tác giả.
 */
class PostViewThrottleTest {

    PostViewThrottle throttle;

    @BeforeEach
    void setUp() {
        throttle = new PostViewThrottle();
        ReflectionTestUtils.setField(throttle, "windowMs", 1_800_000L);
        ReflectionTestUtils.setField(throttle, "maxEntries", 50_000);
    }

    @Test
    void lanDauTinh_taiLaiTrongCuaSoThiKhongTinh() {
        assertTrue(throttle.shouldCount("p-1", "u-1"));
        assertFalse(throttle.shouldCount("p-1", "u-1"), "F5 lại không được tính thêm lượt");
        assertFalse(throttle.shouldCount("p-1", "u-1"));
    }

    @Test
    void nguoiKhacNhauDemRieng() {
        assertTrue(throttle.shouldCount("p-1", "u-1"));
        assertTrue(throttle.shouldCount("p-1", "u-2"));
        assertTrue(throttle.shouldCount("p-1", "1.2.3.4"), "khách vãng lai phân biệt bằng IP");
    }

    @Test
    void baiKhacNhauDemRieng() {
        assertTrue(throttle.shouldCount("p-1", "u-1"));
        assertTrue(throttle.shouldCount("p-2", "u-1"));
    }

    @Test
    void hetCuaSoThiTinhLai() {
        ReflectionTestUtils.setField(throttle, "windowMs", 0L);
        assertTrue(throttle.shouldCount("p-1", "u-1"));
        assertTrue(throttle.shouldCount("p-1", "u-1"), "qua cửa sổ rồi thì lượt xem mới được tính");
    }

    @Test
    void khongCoNguoiXemThiKhongDem() {
        // Không phân biệt được người xem thì đếm bao nhiêu cũng vô nghĩa
        assertFalse(throttle.shouldCount("p-1", null));
        assertFalse(throttle.shouldCount("p-1", ""));
        assertFalse(throttle.shouldCount(null, "u-1"));
    }

    @Test
    void quaNguongThiDonBotChuKhongPhinhVoHan() {
        ReflectionTestUtils.setField(throttle, "maxEntries", 10);
        ReflectionTestUtils.setField(throttle, "windowMs", 0L);   // mọi khoá đều đã quá hạn
        for (int i = 0; i < 200; i++) throttle.shouldCount("p-" + i, "u-1");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Long> seen =
                (java.util.Map<String, Long>) ReflectionTestUtils.getField(throttle, "seen");
        assertTrue(seen.size() <= 20, "bộ nhớ phải được dọn, đang giữ " + seen.size() + " khoá");
    }
}
