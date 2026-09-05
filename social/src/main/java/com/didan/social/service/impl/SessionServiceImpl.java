package com.didan.social.service.impl;

import com.didan.social.entity.BlacklistToken;
import com.didan.social.entity.UserSessions;
import com.didan.social.repository.BlacklistRepository;
import com.didan.social.repository.UserSessionRepository;
import com.didan.social.service.SessionService;
import com.didan.social.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {
    private final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);
    private final UserSessionRepository userSessionRepository;
    private final BlacklistRepository blacklistRepository;
    private final JwtUtils jwtUtils;

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

        userSessionRepository.save(new UserSessions(
                UUID.randomUUID().toString(), userId,
                JwtUtils.sha256Hex(refresh), access, remember, now));

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
