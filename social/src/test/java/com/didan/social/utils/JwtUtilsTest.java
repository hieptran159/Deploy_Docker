package com.didan.social.utils;

import com.didan.social.entity.BlacklistToken;
import com.didan.social.repository.BlacklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtUtilsTest {

    @Mock BlacklistRepository blacklistRepository;
    JwtUtils jwt;

    @BeforeEach
    void setUp() {
        jwt = new JwtUtils(blacklistRepository);
        ReflectionTestUtils.setField(jwt, "secretKey", "wyD7j04/rzm+8EABKgQTPO9cFojZI2Q2xRPycfhHtZc=");
        ReflectionTestUtils.setField(jwt, "accessExpirationMs", 86400000L);
        ReflectionTestUtils.setField(jwt, "refreshExpirationMs", 2592000000L);
        when(blacklistRepository.findFirstByToken(anyString())).thenReturn(null);
    }

    @Test
    void refreshTokenRoundTrips() throws Exception {
        String rt = jwt.generateRefreshToken("user-1");
        jwt.validateRefreshToken(rt); // không ném
        assertEquals("user-1", jwt.getUserIdFromAccessToken(rt));
    }

    @Test
    void accessTokenRejectedByRefreshValidation() {
        String at = jwt.generateAccessToken("user-1");
        Exception e = assertThrows(Exception.class, () -> jwt.validateRefreshToken(at));
        assertTrue(e.getMessage().contains("Không phải refresh token"));
    }

    @Test
    void refreshTokenRejectedAsAccessToken() {
        String rt = jwt.generateRefreshToken("user-1");
        Exception e = assertThrows(Exception.class, () -> jwt.validateAccessToken(rt));
        assertTrue(e.getMessage().contains("Invalid access token"));
    }

    @Test
    void plainAccessTokenStillValidates() throws Exception {
        jwt.validateAccessToken(jwt.generateAccessToken("user-1"));
    }

    @Test
    void blacklistedRefreshTokenRejected() {
        String rt = jwt.generateRefreshToken("user-1");
        when(blacklistRepository.findFirstByToken(rt)).thenReturn(new BlacklistToken(rt));
        Exception e = assertThrows(Exception.class, () -> jwt.validateRefreshToken(rt));
        assertTrue(e.getMessage().contains("thu hồi"));
    }

    /**
     * JWT chỉ ghi iat/exp theo GIÂY. Không có claim ngẫu nhiên thì hai token cấp
     * trong cùng một giây cho cùng người dùng sẽ GIỐNG HỆT nhau — đã gặp thật:
     * hai thiết bị dùng chung refresh token, và xoay vòng thì đụng khoá duy nhất
     * uk_user_sessions_refresh làm /auth/refresh hỏng.
     */
    @Test
    void tokenCapLienTiepPhaiKhacNhau() {
        for (int i = 0; i < 50; i++) {
            assertNotEquals(jwt.generateRefreshToken("u-1", true), jwt.generateRefreshToken("u-1", true),
                    "refresh token cấp liên tiếp bị trùng");
            assertNotEquals(jwt.generateAccessToken("u-1", true), jwt.generateAccessToken("u-1", true),
                    "access token cấp liên tiếp bị trùng");
        }
    }

    /** Token phải nằm gọn trong cột blacklist_token.token (varchar 512). */
    @Test
    void tokenKhongVuotQuaChoLuuBlacklist() {
        assertTrue(jwt.generateRefreshToken("0123456789abcdef0123456789abcdef0123", true).length() < 512);
        assertTrue(jwt.generateAccessToken("0123456789abcdef0123456789abcdef0123", true).length() < 512);
    }

    /** Thêm jti không được làm hỏng việc đọc lại claim.
     *  Dùng remember=true vì setUp không nạp shortRefreshExpirationMs (mặc định 0
     *  -> token phiên tạm hết hạn ngay lập tức). */
    @Test
    void themJtiKhongPhaVoCacClaimCu() throws Exception {
        String rt = jwt.generateRefreshToken("u-9", true);
        jwt.validateRefreshToken(rt);
        assertEquals("u-9", jwt.getUserIdFromAccessToken(rt));
        assertTrue(jwt.isRememberRefreshToken(rt));
    }
}
