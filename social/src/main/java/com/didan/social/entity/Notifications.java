package com.didan.social.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity(name = "notifications")
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_recipient_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notif_recipient_created", columnList = "recipient_id, created_at")
})
public class Notifications {
    @Id
    @Column(name = "notification_id")
    private String notificationId;

    @Column(name = "recipient_id", nullable = false, length = 50)
    private String recipientId;

    @Column(name = "actor_id", length = 50)
    private String actorId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "target_id", length = 50)
    private String targetId;

    // id phụ để điều hướng chính xác (vd: commentId cho COMMENT/COMMENT_LIKE/REPLY/MENTION)
    @Column(name = "ref_id", length = 50)
    private String refId;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "is_read", nullable = false)
    private int isRead;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Notifications() {}

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getIsRead() { return isRead; }
    public void setIsRead(int isRead) { this.isRead = isRead; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
