package com.didan.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

/**
 * Một hàng = một thiết bị đang đăng nhập.
 *
 * Thay cho hai cột users.access_token / users.refresh_token trước đây: vì chỉ có
 * một ô cho mỗi tài khoản nên đăng nhập ở máy thứ hai ghi đè lên phiên của máy
 * thứ nhất và đá nó ra.
 *
 * Cột phẳng, không dùng quan hệ JPA (giống Notifications) để tránh lazy-load
 * ngoài ý muốn trong tầng xác thực.
 */
@Entity(name = "user_sessions")
public class UserSessions {

    @Id
    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /** SHA-256 (hex) của refresh token. Không bao giờ lưu bản gốc. */
    @Column(name = "refresh_hash", length = 64, nullable = false)
    private String refreshHash;

    /** Access token hiện hành của phiên này, giữ để còn thu hồi được khi cần. */
    @Column(name = "access_token", length = 512)
    private String accessToken;

    /** 1 = "ghi nhớ đăng nhập" (token dài hạn), 0 = phiên tạm. */
    @Column(name = "remember", nullable = false)
    private Integer remember;

    // @Temporal là bắt buộc với java.util.Date theo JPA, và mọi entity khác trong
    // dự án đều khai tường minh. Thiếu nó thì ddl-auto=validate không khớp được
    // kiểu cột datetime.
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "last_used_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUsedAt;

    /** User-Agent lúc đăng nhập. Giữ nguyên bản gốc; nhãn hiển thị do UserAgentUtils rút ra. */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** IPv6 dài nhất 45 ký tự. Chỉ chủ tài khoản xem được, để nhận ra phiên lạ. */
    @Column(name = "ip", length = 45)
    private String ip;

    public UserSessions() {}

    public UserSessions(String sessionId, String userId, String refreshHash,
                        String accessToken, boolean remember, Date now) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.refreshHash = refreshHash;
        this.accessToken = accessToken;
        this.remember = remember ? 1 : 0;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRefreshHash() { return refreshHash; }
    public void setRefreshHash(String refreshHash) { this.refreshHash = refreshHash; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public Integer getRemember() { return remember; }
    public void setRemember(Integer remember) { this.remember = remember; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Date lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
