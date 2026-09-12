package com.didan.social.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity(name = "blacklist_user")
public class BlacklistUser {
    @Id
    @Column(name = "user_id")
    private String userId;
    @OneToOne
    @JoinColumn(name = "user_id")
    private Users users;

    @Column(name = "status")
    private String status = "pending";

    @Column(name = "reported_quantity")
    private int reportedQuantity = 0;

    @Column(name = "blocked_at", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date blockedAt = null;

    /** Hết hạn cấm. NULL = vĩnh viễn. */
    @Column(name = "banned_until", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date bannedUntil = null;

    /**
     * Lệnh cấm này CÒN hiệu lực không.
     *
     * Đặt ở entity vì có tới sáu chỗ trong ứng dụng hỏi câu này (đăng nhập, làm mới
     * token, xác thực 2 bước, đổi hồ sơ, thống kê admin, ban/unban). Rải phép so
     * sánh ngày ra sáu nơi thì kiểu gì cũng có chỗ quên, và hậu quả là lệnh cấm hết
     * hạn ở màn này nhưng vẫn còn ở màn kia.
     */
    public boolean isActiveBan() {
        if (!"blocked".equals(status)) return false;
        return bannedUntil == null || bannedUntil.after(new Date());
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getReportedQuantity() {
        return reportedQuantity;
    }

    public void setReportedQuantity(int reportedQuantity) {
        this.reportedQuantity = reportedQuantity;
    }

    public Date getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Date blockedAt) {
        this.blockedAt = blockedAt;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public Date getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(Date bannedUntil) {
        this.bannedUntil = bannedUntil;
    }
}
