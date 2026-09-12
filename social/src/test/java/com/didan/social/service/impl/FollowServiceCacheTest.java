package com.didan.social.service.impl;

import com.didan.social.config.CacheConfig;
import com.didan.social.entity.Followers;
import com.didan.social.entity.Users;
import com.didan.social.repository.BlockRepository;
import com.didan.social.repository.FollowRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.FollowService;
import com.didan.social.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * `friendIdsOf` chạy ở mọi lần dựng feed/tìm kiếm, nên nó được cache. Cái đắt ở đây
 * không phải phần cache mà phần XOÁ cache: quên xoá thì người vừa kết bạn xong không
 * thấy bài "chỉ bạn bè" của nhau suốt 10 phút, mà không có lỗi nào nổ ra để biết.
 *
 * Test dựng một context Spring tí hon (mock hết repository, cache trong RAM) vì
 * @Cacheable/@CacheEvict chỉ có tác dụng qua proxy — gọi thẳng `new FollowServiceImpl()`
 * như các test Mockito khác thì annotation không chạy và bài test sẽ luôn xanh vô nghĩa.
 */
class FollowServiceCacheTest {

    static final String ME = "u-me";
    static final String BAN = "u-ban";

    AnnotationConfigApplicationContext ctx;
    FollowRepository followRepository;
    UserRepository userRepository;
    AuthorizePathService authorizePathService;
    // Kiểu INTERFACE, không phải impl: @EnableCaching bọc bean bằng JDK proxy nên
    // trong context chỉ còn FollowService — giống hệt cách các service khác nhận nó.
    FollowService svc;

    @Configuration
    @EnableCaching
    static class Ctx {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheConfig.FRIEND_IDS, CacheConfig.BLOCKED_IDS, CacheConfig.BLOCKER_IDS);
        }

        @Bean
        FollowServiceImpl followService(FollowRepository f, AuthorizePathService a, UserRepository u,
                                        NotificationService n, BlockRepository b) {
            return new FollowServiceImpl(f, a, u, n, b);
        }
    }

    /** Một hàng "đã kết bạn" giữa a và b. */
    private static Followers accepted(String a, String b) {
        Users ua = new Users(); ua.setUserId(a);
        Users ub = new Users(); ub.setUserId(b);
        Followers f = new Followers();
        f.setUsers1(ua);
        f.setUsers2(ub);
        f.setStatus("accepted");
        return f;
    }

    private static Followers pending(String a, String b) {
        Followers f = accepted(a, b);
        f.setStatus("pending");
        return f;
    }

    @BeforeEach
    void setUp() {
        followRepository = mock(FollowRepository.class);
        userRepository = mock(UserRepository.class);
        authorizePathService = mock(AuthorizePathService.class);

        ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(FollowRepository.class, () -> followRepository);
        ctx.registerBean(UserRepository.class, () -> userRepository);
        ctx.registerBean(AuthorizePathService.class, () -> authorizePathService);
        ctx.registerBean(NotificationService.class, () -> mock(NotificationService.class));
        ctx.registerBean(BlockRepository.class, () -> mock(BlockRepository.class));
        ctx.register(Ctx.class);
        ctx.refresh();
        svc = ctx.getBean(FollowService.class);
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    @Test
    void goiLaiKhongDungLaiDenDB() {
        when(followRepository.findAcceptedOf(ME)).thenReturn(List.of(accepted(ME, BAN)));

        assertEquals(List.of(BAN), svc.friendIdsOf(ME));
        assertEquals(List.of(BAN), svc.friendIdsOf(ME));
        assertEquals(List.of(BAN), svc.friendIdsOf(ME));

        verify(followRepository, times(1)).findAcceptedOf(ME);
    }

    @Test
    void moiNguoiMotKhoaRieng_khongTraNhamDanhSachCuaNguoiKhac() {
        when(followRepository.findAcceptedOf(ME)).thenReturn(List.of(accepted(ME, BAN)));
        when(followRepository.findAcceptedOf(BAN)).thenReturn(List.of(accepted(ME, BAN)));

        assertEquals(List.of(BAN), svc.friendIdsOf(ME));
        assertEquals(List.of(ME), svc.friendIdsOf(BAN), "khoá cache phải theo userId");
    }

    @Test
    void chapNhanKetBanThiXoaCache_khongConDanhSachCu() throws Exception {
        // Trước khi kết bạn: chưa có ai
        when(followRepository.findAcceptedOf(ME)).thenReturn(List.of());
        assertEquals(List.of(), svc.friendIdsOf(ME));

        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
        when(followRepository.findFirstByUsers1_UserIdAndUsers2_UserId(BAN, ME))
                .thenReturn(pending(BAN, ME));
        Users me = new Users(); me.setUserId(ME); me.setFullName("Tôi");
        when(userRepository.findFirstByUserId(ME)).thenReturn(me);

        svc.acceptRequest(BAN);

        when(followRepository.findAcceptedOf(ME)).thenReturn(List.of(accepted(BAN, ME)));
        assertEquals(List.of(BAN), svc.friendIdsOf(ME),
                "kết bạn xong mà vẫn đọc được danh sách cũ = bài 'chỉ bạn bè' không hiện ra");
    }

    @Test
    void huyKetBanThiCungXoaCache() throws Exception {
        when(followRepository.findAcceptedOf(ME)).thenReturn(List.of(accepted(ME, BAN)));
        assertEquals(List.of(BAN), svc.friendIdsOf(ME));

        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
        when(followRepository.findFirstByUsers1_UserIdAndUsers2_UserId(ME, BAN))
                .thenReturn(accepted(ME, BAN));

        svc.unfriend(BAN);

        when(followRepository.findAcceptedOf(ME)).thenReturn(List.of());
        assertEquals(List.of(), svc.friendIdsOf(ME), "huỷ kết bạn xong vẫn thấy nhau là sai");
    }

    /**
     * Chặn cũng xoá quan hệ bạn bè hai chiều trong DB — nếu quên xoá cache thì người bị
     * chặn vẫn đọc được bài "chỉ bạn bè", tức là lỗi quyền riêng tư chứ không chỉ dữ liệu cũ.
     */
    @Test
    void chanNguoiKhacThiXoaLuonCacheBanBe() throws Exception {
        when(followRepository.findAcceptedOf(ME)).thenReturn(List.of(accepted(ME, BAN)));
        assertEquals(List.of(BAN), svc.friendIdsOf(ME));

        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
        Users ban = new Users(); ban.setUserId(BAN);
        when(userRepository.findFirstByUserId(BAN)).thenReturn(ban);
        when(followRepository.findFirstByUsers1_UserIdAndUsers2_UserId(ME, BAN))
                .thenReturn(accepted(ME, BAN));

        svc.blockUser(BAN);

        when(followRepository.findAcceptedOf(ME)).thenReturn(List.of());
        assertEquals(List.of(), svc.friendIdsOf(ME));
    }
}
