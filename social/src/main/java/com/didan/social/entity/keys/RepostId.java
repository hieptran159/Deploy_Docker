package com.didan.social.entity.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RepostId implements Serializable {
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "post_id", length = 50, nullable = false)
    private String postId;

    public RepostId() {
    }

    public RepostId(String userId, String postId) {
        this.userId = userId;
        this.postId = postId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepostId)) return false;
        RepostId that = (RepostId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(postId, that.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, postId);
    }
}
