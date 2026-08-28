package com.didan.social.entity.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MessageReactionId implements Serializable {
    @Column(name = "message_id", length = 50, nullable = false)
    private String messageId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    public MessageReactionId() {
    }

    public MessageReactionId(String messageId, String userId) {
        this.messageId = messageId;
        this.userId = userId;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageReactionId)) return false;
        MessageReactionId that = (MessageReactionId) o;
        return Objects.equals(messageId, that.messageId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, userId);
    }
}
