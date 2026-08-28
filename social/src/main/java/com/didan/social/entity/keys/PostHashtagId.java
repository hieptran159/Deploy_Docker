package com.didan.social.entity.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PostHashtagId implements Serializable {
    @Column(name = "post_id", length = 50, nullable = false)
    private String postId;

    @Column(name = "tag", length = 50, nullable = false)
    private String tag;

    public PostHashtagId() {
    }

    public PostHashtagId(String postId, String tag) {
        this.postId = postId;
        this.tag = tag;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostHashtagId)) return false;
        PostHashtagId that = (PostHashtagId) o;
        return Objects.equals(postId, that.postId) && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, tag);
    }
}
