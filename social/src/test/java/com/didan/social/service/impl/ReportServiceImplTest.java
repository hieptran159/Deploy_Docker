package com.didan.social.service.impl;

import com.didan.social.entity.Posts;
import com.didan.social.repository.CommentRepository;
import com.didan.social.repository.PostRepository;
import com.didan.social.repository.ReportRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AdminLogService;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.CommentService;
import com.didan.social.service.PostService;
import com.didan.social.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {

    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock AuthorizePathService authorizePathService;
    @Mock UserService userService;
    @Mock PostService postService;
    @Mock CommentService commentService;
    @Mock AdminLogService adminLogService;

    ReportServiceImpl svc;
    static final String ME = "reporter-1";
    static final String PID = "post-9";

    @BeforeEach
    void setUp() throws Exception {
        svc = new ReportServiceImpl(reportRepository, userRepository, postRepository, commentRepository,
                authorizePathService, userService, postService, commentService, adminLogService);
        ReflectionTestUtils.setField(svc, "autoHideThreshold", 3);
        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
    }

    private Posts publishedPost() {
        Posts p = new Posts();
        p.setPostId(PID);
        p.setStatus("published");
        return p;
    }

    @Test
    void invalidTypeRejected() {
        Exception e = assertThrows(Exception.class, () -> svc.create("SOMETHING", PID, "x"));
        assertTrue(e.getMessage().contains("không hợp lệ"));
    }

    @Test
    void reportPostBelowThresholdDoesNotHide() throws Exception {
        Posts p = publishedPost();
        when(postRepository.findFirstByPostId(PID)).thenReturn(p);
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(null);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus("POST", PID, "OPEN")).thenReturn(2L);

        assertTrue(svc.create("POST", PID, "spam"));

        verify(reportRepository).save(any());
        assertEquals("published", p.getStatus());
        verify(postRepository, never()).save(p);
    }

    @Test
    void reportPostAtThresholdAutoHides() throws Exception {
        Posts p = publishedPost();
        when(postRepository.findFirstByPostId(PID)).thenReturn(p);
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(null);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus("POST", PID, "OPEN")).thenReturn(3L);

        assertTrue(svc.create("POST", PID, "spam"));

        assertEquals("hidden", p.getStatus());
        verify(postRepository).save(p);
    }

    @Test
    void alreadyHiddenPostNotTouchedAgain() throws Exception {
        Posts p = publishedPost();
        p.setStatus("hidden");
        when(postRepository.findFirstByPostId(PID)).thenReturn(p);
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(null);

        assertTrue(svc.create("POST", PID, "spam"));

        verify(reportRepository, times(1)).save(any());
        verify(postRepository, never()).save(any());
    }

    @Test
    void duplicateOpenReportIsNoop() throws Exception {
        when(postRepository.findFirstByPostId(PID)).thenReturn(publishedPost());
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(new com.didan.social.entity.Reports());

        assertTrue(svc.create("POST", PID, "spam"));

        verify(reportRepository, never()).save(any());
    }

    @Test
    void thresholdZeroDisablesAutoHide() throws Exception {
        ReflectionTestUtils.setField(svc, "autoHideThreshold", 0);
        Posts p = publishedPost();
        when(postRepository.findFirstByPostId(PID)).thenReturn(p);
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(null);

        assertTrue(svc.create("POST", PID, "spam"));

        assertEquals("published", p.getStatus());
        verify(reportRepository, never()).countByTargetTypeAndTargetIdAndStatus(any(), any(), any());
    }

    /* ---------- Hàng đợi báo cáo: nạp theo lô ---------- */

    private com.didan.social.entity.Reports rp(String id, String type, String targetId) {
        com.didan.social.entity.Reports r = new com.didan.social.entity.Reports();
        r.setReportId(id);
        r.setReporterId("u-" + id);
        r.setTargetType(type);
        r.setTargetId(targetId);
        r.setStatus("OPEN");
        return r;
    }

    private com.didan.social.entity.Users usr(String id) {
        com.didan.social.entity.Users u = new com.didan.social.entity.Users();
        u.setUserId(id); u.setFullName("Ten " + id); u.setEmail(id + "@x.vn");
        return u;
    }

    /**
     * Chốt chặn hiệu năng: trước đây mỗi dòng tốn 3-4 truy vấn (người báo cáo, xem
     * trước, đếm cùng đích, bài viết) -> 300 dòng thành ~1.200 truy vấn. Test này đỏ
     * ngay nếu ai đó đưa truy vấn từng dòng quay lại.
     */
    @Test
    void hangDoiBaoCaoNapTheoLoChuKhongTruyVanTungDong() throws Exception {
        com.didan.social.entity.Users admin = usr("admin");
        admin.setIsAdmin(1);
        when(authorizePathService.getUserIdAuthoried()).thenReturn("admin");
        when(userRepository.findFirstByUserId("admin")).thenReturn(admin);

        when(reportRepository.findTop300ByStatusOrderByCreatedAtDesc("OPEN")).thenReturn(
                java.util.List.of(rp("1", "POST", "p-1"), rp("2", "POST", "p-1"),
                                  rp("3", "USER", "u-x"), rp("4", "COMMENT", "c-1")));
        when(userRepository.findByUserIdIn(anyCollection()))
                .thenReturn(java.util.List.of(usr("u-1"), usr("u-2"), usr("u-3"), usr("u-4"), usr("u-x")));
        Posts p = new Posts(); p.setPostId("p-1"); p.setTitle("Tieu de bai"); p.setStatus("published");
        when(postRepository.findByPostIdIn(anyCollection())).thenReturn(java.util.List.of(p));
        com.didan.social.entity.Comments c = new com.didan.social.entity.Comments();
        c.setCommentId("c-1"); c.setContent("noi dung binh luan");
        when(commentRepository.findByCommentIdIn(anyCollection())).thenReturn(java.util.List.of(c));
        when(reportRepository.countOpenByTargets(anyCollection()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"POST", "p-1", 2L}));

        java.util.List<com.didan.social.dto.ReportDTO> out = svc.listForAdmin("OPEN");

        assertEquals(4, out.size());
        // Mỗi loại nạp ĐÚNG MỘT lần cho cả trang
        verify(userRepository, times(1)).findByUserIdIn(anyCollection());
        verify(postRepository, times(1)).findByPostIdIn(anyCollection());
        verify(commentRepository, times(1)).findByCommentIdIn(anyCollection());
        verify(reportRepository, times(1)).countOpenByTargets(anyCollection());
        // Và KHÔNG được gọi bản từng dòng nữa
        verify(postRepository, never()).findFirstByPostId(anyString());
        verify(commentRepository, never()).findByCommentId(anyString());
        verify(reportRepository, never()).countByTargetTypeAndTargetIdAndStatus(anyString(), anyString(), anyString());
    }

    @Test
    void hangDoiBaoCaoDungDuLieuDaNap() throws Exception {
        com.didan.social.entity.Users admin = usr("admin");
        admin.setIsAdmin(1);
        when(authorizePathService.getUserIdAuthoried()).thenReturn("admin");
        when(userRepository.findFirstByUserId("admin")).thenReturn(admin);
        when(reportRepository.findTop300ByStatusOrderByCreatedAtDesc("OPEN"))
                .thenReturn(java.util.List.of(rp("1", "POST", "p-1")));
        when(userRepository.findByUserIdIn(anyCollection())).thenReturn(java.util.List.of(usr("u-1")));
        Posts p = new Posts(); p.setPostId("p-1"); p.setTitle("Tieu de bai"); p.setStatus("hidden");
        when(postRepository.findByPostIdIn(anyCollection())).thenReturn(java.util.List.of(p));
        when(reportRepository.countOpenByTargets(anyCollection()))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{"POST", "p-1", 5L}));

        com.didan.social.dto.ReportDTO d = svc.listForAdmin("OPEN").get(0);

        assertEquals("Ten u-1", d.getReporterName());
        assertEquals("Tieu de bai", d.getTargetPreview());
        assertEquals("hidden", d.getTargetStatus());
        assertEquals(5L, d.getSameTargetOpenCount());
    }

    @Test
    void baiDaXoaThiXemTruocNoiro() throws Exception {
        com.didan.social.entity.Users admin = usr("admin");
        admin.setIsAdmin(1);
        when(authorizePathService.getUserIdAuthoried()).thenReturn("admin");
        when(userRepository.findFirstByUserId("admin")).thenReturn(admin);
        when(reportRepository.findTop300ByStatusOrderByCreatedAtDesc("OPEN"))
                .thenReturn(java.util.List.of(rp("1", "POST", "da-xoa")));
        when(userRepository.findByUserIdIn(anyCollection())).thenReturn(java.util.List.of(usr("u-1")));
        when(postRepository.findByPostIdIn(anyCollection())).thenReturn(java.util.List.of());
        when(reportRepository.countOpenByTargets(anyCollection())).thenReturn(java.util.List.of());

        com.didan.social.dto.ReportDTO d = svc.listForAdmin("OPEN").get(0);

        assertEquals("(bài đã xoá)", d.getTargetPreview());
        assertEquals("deleted", d.getTargetStatus());
        assertEquals(0L, d.getSameTargetOpenCount(), "không có trong map đếm -> 0");
    }
}
