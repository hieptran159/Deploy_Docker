package com.didan.social.entity.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** (user_id, tag) — khoá chính lo luôn việc "theo dõi hai lần" là vô nghĩa. */
@Embeddable
public class HashtagFollowId implements Serializable {

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "tag", nullable = false, length = 50)
    private String tag;

    public HashtagFollowId() {}

    public HashtagFollowId(String userId, String tag) {
        this.userId = userId;
        this.tag = tag;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HashtagFollowId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tag);
    }
}
