package com.didan.social.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Chưa đăng nhập" phải là MỘT trạng thái duy nhất.
 *
 * Ở các route cho khách xem, Spring Security vẫn đặt sẵn một Authentication ẩn danh với
 * principal là chuỗi "anonymousUser". Trả chuỗi đó ra như một userId thì cả ứng dụng tin
 * rằng có người đang đăng nhập: lượt xem của mọi khách gộp vào chung một khoá chống trùng
 * (mỗi bài chỉ đếm 1 lượt/30 phút), và nhánh lấy IP dự phòng không bao giờ chạy.
 */
class AuthorizePathServiceImplTest {

    private final AuthorizePathServiceImpl svc = new AuthorizePathServiceImpl();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void setAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    }

    private void setLoggedIn(Object principal, String token) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, token, List.of()));
    }

    @Test
    void khachXemThiCoiNhuCHUAdangNhap() {
        setAnonymous();
        assertThrows(Exception.class, svc::getUserIdAuthoried,
                "\"anonymousUser\" không phải một người dùng — trả nó ra là nói dối cả ứng dụng");
    }

    @Test
    void khongCoAuthenticationThiVanNhuCu() {
        SecurityContextHolder.clearContext();
        assertThrows(Exception.class, svc::getUserIdAuthoried);
    }

    @Test
    void nguoiDangNhapThatThiTraVeDungId() throws Exception {
        setLoggedIn("u-123", "tok");
        assertEquals("u-123", svc.getUserIdAuthoried());
    }

    @Test
    void principalKhongPhaiChuoiThiKhongEpKieuMu() {
        // Ép kiểu mù (String) sẽ nổ ClassCastException thay vì báo "chưa đăng nhập"
        setLoggedIn(new Object(), "tok");
        assertThrows(Exception.class, svc::getUserIdAuthoried);
    }

    @Test
    void principalRongCungKhongTinh() {
        setLoggedIn("   ", "tok");
        assertThrows(Exception.class, svc::getUserIdAuthoried);
    }

    // ---- token của thiết bị đang gọi ----

    @Test
    void tokenCuaKhachLaNULLchuKhongPhaiChuoiRong() {
        setAnonymous();
        assertNull(svc.getAccessTokenAuthoried(),
                "phiên ẩn danh mang credentials rỗng; để lọt \"\" ra ngoài là mời gọi so sánh nhầm token");
    }

    @Test
    void tokenNguoiDangNhapDuocTraNguyenVen() {
        setLoggedIn("u-123", "eyJhbGciOi.abc.def");
        assertEquals("eyJhbGciOi.abc.def", svc.getAccessTokenAuthoried());
    }
}
