package com.didan.social.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Chặn dội request (brute-force mật khẩu, spam gửi OTP) ở các route công khai /auth/**.
 * Bộ đếm cửa sổ cố định, lưu trong bộ nhớ (backend chạy 1 instance). Khóa theo IP client.
 * Vượt ngưỡng -> HTTP 429 + header Retry-After, body giữ đúng dạng ResponseData.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${app.ratelimit.enabled:true}")
    private boolean enabled;

    // limit / windowSeconds cho từng nhóm route
    @Value("${app.ratelimit.signin:20}")        private int signinLimit;
    @Value("${app.ratelimit.signin-window:300}") private int signinWindow;
    @Value("${app.ratelimit.signup:6}")          private int signupLimit;
    @Value("${app.ratelimit.signup-window:3600}") private int signupWindow;
    @Value("${app.ratelimit.otp-send:5}")        private int otpSendLimit;
    @Value("${app.ratelimit.otp-send-window:900}") private int otpSendWindow;
    @Value("${app.ratelimit.otp-check:20}")      private int otpCheckLimit;
    @Value("${app.ratelimit.otp-check-window:600}") private int otpCheckWindow;
    @Value("${app.ratelimit.default:40}")        private int defaultLimit;
    @Value("${app.ratelimit.default-window:300}") private int defaultWindow;

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger sinceSweep = new AtomicInteger();

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5; // trước JwtAuthenticationFilter và security chain
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) return true;
        String m = request.getMethod();
        if (!"POST".equalsIgnoreCase(m) && !"PATCH".equalsIgnoreCase(m)) return true;
        String p = request.getRequestURI();
        return p == null || !p.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String bucket;
        int limit, windowSec;
        if (path.startsWith("/auth/signin")) {
            bucket = "signin"; limit = signinLimit; windowSec = signinWindow;
        } else if (path.startsWith("/auth/signup")) {
            bucket = "signup"; limit = signupLimit; windowSec = signupWindow;
        } else if (path.startsWith("/auth/resend-verify") || path.startsWith("/auth/token-reset")) {
            bucket = "otp-send"; limit = otpSendLimit; windowSec = otpSendWindow;
        } else if (path.startsWith("/auth/verify") || path.startsWith("/auth/reset")
                || path.startsWith("/auth/2fa/verify")) {
            bucket = "otp-check"; limit = otpCheckLimit; windowSec = otpCheckWindow;
        } else {
            bucket = "default"; limit = defaultLimit; windowSec = defaultWindow;
        }

        String key = bucket + "|" + clientIp(request);
        long windowMs = windowSec * 1000L;
        long now = System.currentTimeMillis();

        Window w = buckets.computeIfAbsent(key, k -> new Window(now));
        long retryAfterMs;
        synchronized (w) {
            if (now - w.start >= windowMs) {
                w.start = now;
                w.count.set(0);
            }
            long c = w.count.incrementAndGet();
            if (c > limit) {
                retryAfterMs = windowMs - (now - w.start);
            } else {
                retryAfterMs = -1;
            }
        }

        maybeSweep(now);

        if (retryAfterMs >= 0) {
            long retryAfterSec = Math.max(1, (retryAfterMs + 999) / 1000);
            logger.warn("Rate limit {} vượt ngưỡng ({}), IP {} - chặn {}s", bucket, limit, clientIp(request), retryAfterSec);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSec));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"statusCode\":429,\"description\":\"Bạn thao tác quá nhanh. Vui lòng thử lại sau "
                + retryAfterSec + " giây.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private void maybeSweep(long now) {
        if (sinceSweep.incrementAndGet() < 500) return;
        sinceSweep.set(0);
        long maxWindow = Math.max(defaultWindow, Math.max(signinWindow, Math.max(signupWindow,
                Math.max(otpSendWindow, otpCheckWindow)))) * 1000L;
        buckets.entrySet().removeIf(e -> now - e.getValue().start > maxWindow);
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(real)) return real.trim();
        return request.getRemoteAddr();
    }

    private static final class Window {
        volatile long start;
        final AtomicLong count = new AtomicLong();
        Window(long start) { this.start = start; }
    }
}
