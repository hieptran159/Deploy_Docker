package com.didan.social.entity;

import com.didan.social.entity.keys.HashtagFollowId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

/** Một người theo dõi một hashtag. */
@Entity(name = "hashtag_follows")
@Table(name = "hashtag_follows")
public class HashtagFollows {

    @EmbeddedId
    private HashtagFollowId hashtagFollowId;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public HashtagFollows() {}

    public HashtagFollows(HashtagFollowId hashtagFollowId, Date createdAt) {
        this.hashtagFollowId = hashtagFollowId;
        this.createdAt = createdAt;
    }

    public HashtagFollowId getHashtagFollowId() { return hashtagFollowId; }
    public void setHashtagFollowId(HashtagFollowId hashtagFollowId) { this.hashtagFollowId = hashtagFollowId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
