package com.didan.social.dto;

public class NotificationDTO {
    private String notificationId;
    private String actorId;
    private String actorName;
    private String actorAvatar;
    private String type;
    private String targetId;
    private String refId;
    private String message;
    private boolean read;
    private String createdAt;

    public NotificationDTO() {}

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorAvatar() { return actorAvatar; }
    public void setActorAvatar(String actorAvatar) { this.actorAvatar = actorAvatar; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
