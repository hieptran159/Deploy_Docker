package com.didan.social.service.impl;

import com.didan.social.dto.UserDTO;
import com.didan.social.dto.UserLiteDTO;
import com.didan.social.entity.Users;
import com.didan.social.repository.*;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.FollowService;
import com.didan.social.service.SessionService;
import com.didan.social.socket.RealtimeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Danh sách người dùng: vừa là chuyện HIỆU NĂNG vừa là chuyện RIÊNG TƯ.
 *
 * Đo trên production: /user/getAllUser trả 43 người trong 1,56s và 190 KB, vì
 * convertToDTO chạm bốn quan hệ lazy mỗi người (~172 truy vấn). Đồng thời nó KHÔNG
 * gọi hidePrivate như getUserById, nên số điện thoại của 2 người đã tắt công khai
 * vẫn lọt ra, và ngày sinh lọt của cả 42 người.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock AuthorizePathService authorizePathService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock FileUploadsServiceImpl fileUploadsService;
    @Mock BlacklistUserRepository blacklistUserRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock BlacklistRepository blacklistRepository;
    @Mock RealtimeGateway realtimeGateway;
    @Mock FollowService followService;
    @Mock SessionService sessionService;

    UserServiceImpl svc;
    static final String ME = "me-1";
    static final String OTHER = "other-2";

    @BeforeEach
    void setUp() throws Exception {
        svc = new UserServiceImpl(userRepository, authorizePathService, passwordEncoder,
                fileUploadsService, blacklistUserRepository, postRepository, commentRepository,
                blacklistRepository, realtimeGateway, followService, sessionService);
        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
    }

    /* ---------- Tìm người dùng: nhẹ + phân trang server ---------- */

    @Test
    void timNguoiDungNapDemTheoLoVaGanDungNguoi() {
        when(userRepository.countSearchLite("an")).thenReturn(2L);
        when(userRepository.searchLite(eq("an"), any(Pageable.class))).thenReturn(List.of(
                new Object[]{"u-1", "Nguyen Van An", "avatar/a.png"},
                new Object[]{"u-2", "Tran An", null}));
        when(userRepository.countFollowersOf(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{"u-1", 7L}));
        when(userRepository.countPostsOf(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{"u-2", 3L}));

        Map<String, Object> m = svc.searchUsersLite("an", 0, 20);

        @SuppressWarnings("unchecked")
        List<UserLiteDTO> items = (List<UserLiteDTO>) m.get("items");
        assertEquals(2L, m.get("total"));
        assertEquals(2, items.size());
        assertEquals(7, items.get(0).getFollowers(), "số theo dõi phải gắn đúng người");
        assertEquals(0, items.get(0).getPosts(), "không có trong map đếm -> 0, không phải null");
        assertEquals(0, items.get(1).getFollowers());
        assertEquals(3, items.get(1).getPosts());
    }

    @Test
    void khongCoKetQuaThiKhongChayTruyVanTrangVaTruyVanDem() {
        when(userRepository.countSearchLite("zzz")).thenReturn(0L);

        Map<String, Object> m = svc.searchUsersLite("zzz", 0, 20);

        assertEquals(0L, m.get("total"));
        assertTrue(((List<?>) m.get("items")).isEmpty());
        verify(userRepository, never()).searchLite(anyString(), any());
        verify(userRepository, never()).countFollowersOf(any());
        verify(userRepository, never()).countPostsOf(any());
    }

    @Test
    void chanKichThuocTrangDeKhongAiKeoCaBangVe() {
        when(userRepository.countSearchLite(anyString())).thenReturn(0L);
        assertEquals(0, svc.searchUsersLite("", -5, 999).get("page"), "trang âm -> 0");
        // size bị kẹp ở 50 -> totalPages tính theo 50 chứ không theo 999
        when(userRepository.countSearchLite(anyString())).thenReturn(100L);
        when(userRepository.searchLite(anyString(), any())).thenReturn(List.of());
        assertEquals(2, svc.searchUsersLite("", 0, 999).get("totalPages"));
    }

    @Test
    void qNullDuocCoiLaRong() {
        when(userRepository.countSearchLite("")).thenReturn(0L);
        svc.searchUsersLite(null, 0, 20);
        verify(userRepository).countSearchLite("");
    }

    /* ---------- Rò dữ liệu riêng tư trong danh sách ---------- */

    private Users user(String id, String phone, Integer phonePublic) {
        Users u = new Users();
        u.setUserId(id);
        u.setFullName("Ten " + id);
        u.setEmail(id + "@x.vn");
        u.setDob(new Date());
        u.setPhone(phone);
        u.setPhonePublic(phonePublic);
        // convertToDTO đọc .size() của bốn tập hợp này; Hibernate luôn khởi tạo sẵn,
        // còn new Users() thuần thì để null.
        u.setFolloweds(new java.util.HashSet<>());
        u.setFollowers(new java.util.HashSet<>());
        u.setUserPosts(new java.util.HashSet<>());
        u.setParticipants(new java.util.HashSet<>());
        return u;
    }

    @Test
    void danhSachPhaiCheSoDienThoaiCuaNguoiDaTatCongKhai() {
        when(userRepository.findAll()).thenReturn(List.of(user(OTHER, "0900123456", 0)));

        UserDTO d = svc.getAllUser().get(0);

        assertNull(d.getPhone(),
                "phonePublic = 0 mà vẫn trả số -> đúng lỗi đã đo được trên production");
    }

    @Test
    void danhSachKhongDuocLoNgaySinhCuaNguoiKhac() {
        when(userRepository.findAll()).thenReturn(List.of(user(OTHER, null, 1)));

        assertNull(svc.getAllUser().get(0).getDob(),
                "dob không có cờ công khai nào -> không được lộ trong danh sách");
    }

    @Test
    void chinhChuVanThayDuLieuCuaMinh() {
        when(userRepository.findAll()).thenReturn(List.of(user(ME, "0900123456", 0)));

        UserDTO d = svc.getAllUser().get(0);

        assertEquals("0900123456", d.getPhone(), "chính chủ vẫn phải thấy số của mình");
        assertNotNull(d.getDob());
    }
}
