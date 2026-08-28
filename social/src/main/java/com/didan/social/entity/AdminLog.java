package com.didan.social.entity;

import jakarta.persistence.*;

import java.util.Date;

/**
 * Nhật ký hành động của quản trị viên. Bảng tự tạo bởi ddl-auto=update.
 */
@Entity(name = "admin_logs")
@Table(name = "admin_logs", indexes = {
        @Index(name = "idx_adminlog_created", columnList = "created_at")
})
public class AdminLog {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "admin_id", length = 50)
    private String adminId;

    @Column(name = "admin_name", length = 255)
    private String adminName;

    // GRANT_ADMIN | BAN_USER | UNBAN_USER | HANDLE_REPORT | REMOVE_TARGET | RESTORE_TARGET
    @Column(name = "action", length = 40)
    private String action;

    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public AdminLog() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
