package com.didan.social.service.impl;

import com.didan.social.dto.SessionDTO;
import com.didan.social.entity.BlacklistToken;
import com.didan.social.entity.UserSessions;
import com.didan.social.repository.BlacklistRepository;
import com.didan.social.repository.UserSessionRepository;
import com.didan.social.service.SessionService;
import com.didan.social.utils.ClientIpUtils;
import com.didan.social.utils.JwtUtils;
import com.didan.social.utils.UserAgentUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {
    private final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);
    private final UserSessionRepository userSessionRepository;
    private final BlacklistRepository blacklistRepository;
    private final JwtUtils jwtUtils;

    /** Cùng cờ với RateLimitFilter: chỉ tin header proxy khi thật sự đứng sau proxy. */
    @Value("${app.ratelimit.trust-forwarded:false}")
    private boolean trustForwarded;

    @Autowired
    public SessionServiceImpl(UserSessionRepository userSessionRepository,
                              BlacklistRepository blacklistRepository,
                              JwtUtils jwtUtils) {
        this.userSessionRepository = userSessionRepository;
        this.blacklistRepository = blacklistRepository;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public String[] openSession(String userId, boolean remember) {
        String access = jwtUtils.generateAccessToken(userId, remember);
        String refresh = jwtUtils.generateRefreshToken(userId, remember);
        Date now = new Date();

        UserSessions session = new UserSessions(
                UUID.randomUUID().toString(), userId,
                JwtUtils.sha256Hex(refresh), access, remember, now);
        // Lấy từ request đang chạy thay vì bắt AuthService chuyền tay qua ba lời gọi.
        // Không có request (test, luồng nội bộ) thì để trống, phiên vẫn mở bình thường.
        HttpServletRequest req = currentRequest();
        if (req != null) {
            String ua = req.getHeader("User-Agent");
            if (ua != null && ua.length() > 255) ua = ua.substring(0, 255);
            session.setUserAgent(ua);
            session.setIp(ClientIpUtils.resolve(req, trustForwarded));
        }
        userSessionRepository.save(session);

        // Dọn phiên đã quá hạn refresh của chính người này, khỏi phình bảng.
        // Hỏng bước dọn thì cũng không được chặn việc đăng nhập.
        try {
            userSessionRepository.deleteByUserIdAndLastUsedAtBefore(
                    userId, new Date(now.getTime() - jwtUtils.getRefreshExpirationMs()));
        } catch (Exception e) {
            logger.warn("Don phien qua han that bai cho {}: {}", userId, e.getMessage());
        }
        return new String[]{ access, refresh };
    }

    @Override
    public void closeSession(String accessToken) {
        if (!StringUtils.hasText(accessToken)) return;
        blacklist(accessToken);
        UserSessions session = userSessionRepository.findFirstByAccessToken(accessToken);
        if (session != null) userSessionRepository.delete(session);
    }

    @Override
    public void revokeAllSessions(String userId) {
        for (UserSessions s : userSessionRepository.findByUserIdOrderByLastUsedAtDesc(userId)) {
            blacklist(s.getAccessToken());
        }
        userSessionRepository.deleteByUserId(userId);
    }

    @Override
    public List<SessionDTO> listSessions(String userId, String currentAccessToken) {
        List<SessionDTO> out = new ArrayList<>();
        for (UserSessions s : userSessionRepository.findByUserIdOrderByLastUsedAtDesc(userId)) {
            SessionDTO dto = new SessionDTO();
            dto.setSessionId(s.getSessionId());
            dto.setDevice(UserAgentUtils.label(s.getUserAgent()));
            dto.setIp(s.getIp());
            dto.setCreatedAt(s.getCreatedAt());
            dto.setLastUsedAt(s.getLastUsedAt());
            dto.setRemember(s.getRemember() != null && s.getRemember() == 1);
            dto.setCurrent(currentAccessToken != null && currentAccessToken.equals(s.getAccessToken()));
            out.add(dto);
        }
        return out;
    }

    @Override
    public void closeSessionById(String userId, String sessionId) throws Exception {
        UserSessions session = userSessionRepository.findById(sessionId).orElse(null);
        // Cùng một thông báo cho "không tồn tại" và "của người khác": nói khác nhau
        // là để lộ sessionId nào có thật.
        if (session == null || !session.getUserId().equals(userId))
            throw new Exception("Phiên không tồn tại");
        blacklist(session.getAccessToken());
        userSessionRepository.delete(session);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    /** Access token là JWT không trạng thái, chặn giữa chừng chỉ có cách đưa vào blacklist. */
    private void blacklist(String token) {
        if (!StringUtils.hasText(token)) return;
        try {
            blacklistRepository.save(new BlacklistToken(token));
        } catch (Exception ignore) {
            // token đã nằm trong blacklist -> trùng khoá chính, bỏ qua
        }
    }
}
