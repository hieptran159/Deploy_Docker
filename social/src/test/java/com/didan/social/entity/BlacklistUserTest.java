package com.didan.social.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * `isActiveBan` là NGUỒN SỰ THẬT DUY NHẤT cho câu hỏi "người này còn bị cấm không".
 *
 * Trước khi có cấm tạm thời, sáu chỗ trong ứng dụng tự so `"blocked".equals(status)`
 * (đăng nhập, làm mới token, xác thực 2 bước, đổi hồ sơ, thống kê admin, ban/unban).
 * Rải thêm phép so ngày ra sáu nơi thì kiểu gì cũng có chỗ quên — và hậu quả là
 * lệnh cấm hết hạn ở màn này nhưng vẫn còn hiệu lực ở màn kia.
 */
class BlacklistUserTest {

    private BlacklistUser ban(String status, Date until) {
        BlacklistUser b = new BlacklistUser();
        b.setStatus(status);
        b.setBannedUntil(until);
        return b;
    }

    private Date daysFromNow(int d) {
        return new Date(System.currentTimeMillis() + d * 86_400_000L);
    }

    @Test
    void camVinhVienThiLuonConHieuLuc() {
        assertTrue(ban("blocked", null).isActiveBan());
    }

    @Test
    void camCoHanChuaToiNgayThiConHieuLuc() {
        assertTrue(ban("blocked", daysFromNow(7)).isActiveBan());
    }

    @Test
    void camCoHanQuaNgayThiHETHieuLuc() {
        // Không cần admin gỡ tay, cũng không cần job dọn định kỳ
        assertFalse(ban("blocked", daysFromNow(-1)).isActiveBan());
    }

    @Test
    void khongPhaiTrangThaiBlockedThiKhongTinhLaCam() {
        assertFalse(ban("pending", null).isActiveBan());
        assertFalse(ban("pending", daysFromNow(7)).isActiveBan(),
                "có hạn nhưng trạng thái đã gỡ -> không còn bị cấm");
        assertFalse(ban(null, null).isActiveBan());
    }

    @Test
    void hangMoiTinhKhoiTaoLaChuaBiCam() {
        assertFalse(new BlacklistUser().isActiveBan(), "mặc định status = 'pending'");
    }
}
