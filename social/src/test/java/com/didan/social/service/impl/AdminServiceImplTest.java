package com.didan.social.service.impl;

import com.didan.social.entity.BlacklistUser;
import com.didan.social.entity.Users;
import com.didan.social.repository.*;
import com.didan.social.service.AdminLogService;
import com.didan.social.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Cấm CÓ THỜI HẠN.
 *
 * Trước đây chặn người dùng chỉ có một nấc — vĩnh viễn, tới khi admin tự gỡ.
 * Hai chỗ dễ sai khi thêm thời hạn: cấm lại một người có lệnh cấm ĐÃ HẾT HẠN
 * (user_id là khoá chính, tạo hàng mới là vỡ), và guard "đã bị cấm rồi" phải hỏi
 * isActiveBan chứ không phải nhìn status.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceImplTest {

    @Mock AuthorizePathServiceImpl authorizePathService;
    @Mock UserRepository userRepository;
    @Mock BlacklistUserRepository blacklistUserRepository;
    @Mock BlacklistRepository blacklistTokenRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock BookmarkRepository bookmarkRepository;
    @Mock BlockRepository blockRepository;
    @Mock ReportRepository reportRepository;
    @Mock AdminLogRepository adminLogRepository;
    @Mock AdminLogService adminLogService;
    @Mock SessionService sessionService;

    AdminServiceImpl svc;
    static final String ADMIN = "admin-1";
    static final String TARGET = "u-2";

    @BeforeEach
    void setUp() throws Exception {
        svc = new AdminServiceImpl(authorizePathService, userRepository, blacklistUserRepository,
                blacklistTokenRepository, postRepository, commentRepository, conversationRepository,
                messageRepository, bookmarkRepository, blockRepository, reportRepository,
                adminLogRepository, adminLogService, sessionService);

        when(authorizePathService.getUserIdAuthoried()).thenReturn(ADMIN);
        Users admin = new Users();
        admin.setUserId(ADMIN);
        admin.setIsAdmin(1);
        when(userRepository.findFirstByUserId(ADMIN)).thenReturn(admin);

        Users target = new Users();
        target.setUserId(TARGET);
        target.setFullName("Người bị cấm");
        when(userRepository.findFirstByUserId(TARGET)).thenReturn(target);
    }

    private BlacklistUser saved() {
        ArgumentCaptor<BlacklistUser> cap = ArgumentCaptor.forClass(BlacklistUser.class);
        verify(blacklistUserRepository).save(cap.capture());
        return cap.getValue();
    }

    @Test
    void camVinhVienKhiKhongTruyenSoNgay() throws Exception {
        when(blacklistUserRepository.findByUserId(TARGET)).thenReturn(null);

        assertTrue(svc.blockUser(TARGET));

        BlacklistUser b = saved();
        assertEquals("blocked", b.getStatus());
        assertNull(b.getBannedUntil(), "không truyền ngày -> vĩnh viễn, giữ nguyên hành vi cũ");
        assertTrue(b.isActiveBan());
    }

    @Test
    void camCoHanDatDungMocHetHan() throws Exception {
        when(blacklistUserRepository.findByUserId(TARGET)).thenReturn(null);
        long truoc = System.currentTimeMillis();

        assertTrue(svc.blockUser(TARGET, 7));

        BlacklistUser b = saved();
        assertNotNull(b.getBannedUntil());
        long duKien = truoc + 7L * 86_400_000L;
        assertTrue(Math.abs(b.getBannedUntil().getTime() - duKien) < 5 * 60_000L,
                "mốc hết hạn phải là ~7 ngày nữa, đang là " + b.getBannedUntil());
        assertTrue(b.isActiveBan());
    }

    @Test
    void camLuonDaHETMoiThietBiCuaNguoiDo() throws Exception {
        when(blacklistUserRepository.findByUserId(TARGET)).thenReturn(null);
        svc.blockUser(TARGET, 3);
        verify(sessionService).revokeAllSessions(TARGET);
    }

    @Test
    void dangBiCamThiKhongCamLaiDuoc() {
        BlacklistUser dangCam = new BlacklistUser();
        dangCam.setUserId(TARGET);
        dangCam.setStatus("blocked");
        dangCam.setBannedUntil(new Date(System.currentTimeMillis() + 86_400_000L));
        when(blacklistUserRepository.findByUserId(TARGET)).thenReturn(dangCam);

        assertThrows(Exception.class, () -> svc.blockUser(TARGET, 7));
        verify(blacklistUserRepository, never()).save(any());
    }

    /**
     * user_id là khoá chính -> cấm lại người từng bị cấm phải DÙNG LẠI hàng cũ.
     * Tạo hàng mới sẽ vỡ khoá chính.
     */
    @Test
    void camLaiNguoiCoLenhCamDaHetHanThiDungLaiHangCu() throws Exception {
        BlacklistUser hetHan = new BlacklistUser();
        hetHan.setUserId(TARGET);
        hetHan.setStatus("blocked");
        hetHan.setBannedUntil(new Date(System.currentTimeMillis() - 86_400_000L));
        hetHan.setReportedQuantity(5);
        when(blacklistUserRepository.findByUserId(TARGET)).thenReturn(hetHan);

        assertTrue(svc.blockUser(TARGET, 30), "lệnh cấm cũ đã hết hạn -> phải cấm lại được");

        BlacklistUser b = saved();
        assertSame(hetHan, b, "phải ghi đè hàng cũ, không tạo hàng thứ hai");
        assertEquals(5, b.getReportedQuantity(), "số lần bị báo cáo không được reset khi cấm lại");
        assertTrue(b.isActiveBan());
    }

    @Test
    void goCamXoaLuonMocHetHan() throws Exception {
        BlacklistUser dangCam = new BlacklistUser();
        dangCam.setUserId(TARGET);
        dangCam.setStatus("blocked");
        dangCam.setBannedUntil(new Date(System.currentTimeMillis() + 86_400_000L));
        when(blacklistUserRepository.findByUserId(TARGET)).thenReturn(dangCam);

        assertTrue(svc.unblockUser(TARGET));

        BlacklistUser b = saved();
        assertEquals("pending", b.getStatus());
        assertNull(b.getBannedUntil(), "còn sót mốc cũ thì lần cấm sau sẽ mang theo hạn cũ");
        assertFalse(b.isActiveBan());
    }
}
