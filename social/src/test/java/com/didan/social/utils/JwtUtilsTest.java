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
}
