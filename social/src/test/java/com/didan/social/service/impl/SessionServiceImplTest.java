package com.didan.social.service.impl;

import com.didan.social.dto.SessionDTO;
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
import java.util.List;
import java.util.Optional;

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

    /* ---------- Màn hình "Thiết bị đang đăng nhập" ---------- */

    @Test
    void danhSachThietBiDanhDauDungMayDangGoi() {
        UserSessions here = session("s-1", "AT-here", "h1");
        here.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/128.0 Safari/537.36");
        here.setIp("1.2.3.4");
        UserSessions other = session("s-2", "AT-other", "h2");
        when(userSessionRepository.findByUserIdOrderByLastUsedAtDesc(ME))
                .thenReturn(Arrays.asList(here, other));

        List<SessionDTO> list = svc.listSessions(ME, "AT-here");

        assertEquals(2, list.size());
        assertTrue(list.get(0).isCurrent(), "phiên của chính thiết bị đang gọi phải được đánh dấu");
        assertFalse(list.get(1).isCurrent());
        assertEquals("Chrome trên Windows", list.get(0).getDevice());
        assertEquals("1.2.3.4", list.get(0).getIp());
        // Phiên cũ (trước khi có cột user_agent) vẫn hiển thị được, không nổ NPE
        assertEquals("Thiết bị không rõ", list.get(1).getDevice());
    }

    @Test
    void danhSachThietBiKhongLoToken() throws Exception {
        UserSessions s1 = session("s-1", "AT-secret", "hash-secret");
        when(userSessionRepository.findByUserIdOrderByLastUsedAtDesc(ME)).thenReturn(List.of(s1));

        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(svc.listSessions(ME, "AT-secret"));

        assertFalse(json.contains("AT-secret"), "DTO không được mang access token ra ngoài");
        assertFalse(json.contains("hash-secret"), "DTO không được mang refresh hash ra ngoài");
    }

    @Test
    void dangXuatMotThietBiChiXoaDungPhienDo() throws Exception {
        UserSessions target = session("s-2", "AT-other", "h2");
        when(userSessionRepository.findById("s-2")).thenReturn(Optional.of(target));

        svc.closeSessionById(ME, "s-2");

        verify(userSessionRepository).delete(target);
        ArgumentCaptor<BlacklistToken> cap = ArgumentCaptor.forClass(BlacklistToken.class);
        verify(blacklistRepository).save(cap.capture());
        assertEquals("AT-other", cap.getValue().getToken(), "phải chặn token của đúng phiên bị xoá");
    }

    @Test
    void khongDaDuocPhienCuaNguoiKhac() {
        UserSessions cuaNguoiKhac = new UserSessions("s-9", "u-999", "h9", "AT-9", true, new Date());
        when(userSessionRepository.findById("s-9")).thenReturn(Optional.of(cuaNguoiKhac));

        assertThrows(Exception.class, () -> svc.closeSessionById(ME, "s-9"));
        verify(userSessionRepository, never()).delete(any());
        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void phienKhongTonTaiBaoLoiGiongPhienCuaNguoiKhac() {
        when(userSessionRepository.findById("s-x")).thenReturn(Optional.empty());
        UserSessions cuaNguoiKhac = new UserSessions("s-9", "u-999", "h9", "AT-9", true, new Date());
        when(userSessionRepository.findById("s-9")).thenReturn(Optional.of(cuaNguoiKhac));

        // Hai thông báo khác nhau sẽ để lộ sessionId nào có thật
        String a = assertThrows(Exception.class, () -> svc.closeSessionById(ME, "s-x")).getMessage();
        String b = assertThrows(Exception.class, () -> svc.closeSessionById(ME, "s-9")).getMessage();
        assertEquals(a, b);
    }

    /* ---------- Đăng xuất mọi thiết bị KHÁC ---------- */

    @Test
    void dangXuatThietBiKhacGiuLaiDungMayDangGoi() throws Exception {
        UserSessions here = session("s-1", "AT-here", "h1");
        UserSessions a = session("s-2", "AT-a", "h2");
        UserSessions b = session("s-3", "AT-b", "h3");
        when(userSessionRepository.findByUserIdOrderByLastUsedAtDesc(ME))
                .thenReturn(Arrays.asList(here, a, b));

        assertEquals(2, svc.closeOtherSessions(ME, "AT-here"), "phải trả về số máy đã đá");

        verify(userSessionRepository).delete(a);
        verify(userSessionRepository).delete(b);
        verify(userSessionRepository, never()).delete(here);
    }

    @Test
    void dangXuatThietBiKhacKhongChanTokenCuaMayDangGoi() throws Exception {
        UserSessions here = session("s-1", "AT-here", "h1");
        UserSessions other = session("s-2", "AT-other", "h2");
        when(userSessionRepository.findByUserIdOrderByLastUsedAtDesc(ME))
                .thenReturn(Arrays.asList(here, other));

        svc.closeOtherSessions(ME, "AT-here");

        ArgumentCaptor<BlacklistToken> cap = ArgumentCaptor.forClass(BlacklistToken.class);
        verify(blacklistRepository, atLeastOnce()).save(cap.capture());
        java.util.List<String> daChan = cap.getAllValues().stream()
                .map(BlacklistToken::getToken).toList();
        assertTrue(daChan.contains("AT-other"));
        assertFalse(daChan.contains("AT-here"),
                "chặn token của chính mình là tự đá mình ra ngay sau khi bấm nút");
    }

    /**
     * Không xác định được thiết bị hiện tại thì thà báo lỗi, KHÔNG đoán — đoán sai
     * là đá luôn cả người đang bấm nút.
     */
    @Test
    void khongBietMayHienTaiThiTuChoiChuKhongDaSach() {
        UserSessions a = session("s-2", "AT-a", "h2");
        when(userSessionRepository.findByUserIdOrderByLastUsedAtDesc(ME))
                .thenReturn(java.util.List.of(a));

        assertThrows(Exception.class, () -> svc.closeOtherSessions(ME, null));
        assertThrows(Exception.class, () -> svc.closeOtherSessions(ME, ""));
        verify(userSessionRepository, never()).delete(any());
        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void chiCoMotThietBiThiKhongDaGiCa() throws Exception {
        UserSessions here = session("s-1", "AT-here", "h1");
        when(userSessionRepository.findByUserIdOrderByLastUsedAtDesc(ME))
                .thenReturn(java.util.List.of(here));

        assertEquals(0, svc.closeOtherSessions(ME, "AT-here"));
        verify(userSessionRepository, never()).delete(any());
    }
}
