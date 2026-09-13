package com.didan.social.service.impl;

import com.didan.social.entity.Comments;
import com.didan.social.entity.UserComment;
import com.didan.social.entity.Users;
import com.didan.social.payload.request.EditCommentRequest;
import com.didan.social.repository.*;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.FileUploadsService;
import com.didan.social.service.NotificationService;
import com.didan.social.socket.RealtimeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Ảnh bình luận: sửa ảnh PHẢI sinh tên file mới.
 *
 * Trước đây `updateComment` ghi lại đúng `comment/<commentId>` — cùng một URL, nội dung
 * khác. Người đã tải ảnh cũ còn thấy ảnh cũ tới khi hết hạn cache, và khi `/images/**`
 * được khai `immutable` thì "tới khi hết hạn" thành một năm. Avatar / ảnh bìa / ảnh nhóm
 * chat đã mang timestamp từ trước đúng vì lý do này; chỗ này là chỗ duy nhất còn sót.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceImplTest {

    @Mock UserCommentRepository userCommentRepository;
    @Mock CommentRepository commentRepository;
    @Mock CommentLikeRepository commentLikeRepository;
    @Mock UserRepository userRepository;
    @Mock FileUploadsService fileUploadsService;
    @Mock AuthorizePathService authorizePathService;
    @Mock PostRepository postRepository;
    @Mock UserPostRepository userPostRepository;
    @Mock NotificationService notificationService;
    @Mock RealtimeGateway realtimeGateway;

    CommentServiceImpl svc;

    static final String ME = "u-1";
    static final String CID = "c-123";

    Comments comment;

    @BeforeEach
    void setUp() throws Exception {
        svc = new CommentServiceImpl(userCommentRepository, commentRepository, commentLikeRepository,
                userRepository, fileUploadsService, authorizePathService, postRepository,
                userPostRepository, notificationService, realtimeGateway);

        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
        Users me = new Users();
        me.setUserId(ME);
        me.setFullName("Tôi");
        when(userRepository.findFirstByUserId(ME)).thenReturn(me);

        comment = new Comments();
        comment.setCommentId(CID);
        comment.setContent("nội dung cũ");
        comment.setCommentAt(Timestamp.valueOf("2026-09-01 10:00:00"));
        comment.setCommentImg("comment/" + CID + ".png");

        UserComment uc = new UserComment();
        uc.setComments(comment);
        uc.setUsers(me);
        when(userCommentRepository.findFirstByUsers_UserIdAndComments_CommentId(ME, CID)).thenReturn(uc);
        when(fileUploadsService.storeFile(any(), anyString(), anyString())).thenReturn("ten-file-moi.jpg");
    }

    private EditCommentRequest suaKemAnh() {
        EditCommentRequest req = new EditCommentRequest();
        req.setContent("nội dung mới");
        req.setCommentImg(new MockMultipartFile("commentImg", "anh.png", "image/png", new byte[]{1, 2, 3}));
        return req;
    }

    /** Id truyền cho storeFile quyết định tên file -> phải khác lần trước. */
    private String idTruyenChoStoreFile() throws Exception {
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(fileUploadsService).storeFile(any(), eq("comment"), cap.capture());
        return cap.getValue();
    }

    @Test
    void suaAnhBinhLuanThiSinhTenFileMOI() throws Exception {
        svc.updateComment(CID, suaKemAnh());

        String id = idTruyenChoStoreFile();
        assertNotEquals(CID, id, "ghi lại đúng commentId = cùng một URL, nội dung khác");
        assertTrue(id.startsWith(CID + "-"), "vẫn phải bắt đầu bằng commentId để truy ngược được, đang là " + id);
        String phanThem = id.substring(CID.length() + 1);
        assertTrue(phanThem.matches("\\d{10,}"), "phần thêm phải là dấu thời gian, đang là " + phanThem);
    }

    @Test
    void anhCuBiXoaTruocKhiGhiAnhMoi() throws Exception {
        svc.updateComment(CID, suaKemAnh());

        // Không xoá thì mỗi lần sửa ảnh lại bỏ lại một file mồ côi trong volume
        verify(fileUploadsService).deleteFile("comment/" + CID + ".png");
    }

    @Test
    void suaMoiNoiDungThiKhongChamVaoAnh() throws Exception {
        EditCommentRequest req = new EditCommentRequest();
        req.setContent("chỉ đổi chữ");

        svc.updateComment(CID, req);

        verify(fileUploadsService, never()).storeFile(any(), anyString(), anyString());
        verify(fileUploadsService, never()).deleteFile(anyString());
        assertEquals("comment/" + CID + ".png", comment.getCommentImg(), "ảnh cũ phải còn nguyên");
    }

    @Test
    void nguoiKhongSoHuuThiKhongSuaDuoc() {
        when(userCommentRepository.findFirstByUsers_UserIdAndComments_CommentId(ME, CID)).thenReturn(null);

        assertThrows(Exception.class, () -> svc.updateComment(CID, suaKemAnh()));
        verifyNoInteractions(fileUploadsService);
    }
}
