package com.didan.social.dto;


public class ConversationDTO {
    private String conversationId;
    private String conversationName;
    private String avatarUrl;
    private String createdAt;
    private String lastMessage;
    private String lastMessageImg;
    private String lastMessageAt;
    private String lastSenderId;
    private boolean muted;

    public ConversationDTO() {
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public ConversationDTO(String conversationId, String conversationName, String createdAt) {
        this.conversationId = conversationId;
        this.conversationName = conversationName;
        this.createdAt = createdAt;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageImg() {
        return lastMessageImg;
    }

    public void setLastMessageImg(String lastMessageImg) {
        this.lastMessageImg = lastMessageImg;
    }

    public String getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(String lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }
}
