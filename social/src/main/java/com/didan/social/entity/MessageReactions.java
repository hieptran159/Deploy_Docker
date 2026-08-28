package com.didan.social.entity;

import com.didan.social.entity.keys.MessageReactionId;
import jakarta.persistence.*;

/**
 * Cảm xúc thả cho 1 tin nhắn. Mỗi người 1 emoji / 1 tin nhắn (khóa kép message_id + user_id).
 * Bảng tự tạo bởi ddl-auto=update.
 */
@Entity(name = "message_reactions")
@Table(name = "message_reactions", indexes = {
        @Index(name = "idx_msgreact_message", columnList = "message_id")
})
public class MessageReactions {
    @EmbeddedId
    private MessageReactionId messageReactionId;

    @Column(name = "emoji", length = 16)
    private String emoji;

    public MessageReactions() {
    }

    public MessageReactions(MessageReactionId messageReactionId, String emoji) {
        this.messageReactionId = messageReactionId;
        this.emoji = emoji;
    }

    public MessageReactionId getMessageReactionId() { return messageReactionId; }
    public void setMessageReactionId(MessageReactionId messageReactionId) { this.messageReactionId = messageReactionId; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
}
