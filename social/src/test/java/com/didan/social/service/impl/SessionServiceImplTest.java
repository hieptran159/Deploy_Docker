package com.didan.social.service.impl;

import com.didan.social.entity.BlacklistToken;
import com.didan.social.entity.UserSessions;
import com.didan.social.repository.BlacklistRepository;
import com.didan.social.repository.UserSessionRepository;
import com.didan.social.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Đăng nhập nhiều thiết bị.
 *
 * Trước đây access token và refresh token cùng nằm trong MỘT ô trên hàng users,
 * nên đăng nhập ở máy thứ hai ghi đè lên phiên máy thứ nhất và đá nó ra, còn
 * đăng xuất ở máy này thì chặn nhầm token của máy kia.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceImplTest {

    @Mock UserSessionRepository userSessionRepository;
    @Mock BlacklistRepository blacklistRepository;
    @Mock JwtUtils jwtUtils;

    SessionServiceImpl svc;
    static final String ME = "u-1";

    @BeforeEach
    void setUp() {
        svc = new SessionServiceImpl(userSessionRepository, blacklistRepository, jwtUtils);
        when(jwtUtils.getRefreshExpirationMs()).thenReturn(2592000000L);
    }

    private UserSessions session(String id, String access, String refreshHash) {
        return new UserSessions(id, ME, refreshHash, access, true, new Date());
    }

    @Test
    void moPhienMoiKhongDungToiPhienDangCo() {
        when(jwtUtils.generateAccessToken(ME, true)).thenReturn("AT2");
        when(jwtUtils.generateRefreshToken(ME, true)).thenReturn("RT2");

        String[] pair = svc.openSession(ME, true);

        assertArrayEquals(new String[]{"AT2", "RT2"}, pair);
        // Điểm mấu chốt: KHÔNG chặn token nào, KHÔNG xoá phiên nào
        verify(blacklistRepository, never()).save(any());
        verify(userSessionRepository, never()).deleteByUserId(anyString());

        ArgumentCaptor<UserSessions> cap = ArgumentCaptor.forClass(UserSessions.class);
        verify(userSessionRepository).save(cap.capture());
        assertEquals(ME, cap.getValue().getUserId());
        // DB chỉ giữ hash, không giữ refresh token gốc
        assertEquals(JwtUtils.sha256Hex("RT2"), cap.getValue().getRefreshHash());
        assertNotEquals("RT2", cap.getValue().getRefreshHash());
        assertEquals("AT2", cap.getValue().getAccessToken());
    }

    @Test
    void dangXuatChiThuHoiPhienCuaThietBiDangGoi() {
        UserSessions s1 = session("s-1", "AT1", "h1");
        when(userSessionRepository.findFirstByAccessToken("AT1")).thenReturn(s1);

        svc.closeSession("AT1");

        verify(blacklistRepository).save(argThat(t -> "AT1".equals(((BlacklistToken) t).getToken())));
        verify(userSessionRepository).delete(s1);
        // không được đụng phiên của thiết bị khác
        verify(userSessionRepository, never()).deleteByUserId(anyString());
    }

    @Test
    void dangXuatVoiTokenLaKhongXoaPhienNao() {
        when(userSessionRepository.findFirstByAccessToken("AT-la")).thenReturn(null);
        svc.closeSession("AT-la");
        verify(userSessionRepository, never()).delete(any(UserSessions.class));
    }

    @Test
    void dangXuatBoQuaKhiThieuToken() {
        svc.closeSession(null);
        svc.closeSession("");
        verify(blacklistRepository, never()).save(any());
        verify(userSessionRepository, never()).delete(any(UserSessions.class));
    }

    @Test
    void thuHoiToanBoChanTokenCuaMoiThietBiRoiXoaHet() {
        when(userSessionRepository.findByUserIdOrderByLastUsedAtDesc(ME))
                .thenReturn(Arrays.asList(session("s-1", "AT1", "h1"), session("s-2", "AT2", "h2")));

        svc.revokeAllSessions(ME);

        ArgumentCaptor<BlacklistToken> cap = ArgumentCaptor.forClass(BlacklistToken.class);
        verify(blacklistRepository, times(2)).save(cap.capture());
        assertEquals(Arrays.asList("AT1", "AT2"),
                cap.getAllValues().stream().map(BlacklistToken::getToken).toList());
        verify(userSessionRepository).deleteByUserId(ME);
    }

    @Test
    void donPhienQuaHanHongKhongChanDangNhap() {
        when(jwtUtils.generateAccessToken(ME, true)).thenReturn("AT");
        when(jwtUtils.generateRefreshToken(ME, true)).thenReturn("RT");
        doThrow(new RuntimeException("db lag"))
                .when(userSessionRepository).deleteByUserIdAndLastUsedAtBefore(anyString(), any(Date.class));

        assertDoesNotThrow(() -> svc.openSession(ME, true));
    }
}
