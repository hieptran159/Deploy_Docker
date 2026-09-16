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
import java.util.List;

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
    @Mock com.didan.social.repository.CategoryRepository categoryRepository;

    AdminServiceImpl svc;
    static final String ADMIN = "admin-1";
    static final String TARGET = "u-2";

    @BeforeEach
    void setUp() throws Exception {
        svc = new AdminServiceImpl(authorizePathService, userRepository, blacklistUserRepository,
                blacklistTokenRepository, postRepository, commentRepository, conversationRepository,
                messageRepository, bookmarkRepository, blockRepository, reportRepository,
                adminLogRepository, adminLogService, sessionService, categoryRepository);

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

    // ---- Chuyên mục: CRUD chỉ admin, slug tự sinh và không trùng ----

    @Test
    void nguoiThuongKhongTaoDuocChuyenMuc() {
        when(userRepository.findFirstByUserId(ADMIN)).thenReturn(null); // authAdmin() không tìm thấy -> coi như không phải admin
        assertThrows(Exception.class, () -> svc.createCategory("Thể thao", 0));
    }

    @Test
    void taoChuyenMucSinhSlugTuTenCoDauTiengViet() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());

        com.didan.social.dto.CategoryDTO out = svc.createCategory("Thể Thao", 3);

        assertEquals("Thể Thao", out.getName());
        assertEquals("the-thao", out.getSlug());
        ArgumentCaptor<com.didan.social.entity.Category> c =
                ArgumentCaptor.forClass(com.didan.social.entity.Category.class);
        verify(categoryRepository).save(c.capture());
        assertEquals(3, c.getValue().getPosition());
    }

    @Test
    void tenTrungThiSlugThemHauToSo() throws Exception {
        com.didan.social.entity.Category existing =
                new com.didan.social.entity.Category("c-old", "Thể thao", "the-thao", 0);
        when(categoryRepository.findAll()).thenReturn(List.of(existing));

        com.didan.social.dto.CategoryDTO out = svc.createCategory("Thể Thao", 0);

        assertEquals("the-thao-2", out.getSlug(), "trùng slug với chuyên mục khác -> thêm hậu tố, không văng lỗi SQL UNIQUE");
    }

    @Test
    void tenRongBiTuChoi() {
        assertThrows(Exception.class, () -> svc.createCategory("   ", 0));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void suaChuyenMucKhongDoiTenThiGiuNguyenSlugCu() throws Exception {
        com.didan.social.entity.Category c =
                new com.didan.social.entity.Category("c-1", "Thảo luận chung", "thao-luan-chung", 0);
        when(categoryRepository.findById("c-1")).thenReturn(java.util.Optional.of(c));

        svc.updateCategory("c-1", null, 9);

        assertEquals("thao-luan-chung", c.getSlug(), "chỉ đổi vị trí -> URL cũ không được đổi theo");
        assertEquals(9, c.getPosition());
    }

    @Test
    void xoaChuyenMucKhongTonTaiThiBaoLoi() {
        when(categoryRepository.existsById("khong-co")).thenReturn(false);
        assertThrows(Exception.class, () -> svc.deleteCategory("khong-co"));
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void xoaChuyenMucHopLe() throws Exception {
        when(categoryRepository.existsById("c-1")).thenReturn(true);
        assertTrue(svc.deleteCategory("c-1"));
        verify(categoryRepository).deleteById("c-1");
    }
}
