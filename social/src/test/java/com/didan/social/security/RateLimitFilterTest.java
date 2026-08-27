package com.didan.social.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra bộ đếm cửa sổ cố định của RateLimitFilter (không cần Spring context). */
class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "signinLimit", 3);
        ReflectionTestUtils.setField(filter, "signinWindow", 300);
        ReflectionTestUtils.setField(filter, "signupLimit", 3);
        ReflectionTestUtils.setField(filter, "signupWindow", 3600);
        ReflectionTestUtils.setField(filter, "otpSendLimit", 3);
        ReflectionTestUtils.setField(filter, "otpSendWindow", 900);
        ReflectionTestUtils.setField(filter, "otpCheckLimit", 3);
        ReflectionTestUtils.setField(filter, "otpCheckWindow", 600);
        ReflectionTestUtils.setField(filter, "defaultLimit", 3);
        ReflectionTestUtils.setField(filter, "defaultWindow", 300);
    }

    private MockHttpServletRequest post(String uri, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        req.setRemoteAddr(ip);
        return req;
    }

    private int call(MockHttpServletRequest req) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void blocksAfterLimitOnSameIp() throws Exception {
        for (int i = 1; i <= 3; i++) {
            assertEquals(200, call(post("/auth/signin", "10.0.0.1")), "request " + i + " phải qua");
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(post("/auth/signin", "10.0.0.1"), res, new MockFilterChain());
        assertEquals(429, res.getStatus());
        assertNotNull(res.getHeader("Retry-After"));
        assertTrue(res.getContentAsString().contains("quá nhanh"));
    }

    @Test
    void differentIpsAreIndependent() throws Exception {
        for (int i = 0; i < 3; i++) call(post("/auth/signin", "1.1.1.1"));
        assertEquals(429, call(post("/auth/signin", "1.1.1.1")));
        assertEquals(200, call(post("/auth/signin", "2.2.2.2")));
    }

    @Test
    void bucketsAreSeparate() throws Exception {
        for (int i = 0; i < 3; i++) call(post("/auth/signin", "3.3.3.3"));
        assertEquals(429, call(post("/auth/signin", "3.3.3.3")));
        // cùng IP nhưng route khác nhóm -> bucket riêng, vẫn qua
        assertEquals(200, call(post("/auth/resend-verify", "3.3.3.3")));
    }

    @Test
    void honorsXForwardedFor() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest r = post("/auth/signin", "127.0.0.1");
            r.addHeader("X-Forwarded-For", "9.9.9.9, 127.0.0.1");
            call(r);
        }
        MockHttpServletRequest r = post("/auth/signin", "127.0.0.1");
        r.addHeader("X-Forwarded-For", "9.9.9.9, 127.0.0.1");
        assertEquals(429, call(r));
        // client IP khác (không có XFF) -> không bị chặn
        assertEquals(200, call(post("/auth/signin", "8.8.8.8")));
    }

    @Test
    void ignoresNonAuthPathsAndGet() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, call(post("/post/get", "4.4.4.4")));
        }
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/auth/signin");
            get.setRemoteAddr("4.4.4.4");
            assertEquals(200, call(get));
        }
    }

    @Test
    void disabledFlagBypasses() throws Exception {
        ReflectionTestUtils.setField(filter, "enabled", false);
        for (int i = 0; i < 20; i++) {
            assertEquals(200, call(post("/auth/signin", "5.5.5.5")));
        }
    }
}
