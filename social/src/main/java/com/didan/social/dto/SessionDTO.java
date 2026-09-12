package com.didan.social.dto;

import java.util.Date;

/**
 * Một thiết bị đang đăng nhập, như chủ tài khoản nhìn thấy.
 *
 * Cố ý KHÔNG mang accessToken hay refreshHash ra ngoài: chỉ cần sessionId là đủ
 * để thu hồi, mà lộ token thì chính màn hình bảo mật này lại thành chỗ rò.
 */
public class SessionDTO {
    private String sessionId;
    /** Nhãn ngắn rút từ User-Agent, ví dụ "Chrome trên Windows". */
    private String device;
    private String ip;
    private Date createdAt;
    /** Lần làm mới token gần nhất — KHÔNG phải lần dùng gần nhất (xem SessionService). */
    private Date lastUsedAt;
    private boolean remember;
    /** true = chính thiết bị đang gọi API này. */
    private boolean current;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Date lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public boolean isRemember() { return remember; }
    public void setRemember(boolean remember) { this.remember = remember; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
}
